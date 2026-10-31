package com.juhao.rollcall.state

import com.juhao.rollcall.data.AttendanceCounts
import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.data.AttendanceStatus
import com.juhao.rollcall.data.DaySummary
import com.juhao.rollcall.data.Student
import com.juhao.rollcall.data.WeekStats

/**
 * 整个 App 只有这一份界面状态。
 * 页面全部是「state + 回调」的纯函数，不直接碰 DataStore。
 */
data class UiState(
    val loading: Boolean = true,
    val today: String = "",
    /** "10月31日 周五 · 今天" */
    val headerLabel: String = "",
    val students: List<Student> = emptyList(),
    val mode: AttendanceMode = AttendanceMode.FAST,
    val dayMode: Boolean = false,
    /** 界面缩放倍数（1.0 = 原始，1.25 = 大字）。 */
    val uiScale: Float = 1.25f,
    /** 方屏圆角半径（dp，物理尺寸，不随缩放变）。 */
    val cornerRadiusDp: Float = 68f,
    /** 已解析的出勤状态：null = 未点（只在“逐个”模式出现）。 */
    val attendance: Map<Long, AttendanceStatus?> = emptyMap(),
    val counts: AttendanceCounts = AttendanceCounts(),
    /** 作业：true = 已交，false = 未交（缺省未交）。 */
    val homework: Map<Long, Boolean> = emptyMap(),
    val homeworkSubmitted: Int = 0,
    val week: WeekStats = WeekStats(),
    val history: List<DaySummary> = emptyList(),
    val lastExportInfo: String = "",
    val exporting: Boolean = false,
) {
    val total: Int get() = students.size
    val homeworkMissing: Int get() = (total - homeworkSubmitted).coerceAtLeast(0)
    val attendanceDone: Boolean get() = counts.unmarked == 0
}
