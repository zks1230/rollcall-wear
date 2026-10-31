package com.juhao.rollcall.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.rollcall.data.DaySummary
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.utils.LocalScreenShape
import com.juhao.rollcall.utils.ScreenShape
import com.juhao.rollcall.utils.rememberAdaptiveTransformationSpec

/**
 * 历史记录页：state.history 已按日期倒序，一天一张卡，点进去看当天明细。
 * 纯展示，返回 / 打开某天都回调给上层。
 */
@Composable
fun HistoryScreen(
    state: UiState,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    val history = state.history

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 不在 pager 里，自己给顶部时间胶囊留一点边距。
                        .padding(top = 10.dp)
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        ),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "历史记录")
                        Text(
                            text = "共 ${history.size} 天",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    HintText(
                        text = "还没有任何记录",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 12.dp),
                    )
                }
            } else {
                items(
                    count = history.size,
                    key = { index -> history[index].date },
                ) { index ->
                    HistoryRow(
                        day = history[index],
                        onClick = { onOpenDay(history[index].date) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
            }

            item {
                AppButton(
                    text = "返回",
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 10.dp)
                        .padding(top = 8.dp, bottom = 10.dp),
                )
            }
        }
    }
}

/** 一天一行：日期 + 「到/假/迟/缺」与作业未交人数；整卡可点。 */
@Composable
private fun HistoryRow(
    day: DaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val counts = day.counts
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "到 ${counts.present} · 假 ${counts.leave} · 迟 ${counts.late} · " +
                "缺 ${counts.absent} · 作业未交 ${day.homeworkMissing}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
