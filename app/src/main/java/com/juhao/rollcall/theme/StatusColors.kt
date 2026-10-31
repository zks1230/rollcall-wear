package com.juhao.rollcall.theme

import androidx.compose.ui.graphics.Color
import com.juhao.rollcall.data.AttendanceCounts
import com.juhao.rollcall.data.AttendanceStatus

/**
 * 状态色是“语义色”，固定不随主题变；主题的强调色从状态汇总里取。
 */
object StatusPalette {
    val present = Color(0xFF56D9A2)
    val leave = Color(0xFFF3C764)
    val late = Color(0xFF74C8FF)
    val absent = Color(0xFFFF7C6E)
    val unmarked = Color(0xFF7C8494)

    val submitted = Color(0xFF56D9A2)
    val missing = Color(0xFFFF7C6E)
}

fun attendanceColor(status: AttendanceStatus?): Color = when (status) {
    AttendanceStatus.PRESENT -> StatusPalette.present
    AttendanceStatus.LEAVE -> StatusPalette.leave
    AttendanceStatus.LATE -> StatusPalette.late
    AttendanceStatus.ABSENT -> StatusPalette.absent
    null -> StatusPalette.unmarked
}

fun homeworkColor(submitted: Boolean): Color =
    if (submitted) StatusPalette.submitted else StatusPalette.missing

/** 出勤页的强调色：有缺勤→红，有请假/迟到→黄，全到→绿。 */
fun accentForCounts(counts: AttendanceCounts): Color = when {
    counts.absent > 0 -> AccentSeeds.attendanceAlert
    counts.leave + counts.late > 0 -> AccentSeeds.attendanceWarn
    else -> AccentSeeds.attendanceClear
}
