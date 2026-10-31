package com.juhao.rollcall.data

import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val students = listOf(
        Student(1L, "张三", 0),
        Student(2L, "李四", 1),
    )

    private val fastRecord = mapOf(
        "2026-10-31" to DayRecord(
            mode = AttendanceMode.FAST,
            attendance = mapOf("1" to AttendanceStatus.ABSENT),
            homework = mapOf("2" to true),
        ),
    )

    private val strictRecord = mapOf(
        "2026-10-31" to DayRecord(
            mode = AttendanceMode.STRICT,
            attendance = mapOf("1" to AttendanceStatus.PRESENT),
            homework = mapOf("2" to true),
        ),
    )

    @Test
    fun `快速模式下未标记默认到齐`() {
        val csv = CsvExporter.buildCsv(students, fastRecord)
        val lines = csv.trim().split("\r\n")
        assertTrue(lines.first().contains("日期"))
        assertTrue(lines.any { it.contains("张三") && it.contains("缺勤") })
        assertTrue(lines.any { it.contains("李四") && it.contains("到") })
        // 李四显式交了作业，张三没标记 = 未记
        assertTrue(lines.any { it.contains("李四") && it.contains("已交") })
        assertTrue(lines.any { it.contains("张三") && it.contains("未记") })
    }

    @Test
    fun `逐个模式下没点的显示未点`() {
        val csv = CsvExporter.buildCsv(students, strictRecord)
        assertTrue(csv.contains("李四,未点"))
        assertTrue(csv.contains("张三,到"))
    }

    @Test
    fun `历史记录的模式不受当前设置影响`() {
        // 同一条 FAST 记录，无论现在是什么模式，导出的“到/未点”都按当天模式算
        assertTrue(CsvExporter.buildCsv(students, fastRecord).contains("李四,到"))
        assertTrue(CsvExporter.buildCsv(students, strictRecord).contains("李四,未点"))
    }
}
