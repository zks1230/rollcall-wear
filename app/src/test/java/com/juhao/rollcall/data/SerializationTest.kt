package com.juhao.rollcall.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DataStore 里存的就是这几段 JSON，格式错了会直接丢数据，所以单独锁一遍。 */
class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `一天的记录可以原样往返`() {
        val record = DayRecord(
            mode = AttendanceMode.STRICT,
            attendance = mapOf(
                "1" to AttendanceStatus.ABSENT,
                "2" to AttendanceStatus.LEAVE,
                "3" to AttendanceStatus.LATE,
            ),
            homework = mapOf("1" to false, "2" to true),
        )
        val encoded = json.encodeToString(DayRecord.serializer(), record)
        val decoded = json.decodeFromString<DayRecord>(encoded)

        assertEquals(record, decoded)
        assertTrue(encoded.contains("ABSENT"))
        assertTrue(encoded.contains("STRICT"))
        // 学生 id 是字符串 key，不要退化成数组
        assertTrue(encoded.contains("\"1\""))
    }

    @Test
    fun `旧版没写模式的记录按快速模式解析`() {
        val decoded = json.decodeFromString<DayRecord>(
            """{"attendance":{"1":"ABSENT"},"homework":{}}"""
        )
        assertEquals(AttendanceMode.FAST, decoded.mode)
        assertEquals(AttendanceStatus.ABSENT, decoded.attendance["1"])
    }

    @Test
    fun `设置兼容未知字段和缺失字段`() {
        val decoded = json.decodeFromString<Settings>(
            """{"mode":"STRICT","futureField":123}"""
        )
        assertEquals(AttendanceMode.STRICT, decoded.mode)
        assertEquals(false, decoded.dayMode)
        assertEquals("", decoded.lastExportInfo)
        assertEquals(false, decoded.seeded)
        // 老版本 JSON 没有这两个字段时，要落到新默认值
        assertEquals(DEFAULT_UI_SCALE, decoded.uiScale, 0.001f)
        assertEquals(DEFAULT_CORNER_RADIUS_DP, decoded.cornerRadiusDp, 0.001f)
    }

    @Test
    fun `名单往返保持顺序`() {
        val students = listOf(
            Student(7L, "张三", 0),
            Student(9L, "李四", 1),
        )
        val encoded = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Student.serializer()), students)
        assertEquals(students, json.decodeFromString<List<Student>>(encoded))
    }

    @Test
    fun `默认设置就是快速模式`() {
        assertEquals(AttendanceMode.FAST, Settings().mode)
    }
}
