package com.juhao.rollcall.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.HorizontalPagerScaffold
import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.ui.attendance.AttendancePage
import com.juhao.rollcall.ui.homework.HomeworkPage
import com.juhao.rollcall.ui.stats.StatsPage
import com.juhao.rollcall.utils.KeepScreenOn

/** 主页三页：出勤点名 / 收作业 / 统计。左右滑翻页。 */
@Composable
fun HomeScreen(
    state: UiState,
    onCycleAttendance: (Long) -> Unit,
    onClearAttendance: () -> Unit,
    onModeChange: (AttendanceMode) -> Unit,
    onToggleHomework: (Long) -> Unit,
    onAllHomeworkSubmitted: () -> Unit,
    onAllHomeworkMissing: () -> Unit,
    onExport: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    KeepScreenOn(enabled = true)

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
        ) { page ->
            // 顶部留出时间胶囊的位置，底部留出翻页圆点的位置。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp, bottom = 16.dp),
            ) {
                when (page) {
                    0 -> AttendancePage(
                        state = state,
                        onCycle = onCycleAttendance,
                        onClear = onClearAttendance,
                        onModeChange = onModeChange,
                    )

                    1 -> HomeworkPage(
                        state = state,
                        onToggle = onToggleHomework,
                        onAllSubmitted = onAllHomeworkSubmitted,
                        onAllMissing = onAllHomeworkMissing,
                    )

                    else -> StatsPage(
                        state = state,
                        onOpenHistory = onOpenHistory,
                        onOpenRoster = onOpenRoster,
                        onOpenSettings = onOpenSettings,
                        onExport = onExport,
                    )
                }
            }
        }
    }
}
