package com.juhao.rollcall.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.juhao.rollcall.data.AttendanceStatus
import com.juhao.rollcall.data.DaySummary
import com.juhao.rollcall.data.Student
import com.juhao.rollcall.theme.StatusPalette
import com.juhao.rollcall.theme.attendanceColor
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.components.SectionHeader
import com.juhao.rollcall.utils.LocalScreenShape
import com.juhao.rollcall.utils.ScreenShape
import com.juhao.rollcall.utils.rememberAdaptiveTransformationSpec

/**
 * 某一天的明细：汇总 + 出勤异常学生 + 作业未交名单。
 * 只读 [day] 与当前名单 [students]，不碰 DataStore。
 */
@Composable
fun DayDetailScreen(
    day: DaySummary,
    students: List<Student>,
    onBack: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    val counts = day.counts

    // 状态非「到」的人（null = 逐个模式下没点，不算异常）。
    val exceptions = students.mapNotNull { student ->
        val status = day.attendance[student.id]
        if (status != null && status != AttendanceStatus.PRESENT) student to status else null
    }
    // 缺省 = 未交；只有当天确实记录过作业才统计，避免把「没记录」误报成全员未交。
    val missingNames = if (day.homeworkMarked) {
        students.filter { day.homework[it.id] == false }.map { it.name }
    } else {
        emptyList<String>()
    }

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
                        Text(text = day.label)
                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SummaryCard(
                    countsLabel = "到 ${counts.present} · 假 ${counts.leave} · " +
                        "迟 ${counts.late} · 缺 ${counts.absent}",
                    homeworkLabel = "作业已交 ${day.homeworkSubmitted}/${day.totalStudents}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                )
            }

            item {
                SectionHeader(
                    title = "出勤异常",
                    subtitle = if (day.attendanceMarked) "共 ${exceptions.size} 人" else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 2.dp),
                )
            }
            if (!day.attendanceMarked) {
                item {
                    HintText(
                        text = "当天没有记录出勤",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 6.dp),
                    )
                }
            } else if (exceptions.isEmpty()) {
                item {
                    HintText(
                        text = "全员到齐",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 6.dp),
                    )
                }
            } else {
                items(
                    count = exceptions.size,
                    key = { index -> exceptions[index].first.id },
                ) { index ->
                    val (student, status) = exceptions[index]
                    ExceptionRow(
                        name = student.name,
                        status = status,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
            }

            item {
                SectionHeader(
                    title = "作业未交",
                    subtitle = when {
                        !day.homeworkMarked -> null
                        missingNames.isEmpty() -> null
                        else -> "共 ${missingNames.size} 人"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 2.dp),
                )
            }
            when {
                !day.homeworkMarked -> {
                    item {
                        HintText(
                            text = "当天没有记录作业",
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(vertical = 6.dp),
                        )
                    }
                }

                missingNames.isEmpty() -> {
                    item {
                        HintText(
                            text = "作业全部已交",
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(vertical = 6.dp),
                        )
                    }
                }

                else -> {
                    item {
                        MissingNamesCard(
                            names = missingNames,
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                        )
                    }
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
                        .padding(top = 10.dp, bottom = 10.dp),
                )
            }
        }
    }
}

/** 当天汇总卡：出勤四态 + 作业已交人数。 */
@Composable
private fun SummaryCard(
    countsLabel: String,
    homeworkLabel: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = countsLabel,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = homeworkLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 一条出勤异常：名字 + 状态色标签。 */
@Composable
private fun ExceptionRow(
    name: String,
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
) {
    val accent = attendanceColor(status)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = accent,
                maxLines = 1,
            )
        }
    }
}

/** 作业未交名单：顿号连接成一段可换行的文字。 */
@Composable
private fun MissingNamesCard(
    names: List<String>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = names.joinToString("、"),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
            color = StatusPalette.missing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
