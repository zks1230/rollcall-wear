package com.juhao.rollcall.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

/**
 * 全局主题：颜色由强调色派生，强调色由“当前班级状态”决定
 * （全到 = 绿、有人请假迟到 = 黄、有人缺勤 = 红）。
 */
@Composable
fun RollCallTheme(
    accent: Color,
    isDay: Boolean,
    content: @Composable () -> Unit,
) {
    val scheme = remember(accent, isDay) {
        buildAccentScheme(
            base = if (isDay) dayBaseScheme() else darkBaseScheme(),
            accent = accent,
            isDay = isDay,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content,
    )
}
