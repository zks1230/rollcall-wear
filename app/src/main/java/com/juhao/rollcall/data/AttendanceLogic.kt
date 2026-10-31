package com.juhao.rollcall.data

/** 点击循环 + 解析 + 计数，纯函数，方便单测。 */
object AttendanceLogic {

    /** 返回 next 的“显式标记”；null 表示删掉标记、回到模式默认值。 */
    fun next(current: AttendanceStatus?, mode: AttendanceMode): AttendanceStatus? = when (mode) {
        AttendanceMode.FAST -> when (current) {
            null, AttendanceStatus.PRESENT -> AttendanceStatus.LEAVE
            AttendanceStatus.LEAVE -> AttendanceStatus.LATE
            AttendanceStatus.LATE -> AttendanceStatus.ABSENT
            AttendanceStatus.ABSENT -> null
        }

        AttendanceMode.STRICT -> when (current) {
            null -> AttendanceStatus.PRESENT
            AttendanceStatus.PRESENT -> AttendanceStatus.LEAVE
            AttendanceStatus.LEAVE -> AttendanceStatus.LATE
            AttendanceStatus.LATE -> AttendanceStatus.ABSENT
            AttendanceStatus.ABSENT -> null
        }
    }

    /** 把“显式标记”解析成界面上的状态；null = 未点（只在逐个模式出现）。 */
    fun resolve(mark: AttendanceStatus?, mode: AttendanceMode): AttendanceStatus? =
        mark ?: if (mode == AttendanceMode.FAST) AttendanceStatus.PRESENT else null

    fun counts(students: List<Student>, marks: Map<String, AttendanceStatus>, mode: AttendanceMode): AttendanceCounts {
        var present = 0
        var leave = 0
        var late = 0
        var absent = 0
        var unmarked = 0
        students.forEach { student ->
            when (resolve(marks[student.id.toString()], mode)) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LEAVE -> leave++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.ABSENT -> absent++
                null -> unmarked++
            }
        }
        return AttendanceCounts(present, leave, late, absent, unmarked)
    }
}

/** 近 N 天统计，纯函数。 */
object StatsCalculator {

    fun windowDates(today: String, days: Int): List<String> {
        val end = runCatching { java.time.LocalDate.parse(today) }.getOrNull() ?: return emptyList()
        return (0 until days).map { end.minusDays(it.toLong()).toString() }
    }

    fun week(
        students: List<Student>,
        records: Map<String, DayRecord>,
        today: String,
        days: Int = 7,
    ): WeekStats {
        val dates = windowDates(today, days)
        fun rank(status: AttendanceStatus): List<StudentStat> = students.mapNotNull { student ->
            val count = dates.count { date ->
                records[date]?.attendance?.get(student.id.toString()) == status
            }
            if (count > 0) StudentStat(student, count) else null
        }.sortedByDescending { it.count }

        val missingRank = students.mapNotNull { student ->
            val count = dates.count { date ->
                records[date]?.homework?.get(student.id.toString()) == false
            }
            if (count > 0) StudentStat(student, count) else null
        }.sortedByDescending { it.count }

        return WeekStats(
            days = days,
            absentRank = rank(AttendanceStatus.ABSENT),
            leaveRank = rank(AttendanceStatus.LEAVE),
            lateRank = rank(AttendanceStatus.LATE),
            homeworkMissingRank = missingRank,
            totalAbsent = rank(AttendanceStatus.ABSENT).sumOf { it.count },
            totalLeave = rank(AttendanceStatus.LEAVE).sumOf { it.count },
            totalLate = rank(AttendanceStatus.LATE).sumOf { it.count },
            totalMissing = missingRank.sumOf { it.count },
            daysWithAttendance = dates.count { !records[it]?.attendance.isNullOrEmpty() },
            daysWithHomework = dates.count { !records[it]?.homework.isNullOrEmpty() },
        )
    }
}
