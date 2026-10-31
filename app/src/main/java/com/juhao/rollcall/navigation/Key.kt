package com.juhao.rollcall.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppKey : NavKey

/** 主页：出勤 / 作业 / 统计 三页左右滑。 */
@Serializable
data object HomeKey : AppKey

@Serializable
data object HistoryKey : AppKey

@Serializable
data class DayKey(val date: String) : AppKey

@Serializable
data object RosterKey : AppKey

/** studentId < 0 表示新增学生。 */
@Serializable
data class StudentEditKey(val studentId: Long) : AppKey

@Serializable
data object ImportKey : AppKey

@Serializable
data object SettingsKey : AppKey
