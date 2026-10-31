package com.juhao.rollcall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.rollcall.data.CORNER_RADIUS_OPTIONS
import com.juhao.rollcall.data.UI_SCALE_OPTIONS
import com.juhao.rollcall.state.UiState
import com.juhao.rollcall.ui.components.AppButton
import com.juhao.rollcall.ui.components.AppPill
import com.juhao.rollcall.ui.components.HintText
import com.juhao.rollcall.ui.components.SectionHeader
import kotlin.math.abs
import java.util.Locale

@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onDayModeChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onRestoreSeedRoster: () -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 22.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionHeader(title = "设置", subtitle = "点名 ${versionLabel()} · ${state.total} 人")

        // 界面缩放：改 Compose density，dp 和字号一起放大。
        SettingsBlock(
            title = "界面缩放",
            subtitle = "当前 ${formatScale(state.uiScale)}，1.25× 是常见档；改完立即生效",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                UI_SCALE_OPTIONS.forEach { scale ->
                    AppPill(
                        text = formatScale(scale),
                        selected = abs(state.uiScale - scale) < 0.01f,
                        onClick = { onUiScaleChange(scale) },
                    )
                }
            }
        }

        // 方屏圆角：把内容裁进硬件圆角，避免角上的字被切掉一半。
        SettingsBlock(
            title = "屏幕圆角",
            subtitle = if (state.cornerRadiusDp <= 0f) {
                "不裁切（直角屏）"
            } else {
                "按你的方屏大约 ${state.cornerRadiusDp.toInt()}dp 圆角裁切内容"
            },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CORNER_RADIUS_OPTIONS.forEach { radius ->
                    AppPill(
                        text = if (radius <= 0f) "无" else "${radius.toInt()}dp",
                        selected = abs(state.cornerRadiusDp - radius) < 0.5f,
                        onClick = { onCornerRadiusChange(radius) },
                    )
                }
            }
        }

        SettingsRow {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "日间模式",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (state.dayMode) "浅色背景，户外更清楚" else "深色背景，省电",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppButton(
                text = if (state.dayMode) "关闭" else "打开",
                onClick = { onDayModeChange(!state.dayMode) },
            )
        }

        AppButton(
            text = if (state.exporting) "导出中…" else "导出 CSV",
            onClick = onExport,
            enabled = !state.exporting,
            modifier = Modifier.fillMaxWidth(),
        )
        HintText(
            text = if (state.lastExportInfo.isBlank()) {
                "把每天的点名/作业记录导成 CSV，数据线连电脑拷出"
            } else {
                "最近导出：" + state.lastExportInfo.substringAfterLast('/')
            },
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        AppButton(
            text = "恢复内置名单",
            onClick = onRestoreSeedRoster,
            modifier = Modifier.fillMaxWidth(),
            filled = false,
        )
        HintText(
            text = "同名学生会保留原有记录；内置名单来自安装包里的 roster.csv",
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        AppButton(text = "返回", onClick = onBack, modifier = Modifier.fillMaxWidth(), filled = false)
    }
}

private fun formatScale(scale: Float): String = String.format(Locale.US, "%.2f×", scale)

/** 从 PackageManager 读版本号，避免以后改版本时忘了同步文案。 */
@Composable
private fun versionLabel(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("")
    }
}

@Composable
private fun SettingsBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SettingsRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
