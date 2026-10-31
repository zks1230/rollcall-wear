package com.juhao.rollcall.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {

    private val students = listOf(
        Student(1L, "张三", 0),
        Student(2L, "李四", 1),
        Student(3L, "王五", 2),
    )

    @Test
    fun `统计窗口是最近七天含今天`() {
        val dates = StatsCalculator.windowDates("2026-10-31", 7)
        assertEquals(7, dates.size)
        assertEquals("2026-10-31", dates.first())
        assertEquals("2026-10-25", dates.last())
    }

    @Test
    fun `缺勤按次数排序，作业未交只算显式标记`() {
        val records = mapOf(
            "2026-10-30" to DayRecord(
                attendance = mapOf(
                    "1" to AttendanceStatus.ABSENT,
                    "2" to AttendanceStatus.LEAVE,
                ),
                homework = mapOf("1" to false, "2" to true),
            ),
            "2026-10-31" to DayRecord(
                attendance = mapOf("1" to AttendanceStatus.ABSENT),
                homework = mapOf("1" to false),
            ),
            // 窗口外的一天不该被算进来
            "2026-10-01" to DayRecord(
                attendance = mapOf("1" to AttendanceStatus.ABSENT, "3" to AttendanceStatus.ABSENT),
                homework = mapOf("3" to false),
            ),
        )

        val week = StatsCalculator.week(students, records, "2026-10-31", 7)

        assertEquals(1, week.absentRank.size)
        assertEquals(2, week.absentRank.first().count)
        assertEquals("张三", week.absentRank.first().student.name)

        assertEquals(1, week.leaveRank.size)
        assertEquals(1, week.homeworkMissingRank.size)
        assertEquals(2, week.homeworkMissingRank.first().count)

        assertEquals(2, week.totalAbsent)
        assertEquals(2, week.daysWithAttendance)
        assertEquals(2, week.daysWithHomework)
    }

    @Test
    fun `没有记录时统计为空`() {
        val week = StatsCalculator.week(students, emptyMap(), "2026-10-31", 7)
        assertTrue(week.absentRank.isEmpty())
        assertTrue(week.homeworkMissingRank.isEmpty())
        assertEquals(0, week.daysWithAttendance)
    }
}
