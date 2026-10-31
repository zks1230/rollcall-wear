package com.juhao.rollcall.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceLogicTest {

    @Test
    fun `快速模式从默认的到开始，点一下变请假`() {
        // null 表示“没有显式标记”，在快速模式里就是默认的“到”
        assertEquals(AttendanceStatus.LEAVE, AttendanceLogic.next(null, AttendanceMode.FAST))
        assertEquals(AttendanceStatus.LEAVE, AttendanceLogic.next(AttendanceStatus.PRESENT, AttendanceMode.FAST))
        assertEquals(AttendanceStatus.LATE, AttendanceLogic.next(AttendanceStatus.LEAVE, AttendanceMode.FAST))
        assertEquals(AttendanceStatus.ABSENT, AttendanceLogic.next(AttendanceStatus.LATE, AttendanceMode.FAST))
        // 缺勤再点一下 = 清掉标记，回到默认的“到”
        assertNull(AttendanceLogic.next(AttendanceStatus.ABSENT, AttendanceMode.FAST))
    }

    @Test
    fun `逐个模式从 未点 开始点`() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceLogic.next(null, AttendanceMode.STRICT))
        assertEquals(AttendanceStatus.LEAVE, AttendanceLogic.next(AttendanceStatus.PRESENT, AttendanceMode.STRICT))
        assertEquals(AttendanceStatus.LATE, AttendanceLogic.next(AttendanceStatus.LEAVE, AttendanceMode.STRICT))
        assertEquals(AttendanceStatus.ABSENT, AttendanceLogic.next(AttendanceStatus.LATE, AttendanceMode.STRICT))
        assertNull(AttendanceLogic.next(AttendanceStatus.ABSENT, AttendanceMode.STRICT))
    }

    @Test
    fun `解析默认值`() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceLogic.resolve(null, AttendanceMode.FAST))
        assertNull(AttendanceLogic.resolve(null, AttendanceMode.STRICT))
        assertEquals(AttendanceStatus.LEAVE, AttendanceLogic.resolve(AttendanceStatus.LEAVE, AttendanceMode.FAST))
        assertEquals(AttendanceStatus.PRESENT, AttendanceLogic.resolve(AttendanceStatus.PRESENT, AttendanceMode.STRICT))
    }

    @Test
    fun `计数区分快速与逐个`() {
        val students = listOf(
            Student(1L, "张三"),
            Student(2L, "李四"),
            Student(3L, "王五"),
        )
        val marked = mapOf("2" to AttendanceStatus.ABSENT, "3" to AttendanceStatus.LEAVE)

        val fast = AttendanceLogic.counts(students, marked, AttendanceMode.FAST)
        assertEquals(1, fast.present)
        assertEquals(1, fast.absent)
        assertEquals(1, fast.leave)
        assertEquals(0, fast.unmarked)

        val strict = AttendanceLogic.counts(students, mapOf("1" to AttendanceStatus.PRESENT), AttendanceMode.STRICT)
        assertEquals(1, strict.present)
        assertEquals(2, strict.unmarked)
    }
}
