package com.juhao.rollcall.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 日期/时间文案（中文），纯函数。 */
object DateFormats {

    private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val monthDay = DateTimeFormatter.ofPattern("M月d日")
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    fun todayString(): String = LocalDate.now().toString()

    fun nowClock(): String = LocalTime.now().format(clock)

    fun weekdayOf(date: String): String {
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return ""
        return weekdays[parsed.dayOfWeek.value - 1]
    }

    fun monthDayOf(date: String): String {
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
        return parsed.format(monthDay)
    }

    /** "10月31日 周五" */
    fun shortLabel(date: String): String = "${monthDayOf(date)} ${weekdayOf(date)}".trim()

    /** 今天 / 昨天 / 10月29日 周三 */
    fun historyLabel(date: String, today: String): String {
        val t = runCatching { LocalDate.parse(today) }.getOrNull()
        val d = runCatching { LocalDate.parse(date) }.getOrNull()
        if (t != null && d != null) {
            val diff = java.time.temporal.ChronoUnit.DAYS.between(d, t)
            if (diff == 0L) return "今天"
            if (diff == 1L) return "昨天"
        }
        return shortLabel(date)
    }

    /** "10月31日 周五 · 今天" 之类，用于页头 */
    fun headerLabel(date: String, today: String): String {
        val label = shortLabel(date)
        return if (date == today) "$label · 今天" else label
    }
}
