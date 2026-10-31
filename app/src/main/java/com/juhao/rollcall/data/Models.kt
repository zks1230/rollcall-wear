package com.juhao.rollcall.data

import kotlinx.serialization.Serializable

/**
 * 出勤四态。枚举顺序即“点一下循环”的顺序。
 */
enum class AttendanceStatus(val label: String, val shortLabel: String) {
    PRESENT("到", "到"),
    LEAVE("请假", "假"),
    LATE("迟到", "迟"),
    ABSENT("缺勤", "缺"),
}

/**
 * FAST：默认全员到齐，只点异常的人（跑操场的快模式）。
 * STRICT：默认未点，逐个点确认（正式考勤）。
 */
enum class AttendanceMode {
    FAST,
    STRICT;

    val label: String
        get() = when (this) {
            FAST -> "快速"
            STRICT -> "逐个"
        }
}

@Serializable
data class Student(
    val id: Long,
    val name: String,
    val order: Int = 0,
)

@Serializable
data class DayRecord(
    /**
     * 那一天用的是哪种点名模式。历史按这个解析：
     * 快速模式下“没标记 = 到齐”，逐个模式下“没标记 = 未点”，
     * 不能因为以后切换了模式，把过去几天读成“全员未点”。
     */
    val mode: AttendanceMode = AttendanceMode.FAST,
    /** key = studentId.toString()。只存“显式标记”，缺省含义由 [mode] 决定。 */
    val attendance: Map<String, AttendanceStatus> = emptyMap(),
    /** key = studentId.toString()，value = 是否交了作业。缺省 = 未交。 */
    val homework: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class Settings(
    val mode: AttendanceMode = AttendanceMode.FAST,
    val dayMode: Boolean = false,
    /** 界面缩放：1.0 = 原始大小，1.25 = 常见的大字模式。 */
    val uiScale: Float = DEFAULT_UI_SCALE,
    /** 方屏圆角半径（物理 dp，不随 uiScale 缩放），用于把内容裁进屏幕圆角。 */
    val cornerRadiusDp: Float = DEFAULT_CORNER_RADIUS_DP,
    /** 最近一次导出结果，展示在“导出”按钮下方。 */
    val lastExportInfo: String = "",
    /** 首次启动写入内置名单后置 true，避免删空名单后被重新种入。 */
    val seeded: Boolean = false,
)

/** 界面缩放可选档位。 */
val UI_SCALE_OPTIONS = listOf(1.0f, 1.15f, 1.25f, 1.4f)

/** 圆角可选档位（0 = 不裁切）。 */
val CORNER_RADIUS_OPTIONS = listOf(0f, 40f, 60f, 80f)

const val DEFAULT_UI_SCALE = 1.25f
const val DEFAULT_CORNER_RADIUS_DP = 60f

/** 出勤统计（某一天）。 */
data class AttendanceCounts(
    val present: Int = 0,
    val leave: Int = 0,
    val late: Int = 0,
    val absent: Int = 0,
    val unmarked: Int = 0,
) {
    val total: Int get() = present + leave + late + absent + unmarked
    val marked: Int get() = total - unmarked
    val exceptions: Int get() = leave + late + absent
}

/** 某个学生在一段时间内的计数。 */
data class StudentStat(
    val student: Student,
    val count: Int,
)

/** 近 7 天统计。 */
data class WeekStats(
    val days: Int = 7,
    val absentRank: List<StudentStat> = emptyList(),
    val leaveRank: List<StudentStat> = emptyList(),
    val lateRank: List<StudentStat> = emptyList(),
    val homeworkMissingRank: List<StudentStat> = emptyList(),
    val totalAbsent: Int = 0,
    val totalLeave: Int = 0,
    val totalLate: Int = 0,
    val totalMissing: Int = 0,
    val daysWithAttendance: Int = 0,
    val daysWithHomework: Int = 0,
)

/** 历史里的一天（已解析成“当前名单口径”）。 */
data class DaySummary(
    val date: String,
    val label: String,
    val counts: AttendanceCounts,
    val homeworkSubmitted: Int,
    val totalStudents: Int,
    val attendance: Map<Long, AttendanceStatus?>,
    val homework: Map<Long, Boolean>,
    val attendanceMarked: Boolean,
    val homeworkMarked: Boolean,
) {
    val homeworkMissing: Int get() = (totalStudents - homeworkSubmitted).coerceAtLeast(0)
}
