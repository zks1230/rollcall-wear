package com.juhao.rollcall.state

import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.data.AttendanceStatus
import com.juhao.rollcall.data.DayRecord
import com.juhao.rollcall.data.Settings
import com.juhao.rollcall.data.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 数据 → 界面状态的组装逻辑（纯函数，可在 JVM 上跑）。 */
class StateAssemblerTest {

    private val students = listOf(
        Student(1L, "张三", 0),
        Student(2L, "李四", 1),
        Student(3L, "王五", 2),
    )

    private val records = mapOf(
        "2026-10-29" to DayRecord(
            attendance = mapOf("1" to AttendanceStatus.ABSENT),
            homework = mapOf("2" to true),
        ),
        "2026-10-30" to DayRecord(
            attendance = mapOf("2" to AttendanceStatus.LEAVE),
            homework = mapOf("1" to false),
        ),
        "2026-10-31" to DayRecord(
            attendance = mapOf("3" to AttendanceStatus.ABSENT),
            homework = mapOf("2" to true, "3" to false),
        ),
    )

    @Test
    fun `快速模式下今天没标记的人默认到齐`() {
        val state = StateAssembler.assemble(students, Settings(), records, "2026-10-31")

        assertEquals(3, state.total)
        assertEquals(2, state.counts.present) // 上午没点过的张三、李四
        assertEquals(1, state.counts.absent) // 王五显式标了缺勤
        assertEquals(0, state.counts.unmarked)
        assertEquals(1, state.homeworkSubmitted) // 只有李四显式置了“已交”
        assertEquals(2, state.homeworkMissing)
        assertEquals("10月31日 周六 · 今天", state.headerLabel)
    }

    @Test
    fun `逐个模式下没点的人算未点`() {
        val state = StateAssembler.assemble(
            students,
            Settings(mode = AttendanceMode.STRICT),
            records,
            "2026-10-31",
        )
        assertEquals(0, state.counts.present)
        assertEquals(1, state.counts.absent)
        assertEquals(2, state.counts.unmarked)
        assertEquals(false, state.attendanceDone)
    }

    @Test
    fun `历史按日期倒序，并带今天昨天文案`() {
        val state = StateAssembler.assemble(students, Settings(), records, "2026-10-31")
        assertEquals(
            listOf("2026-10-31", "2026-10-30", "2026-10-29"),
            state.history.map { it.date },
        )
        assertEquals("今天", state.history[0].label)
        assertEquals("昨天", state.history[1].label)
        assertEquals("10月29日 周四", state.history[2].label)
        assertTrue(state.history[0].attendanceMarked)
    }

    @Test
    fun `历史长度受上限约束`() {
        val state = StateAssembler.assemble(
            students,
            Settings(),
            records,
            "2026-10-31",
            historyLimit = 2,
        )
        assertEquals(2, state.history.size)
    }

    @Test
    fun `没有任何记录时状态是干净的空`() {
        val state = StateAssembler.assemble(students, Settings(), emptyMap(), "2026-10-31")
        assertTrue(state.history.isEmpty())
        assertEquals(0, state.week.daysWithAttendance)
        assertEquals(3, state.counts.present)
        assertEquals(0, state.homeworkSubmitted)
    }

    @Test
    fun `周统计窗口是今天往前 7 天`() {
        val state = StateAssembler.assemble(students, Settings(), records, "2026-10-31")
        assertEquals(2, state.week.totalAbsent) // 10-29 张三、10-31 王五
        assertEquals(1, state.week.totalLeave) // 10-30 李四
        assertEquals(3, state.week.daysWithAttendance)
        assertEquals(3, state.week.daysWithHomework)
    }
}
