package com.juhao.rollcall.state

import android.content.Context
import com.juhao.rollcall.data.AttendanceLogic
import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.data.CsvExporter
import com.juhao.rollcall.data.DateFormats
import com.juhao.rollcall.data.DayRecord
import com.juhao.rollcall.data.DaySummary
import com.juhao.rollcall.data.RollCallStore
import com.juhao.rollcall.data.Settings
import com.juhao.rollcall.data.StatsCalculator
import com.juhao.rollcall.data.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 页面状态机：从 DataStore 出各种 Flow，合成唯一一份 [UiState]；
 * 所有用户操作都变成这里的挂起调用，UI 只负责画和点。
 */
class RollCallController(context: Context) {

    private val appContext = context.applicationContext
    private val store = RollCallStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val today = MutableStateFlow(DateFormats.todayString())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        scope.launch {
            runCatching { store.ensureSeeded() }
        }
        scope.launch {
            while (isActive) {
                refreshToday()
                delay(30_000L)
            }
        }
        scope.launch {
            combine(
                store.students,
                store.settings,
                store.allRecords(),
                today,
            ) { students, settings, records, date ->
                StateAssembler.assemble(students, settings, records, date)
            }
                .catch { /* DataStore 短暂异常时保留旧状态 */ }
                .collect { _state.value = it }
        }
    }

    fun refreshToday() {
        runCatching {
            val now = DateFormats.todayString()
            if (today.value != now) today.value = now
        }
    }

    /** Activity 销毁时取消后台任务，避免界面退出后还在写盘。 */
    fun dispose() {
        scope.cancel()
    }

    // ---------- 出勤 ----------

    fun cycleAttendance(studentId: Long) {
        val snapshot = _state.value
        if (snapshot.today.isEmpty()) return
        val next = AttendanceLogic.next(snapshot.attendance[studentId], snapshot.mode)
        write { store.setAttendance(snapshot.today, studentId, next) }
    }

    /** 快速模式 = 全员到齐；逐个模式 = 全部未点。 */
    fun clearTodayAttendance() {
        val snapshot = _state.value
        if (snapshot.today.isEmpty()) return
        write { store.clearAttendance(snapshot.today) }
    }

    fun setMode(mode: AttendanceMode) {
        write {
            store.updateSettings { it.copy(mode = mode) }
            _messages.tryEmit(if (mode == AttendanceMode.FAST) "快速模式：默认全员到齐" else "逐个模式：默认未点")
        }
    }

    // ---------- 作业 ----------

    fun toggleHomework(studentId: Long) {
        val snapshot = _state.value
        if (snapshot.today.isEmpty()) return
        val submitted = !(snapshot.homework[studentId] ?: false)
        write { store.setHomework(snapshot.today, studentId, submitted) }
    }

    fun markAllHomeworkSubmitted() {
        val snapshot = _state.value
        if (snapshot.today.isEmpty()) return
        write { store.markAllHomework(snapshot.today, snapshot.students, true) }
    }

    fun markAllHomeworkMissing() {
        val snapshot = _state.value
        if (snapshot.today.isEmpty()) return
        write { store.markAllHomework(snapshot.today, snapshot.students, false) }
    }

    // ---------- 名单 ----------

    fun addStudent(name: String) {
        write {
            val added = store.addStudent(name)
            if (added != null) _messages.tryEmit("已添加 ${added.name}")
        }
    }

    fun renameStudent(id: Long, name: String) {
        write { store.renameStudent(id, name) }
    }

    fun deleteStudent(id: Long) {
        write {
            store.deleteStudent(id)
            _messages.tryEmit("已删除")
        }
    }

    fun moveStudent(id: Long, delta: Int) {
        write { store.moveStudent(id, delta) }
    }

    fun importRoster(names: List<String>, replace: Boolean) {
        if (names.isEmpty()) return
        write {
            if (replace) store.replaceRoster(names) else store.appendRoster(names)
            _messages.tryEmit(if (replace) "已替换名单：${names.size} 人" else "已追加 ${names.size} 人")
        }
    }

    fun restoreSeedRoster() {
        write {
            store.restoreSeedRoster()
            _messages.tryEmit("已恢复内置名单")
        }
    }

    // ---------- 设置 / 导出 ----------

    fun setDayMode(enabled: Boolean) {
        write { store.updateSettings { it.copy(dayMode = enabled) } }
    }

    /** 界面缩放：1.0 / 1.15 / 1.25 / 1.4，立即生效（Compose 的 LocalDensity）。 */
    fun setUiScale(scale: Float) {
        write { store.updateSettings { it.copy(uiScale = scale.coerceIn(0.8f, 1.6f)) } }
    }

    /** 方屏圆角半径：把内容裁进硬件圆角，避免字被切在角上。 */
    fun setCornerRadius(radiusDp: Float) {
        write { store.updateSettings { it.copy(cornerRadiusDp = radiusDp.coerceIn(0f, 120f)) } }
    }

    fun exportCsv() {
        val snapshot = _state.value
        _state.update { it.copy(exporting = true) }
        scope.launch {
            val records = runCatching { store.allRecords().first() }.getOrDefault(emptyMap())
            val path = withContext(Dispatchers.IO) {
                runCatching { CsvExporter.export(appContext, snapshot.students, records, snapshot.mode) }
                    .getOrDefault("")
            }
            val info = if (path.isEmpty()) "没有可导出的记录" else path
            runCatching { store.updateSettings { it.copy(lastExportInfo = info) } }
            _state.update { it.copy(exporting = false) }
            _messages.tryEmit(
                if (path.isEmpty()) "没有可导出的记录" else "已导出 CSV，数据线连电脑可拷出"
            )
        }
    }

    // ---------- 内部 ----------

    private fun write(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onFailure { _messages.tryEmit("保存失败：${it.message ?: "未知错误"}") }
        }
    }

    private companion object {
        const val HISTORY_LIMIT = StateAssembler.HISTORY_LIMIT
        const val WEEK_DAYS = StateAssembler.WEEK_DAYS
    }
}
