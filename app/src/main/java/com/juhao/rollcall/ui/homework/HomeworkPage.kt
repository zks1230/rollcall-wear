package com.juhao.rollcall.ui.homework

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.theme.StatusPalette
import com.juhao.rollcall.theme.homeworkColor
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.ConfirmActionButton
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.components.SectionHeader
import com.juhao.rollcall.ui.components.StatusLegend
import com.juhao.rollcall.ui.components.StudentCell
import com.juhao.rollcall.ui.components.StudentGrid

/**
 * 收作业页（HorizontalPager 的第 2 页）。
 *
 * 语境：老师收作业本时大多数人都会交，所以默认「未交」是红底，
 * 点一下变绿；「全交」用于大部分人交齐后一键置绿，再补点少数没交的。
 * 「全未交」会清空所有人，必须二次确认，防止误触。
 *
 * 页面是纯函数：只读 [UiState]、只回调，不直接碰 DataStore。
 */
@Composable
fun HomeworkPage(
    state: UiState,
    onToggle: (Long) -> Unit,
    onAllSubmitted: () -> Unit,
    onAllMissing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 6.dp),
    ) {
        // 标题行：左「收作业」+ 日期，右侧「已交 X/40 · 未交 Y」。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                title = "收作业",
                subtitle = state.headerLabel.ifBlank { null },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "已交 ${state.homeworkSubmitted}/${state.total} · 未交 ${state.homeworkMissing}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 操作行：全交（绿）/ 全未交（红，二次确认）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                text = "全交",
                onClick = onAllSubmitted,
                modifier = Modifier.weight(1f),
                accent = StatusPalette.submitted,
            )
            ConfirmActionButton(
                text = "全未交",
                confirmText = "确认全部未交?",
                onConfirm = onAllMissing,
                modifier = Modifier.weight(1f),
                accent = StatusPalette.missing,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (state.students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                HintText("名单为空，去「统计」页 → 名单管理 添加学生")
            }
        } else {
            // 3 列宫格：一屏约 4 行，40 人的班滑两下就到底。
            StudentGrid(
                students = state.students,
                modifier = Modifier.weight(1f),
                cellHeight = 48.dp,
            ) { student ->
                val submitted = state.homework[student.id] ?: false
                StudentCell(
                    name = student.name,
                    statusLabel = if (submitted) "交" else "未交",
                    accent = homeworkColor(submitted),
                    emphasized = !submitted,
                    onClick = { onToggle(student.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 底部图例：绿 = 已交，红 = 未交。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            StatusLegend(
                items = listOf(
                    "已交" to StatusPalette.submitted,
                    "未交" to StatusPalette.missing,
                ),
            )
        }
    }
}
