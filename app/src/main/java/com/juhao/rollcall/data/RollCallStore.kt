package com.juhao.rollcall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.rollCallPrefs: DataStore<Preferences> by preferencesDataStore(name = "rollcall")

private val STUDENTS_KEY = stringPreferencesKey("students_v1")
private val SETTINGS_KEY = stringPreferencesKey("settings_v1")
private const val RECORD_PREFIX = "record_v1_"

private fun recordKey(date: String) = stringPreferencesKey("$RECORD_PREFIX$date")

/**
 * 全部落盘逻辑都在这里：名单、设置、每一天的记录。
 * 记录按天分 key 存放，点一个人只重写当天的 JSON，不会越写越慢。
 */
class RollCallStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val students: Flow<List<Student>> = context.rollCallPrefs.data
        .map { prefs -> decodeStudents(prefs[STUDENTS_KEY]) }
        .distinctUntilChanged()

    val settings: Flow<Settings> = context.rollCallPrefs.data
        .map { prefs -> decodeSettings(prefs[SETTINGS_KEY]) }
        .distinctUntilChanged()

    fun record(date: String): Flow<DayRecord> = context.rollCallPrefs.data
        .map { prefs -> decodeRecord(prefs[recordKey(date)]) }
        .distinctUntilChanged()

    fun allRecords(): Flow<Map<String, DayRecord>> = context.rollCallPrefs.data
        .map { prefs ->
            prefs.asMap().entries.mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(RECORD_PREFIX) || value !is String) return@mapNotNull null
                val date = name.removePrefix(RECORD_PREFIX)
                runCatching { date to json.decodeFromString<DayRecord>(value) }.getOrNull()
            }.toMap()
        }
        .distinctUntilChanged()

    // ---------- 初始化 ----------

    suspend fun ensureSeeded() {
        val existing = students.first()
        val currentSettings = settings.first()
        if (existing.isNotEmpty() || currentSettings.seeded) return
        val names = SeedRoster.load(context)
        writeRoster(names.mapIndexed { index, name -> Student(id = index + 1L, name = name, order = index) })
        updateSettings { it.copy(seeded = true) }
    }

    // ---------- 出勤 ----------

    suspend fun setAttendance(date: String, studentId: Long, mark: AttendanceStatus?) {
        editRecord(date) { record ->
            val key = studentId.toString()
            val updated = record.attendance.toMutableMap()
            if (mark == null) updated.remove(key) else updated[key] = mark
            record.copy(attendance = updated)
        }
    }

    /** 清空当天的出勤标记：快速模式 = 全员到齐，逐个模式 = 全部未点。 */
    suspend fun clearAttendance(date: String) {
        editRecord(date) { it.copy(attendance = emptyMap()) }
    }

    // ---------- 作业 ----------

    suspend fun setHomework(date: String, studentId: Long, submitted: Boolean) {
        editRecord(date) { record ->
            record.copy(homework = record.homework + (studentId.toString() to submitted))
        }
    }

    suspend fun markAllHomework(date: String, students: List<Student>, submitted: Boolean) {
        editRecord(date) { record ->
            record.copy(homework = students.associate { it.id.toString() to submitted })
        }
    }

    // ---------- 名单 ----------

    suspend fun addStudent(name: String): Student? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val current = students.first()
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val student = Student(id = nextId, name = trimmed, order = current.size)
        writeRoster(current + student)
        return student
    }

    suspend fun renameStudent(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        writeRoster(students.first().map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    suspend fun deleteStudent(id: Long) {
        writeRoster(
            students.first()
                .filterNot { it.id == id }
                .mapIndexed { index, student -> student.copy(order = index) }
        )
    }

    suspend fun moveStudent(id: Long, delta: Int) {
        val current = students.first().sortedBy { it.order }.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target < 0 || target >= current.size) return
        val moved = current.removeAt(index)
        current.add(target, moved)
        writeRoster(current.mapIndexed { i, student -> student.copy(order = i) })
    }

    /**
     * 整体替换名单：同名沿用旧 id（历史记录不丢），新名字分配新 id。
     */
    suspend fun replaceRoster(names: List<String>) {
        val old = students.first()
        val oldByName = old.associateBy { it.name }
        var nextId = (old.maxOfOrNull { it.id } ?: 0L) + 1L
        val rebuilt = names.mapIndexed { index, name ->
            val matched = oldByName[name]
            if (matched != null) {
                matched.copy(order = index)
            } else {
                Student(id = nextId++, name = name, order = index)
            }
        }
        writeRoster(rebuilt)
    }

    suspend fun appendRoster(names: List<String>) {
        val current = students.first().sortedBy { it.order }.toMutableList()
        var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        names.forEach { name ->
            current.add(Student(id = nextId++, name = name, order = current.size))
        }
        writeRoster(current.mapIndexed { i, student -> student.copy(order = i) })
    }

    suspend fun restoreSeedRoster() {
        replaceRoster(SeedRoster.load(context))
    }

    // ---------- 设置 ----------

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        context.rollCallPrefs.edit { prefs ->
            val current = decodeSettings(prefs[SETTINGS_KEY])
            prefs[SETTINGS_KEY] = json.encodeToString(transform(current))
        }
    }

    // ---------- 内部 ----------

    private suspend fun writeRoster(list: List<Student>) {
        context.rollCallPrefs.edit { prefs ->
            prefs[STUDENTS_KEY] = json.encodeToString(list)
        }
    }

    private suspend fun editRecord(date: String, transform: (DayRecord) -> DayRecord) {
        context.rollCallPrefs.edit { prefs ->
            val key = recordKey(date)
            val current = decodeRecord(prefs[key])
            prefs[key] = json.encodeToString(transform(current))
        }
    }

    private fun decodeStudents(raw: String?): List<Student> {
        val list = raw?.let {
            runCatching { json.decodeFromString<List<Student>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
        return list.sortedBy { it.order }
    }

    private fun decodeSettings(raw: String?): Settings {
        return raw?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings()
    }

    private fun decodeRecord(raw: String?): DayRecord {
        return raw?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() } ?: DayRecord()
    }
}
