package com.juhao.rollcall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.data.Student

/**
 * 一个学生 = 一个格子。三种视觉状态，必须一眼分得清：
 *
 * - [confirmed]：已确认正常（到 / 已交）→ 明显的一层状态色底 + 状态色描边 + 状态色小字
 * - [emphasized]：异常（请假 / 迟到 / 缺勤 / 未交）→ 更重的染色 + 粗描边
 * - 都不是：未点 / 未记录 → 深色底 + 灰字
 *
 * 之前“到”只有一条很淡的描边，老师会误以为没点到，所以这里把确认态也染上底色。
 */
@Composable
fun StudentCell(
    name: String,
    statusLabel: String?,
    accent: Color,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    confirmed: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(13.dp)

    val background = when {
        emphasized -> accent.copy(alpha = 0.30f)
        confirmed -> accent.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        emphasized -> accent.copy(alpha = 0.95f)
        confirmed -> accent.copy(alpha = 0.70f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val labelColor = when {
        emphasized || confirmed -> accent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 3.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = if (confirmed || emphasized) FontWeight.Medium else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (statusLabel != null) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = if (confirmed || emphasized) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 学生宫格：3 列，一屏 4 行左右。
 * 40 人的班 = 不到 4 屏，滑动一下就到头，比逐行列表点起来快。
 */
@Composable
fun StudentGrid(
    students: List<Student>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    cellHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
    cell: @Composable (Student) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(items = students, key = { it.id }) { student ->
            Box(modifier = Modifier.height(cellHeight)) {
                cell(student)
            }
        }
    }
}
