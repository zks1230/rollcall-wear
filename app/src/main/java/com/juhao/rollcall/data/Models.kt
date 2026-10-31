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
    /** key = studentId.toString()。只存“显式标记”，缺省含义由 [AttendanceMode] 决定。 */
    val attendance: Map<String, AttendanceStatus> = emptyMap(),
    /** key = studentId.toString()，value = 是否交了作业。缺省 = 未交。 */
    val homework: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class Settings(
    val mode: AttendanceMode = AttendanceMode.FAST,
    val dayMode: Boolean = false,
    /** 最近一次导出结果，展示在“导出”按钮下方。 */
    val lastExportInfo: String = "",
    /** 首次启动写入内置名单后置 true，避免删空名单后被重新种入。 */
    val seeded: Boolean = false,
)

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
