package com.juhao.rollcall.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.juhao.rollcall.data.StudentStat
import com.juhao.rollcall.data.WeekStats
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.theme.StatusPalette
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.components.SectionHeader
import com.juhao.rollcall.utils.LocalScreenShape
import com.juhao.rollcall.utils.ScreenShape
import com.juhao.rollcall.utils.rememberAdaptiveTransformationSpec

/**
 * 统计页（HomeScreen 的 HorizontalPager 第 3 页）。
 *
 * 自上而下：标题 → 近 7 天概览（缺勤/请假/迟到/作业未交）→ 缺勤排行 → 作业未交排行
 * → 历史 / 名单 / 导出 / 设置。纯展示：只读 [UiState]，所有动作都回调给上层。
 */
@Composable
fun StatsPage(
    state: UiState,
    onOpenHistory: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenSettings: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    val week = state.week
    val absentRank = week.absentRank.take(5)
    val missingRank = week.homeworkMissingRank.take(5)
    val absentMax = absentRank.maxOfOrNull { it.count } ?: 0
    val missingMax = missingRank.maxOfOrNull { it.count } ?: 0

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            modifier = modifier,
            // 统计页在 HomeScreen 的 pager 里，外层已经统一留了顶部 24dp / 底部 4dp，
            // 这里不再重复吃 ScreenScaffold 自带的 ~10% 屏高内边距，标题才和另外两页齐平。
            contentPadding = PaddingValues(0.dp),
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        Text(text = "统计")
                        Text(
                            text = "近 ${week.days} 天",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                OverviewCard(
                    week = week,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                )
            }

            item {
                SectionHeader(
                    title = "缺勤排行",
                    subtitle = "近 ${week.days} 天 · 前 5",
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 12.dp)
                        .padding(top = 6.dp, bottom = 2.dp),
                )
            }
            if (absentMax == 0) {
                item {
                    HintText(
                        text = "近 ${week.days} 天没有缺勤",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 6.dp),
                    )
                }
            } else {
                items(
                    count = absentRank.size,
                    key = { index -> "absent-${absentRank[index].student.id}" },
                ) { index ->
                    RankRow(
                        stat = absentRank[index],
                        maxCount = absentMax,
                        accent = StatusPalette.absent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "作业未交排行",
                    subtitle = "近 ${week.days} 天 · 前 5",
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 2.dp),
                )
            }
            if (missingMax == 0) {
                item {
                    HintText(
                        text = "近 ${week.days} 天没有作业未交",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 6.dp),
                    )
                }
            } else {
                items(
                    count = missingRank.size,
                    key = { index -> "missing-${missingRank[index].student.id}" },
                ) { index ->
                    RankRow(
                        stat = missingRank[index],
                        maxCount = missingMax,
                        accent = StatusPalette.missing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
            }

            item {
                AppButton(
                    text = "历史记录",
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 10.dp)
                        .padding(top = 8.dp),
                )
            }

            item {
                AppButton(
                    text = "名单管理",
                    onClick = onOpenRoster,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 10.dp)
                        .padding(top = 5.dp),
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 10.dp)
                        .padding(top = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppButton(
                        text = if (state.exporting) "导出中…" else "导出 CSV",
                        onClick = onExport,
                        enabled = !state.exporting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.lastExportInfo.isNotBlank()) {
                        Text(
                            text = "最近导出：" + state.lastExportInfo.substringAfterLast('/'),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        )
                        HintText(
                            text = "数据线连电脑可拷出",
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }

            item {
                AppButton(
                    text = "设置",
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 10.dp)
                        .padding(top = 5.dp, bottom = 14.dp),
                )
            }
        }
    }
}

/** 概览卡：一行 4 个数字块 + 「已记录 X 天出勤 · Y 天作业」。 */
@Composable
private fun OverviewCard(week: WeekStats, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), shape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            OverviewBlock(
                label = "缺勤",
                value = week.totalAbsent,
                accent = StatusPalette.absent,
                modifier = Modifier.weight(1f),
            )
            OverviewBlock(
                label = "请假",
                value = week.totalLeave,
                accent = StatusPalette.leave,
                modifier = Modifier.weight(1f),
            )
            OverviewBlock(
                label = "迟到",
                value = week.totalLate,
                accent = StatusPalette.late,
                modifier = Modifier.weight(1f),
            )
            OverviewBlock(
                label = "作业未交",
                value = week.totalMissing,
                accent = StatusPalette.missing,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "已记录 ${week.daysWithAttendance} 天出勤 · ${week.daysWithHomework} 天作业",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

@Composable
private fun OverviewBlock(
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 17.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 排行的一行：名字 + 次数 + 一条按 count / maxCount 画宽的进度条。 */
@Composable
private fun RankRow(
    stat: StudentStat,
    maxCount: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val fraction = if (maxCount <= 0) {
        0f
    } else {
        (stat.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    }
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f), shape)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stat.student.name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${stat.count} 次",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = accent,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}
