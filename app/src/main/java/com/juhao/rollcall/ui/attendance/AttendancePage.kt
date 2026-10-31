package com.juhao.rollcall.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.data.AttendanceMode
import com.juhao.rollcall.data.AttendanceStatus
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.theme.StatusPalette
import com.juhao.rollcall.theme.attendanceColor
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.AppPill
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.components.StatusLegend
import com.juhao.rollcall.ui.components.StudentCell
import com.juhao.rollcall.ui.components.StudentGrid

/**
 * 出勤点名页（HomeScreen 的 HorizontalPager 第一页）。
 *
 * 自上而下：标题行 → 统计行 → 学生宫格（占满剩余高度，可滑）→ 底部图例。
 * 页面本身不碰 DataStore，点格子只回调 [onCycle]，模式/清空都由上层处理。
 * 顶部时间胶囊的 26dp 留白由外层统一处理，这里只留底部 6dp。
 */
@Composable
fun AttendancePage(
    state: UiState,
    onCycle: (Long) -> Unit,
    onClear: () -> Unit,
    onModeChange: (AttendanceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AttendanceHeader(state = state, onModeChange = onModeChange)
        AttendanceStatsRow(state = state, onClear = onClear)

        if (state.students.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                HintText("名单为空，去「统计」页 → 名单管理 添加学生")
            }
        } else {
            StudentGrid(
                students = state.students,
                modifier = Modifier.weight(1f),
                cellHeight = 48.dp,
            ) { student ->
                val status = state.attendance[student.id]
                val label = when {
                    status == null -> "未点"
                    status == AttendanceStatus.PRESENT -> "到"
                    else -> status.shortLabel
                }
                StudentCell(
                    name = student.name,
                    statusLabel = label,
                    accent = attendanceColor(status),
                    emphasized = status != null && status != AttendanceStatus.PRESENT,
                    confirmed = status == AttendanceStatus.PRESENT,
                    onClick = { onCycle(student.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AttendanceLegend(mode = state.mode)
    }
}

/** 标题「跑操点名」+ 日期，右侧「快速 / 逐个」模式胶囊。 */
@Composable
private fun AttendanceHeader(
    state: UiState,
    onModeChange: (AttendanceMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(top = 2.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "跑操点名",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = state.headerLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppPill(
            text = "快速",
            selected = state.mode == AttendanceMode.FAST,
            onClick = { onModeChange(AttendanceMode.FAST) },
        )
        AppPill(
            text = "逐个",
            selected = state.mode == AttendanceMode.STRICT,
            onClick = { onModeChange(AttendanceMode.STRICT) },
        )
    }
}

/** 统计「到 N · 假 N · 迟 N · 缺 N（· 未点 N）」，右侧「全到 / 重置」。 */
@Composable
private fun AttendanceStatsRow(
    state: UiState,
    onClear: () -> Unit,
) {
    val counts = state.counts
    val statsLabel = buildString {
        append("到 ").append(counts.present)
        append(" · 假 ").append(counts.leave)
        append(" · 迟 ").append(counts.late)
        append(" · 缺 ").append(counts.absent)
        if (state.mode == AttendanceMode.STRICT) append(" · 未点 ").append(counts.unmarked)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = statsLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = if (state.mode == AttendanceMode.FAST) "全到" else "重置",
            onClick = onClear,
            height = 28.dp,
        )
    }
}

/** 底部状态图例；逐个模式下多一个「未点」。 */
@Composable
private fun AttendanceLegend(mode: AttendanceMode) {
    val items = if (mode == AttendanceMode.STRICT) {
        listOf(
            "到" to StatusPalette.present,
            "请假" to StatusPalette.leave,
            "迟到" to StatusPalette.late,
            "缺勤" to StatusPalette.absent,
            "未点" to StatusPalette.unmarked,
        )
    } else {
        listOf(
            "到" to StatusPalette.present,
            "请假" to StatusPalette.leave,
            "迟到" to StatusPalette.late,
            "缺勤" to StatusPalette.absent,
        )
    }
    StatusLegend(
        items = items,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(top = 3.dp, bottom = 6.dp),
    )
}
