package com.juhao.rollcall.state

import com.juhao.rollcall.data.AttendanceLogic
import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.data.DateFormats
import com.juhao.rollcall.data.DayRecord
import com.juhao.rollcall.data.DaySummary
import com.juhao.rollcall.data.Settings
import com.juhao.rollcall.data.StatsCalculator
import com.juhao.rollcall.data.Student

/**
 * 把「DataStore 里的原始数据」压成「界面要的一份状态」。
 *
 * 单独放一个纯对象（不碰 Context / DataStore），是为了能在 JVM 上直接单测：
 * 计数、历史排序、周统计这些容易出错的地方都在这里。
 */
object StateAssembler {

    const val HISTORY_LIMIT = 60
    const val WEEK_DAYS = 7

    fun assemble(
        students: List<Student>,
        settings: Settings,
        records: Map<String, DayRecord>,
        date: String,
        today: String = date,
        historyLimit: Int = HISTORY_LIMIT,
        weekDays: Int = WEEK_DAYS,
    ): UiState {
        val record = records[date] ?: DayRecord()
        val marks = record.attendance
        val attendance = students.associate { student ->
            student.id to AttendanceLogic.resolve(marks[student.id.toString()], settings.mode)
        }
        val homework = students.associate { student ->
            student.id to (record.homework[student.id.toString()] ?: false)
        }
        val history = records.keys
            .sortedDescending()
            .take(historyLimit)
            .map { day -> summaryOf(day, students, records[day] ?: DayRecord(), today) }

        return UiState(
            loading = false,
            today = date,
            headerLabel = DateFormats.headerLabel(date, today),
            students = students,
            mode = settings.mode,
            dayMode = settings.dayMode,
            uiScale = settings.uiScale,
            cornerRadiusDp = settings.cornerRadiusDp,
            attendance = attendance,
            counts = AttendanceLogic.counts(students, marks, settings.mode),
            homework = homework,
            homeworkSubmitted = homework.values.count { it },
            week = StatsCalculator.week(students, records, date, weekDays),
            history = history,
            lastExportInfo = settings.lastExportInfo,
        )
    }

    /**
     * 历史某天：用那天记录里的模式来解析（[DayRecord.mode]），
     * 这样今天把模式切到“逐个”，过去几天的“全员到齐”不会被读成“全员未点”。
     */
    fun summaryOf(
        date: String,
        students: List<Student>,
        record: DayRecord,
        today: String = date,
    ): DaySummary {
        val mode = record.mode
        val homework = students.associate { student ->
            student.id to (record.homework[student.id.toString()] ?: false)
        }
        return DaySummary(
            date = date,
            label = DateFormats.historyLabel(date, today),
            counts = AttendanceLogic.counts(students, record.attendance, mode),
            homeworkSubmitted = homework.values.count { it },
            totalStudents = students.size,
            attendance = students.associate { student ->
                student.id to AttendanceLogic.resolve(record.attendance[student.id.toString()], mode)
            },
            homework = homework,
            attendanceMarked = record.attendance.isNotEmpty(),
            homeworkMarked = record.homework.isNotEmpty(),
        )
    }
}
