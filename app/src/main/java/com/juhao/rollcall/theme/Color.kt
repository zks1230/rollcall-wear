package com.juhao.rollcall.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

/** 深夜基调：背景几乎黑，所有彩色都往强调色上偏。 */
private val darkNeutralBase: ColorScheme = ColorScheme(
    primary = Color(0xFF1F1F1F),
    onPrimary = Color(0xFF1F1F1F),
    primaryContainer = Color(0xFF1F1F1F),
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondary = Color(0xFF1F1F1F),
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF1F1F1F),
    onTertiary = Color(0xFF1F1F1F),
    tertiaryContainer = Color(0xFF1F1F1F),
    onTertiaryContainer = Color(0xFF1F1F1F),
    error = Color(0xFFFFC4BC),
    onError = Color(0xFF741210),
    errorContainer = Color(0xFF8C1515),
    onErrorContainer = Color(0xFFFFE5E1),
    background = Color(0xFF181A20),
    onBackground = Color(0xFFE8E8EF),
    onSurface = Color(0xFFE8E8EF),
    onSurfaceVariant = Color(0xFFD0D2DC),
    outline = Color(0xFF9A9CA5),
    outlineVariant = Color(0xFF525560),
    surfaceContainerLow = Color(0xFF20232A),
    surfaceContainer = Color(0xFF252830),
    surfaceContainerHigh = Color(0xFF30333B),
    primaryDim = Color(0xFF2E4A73),
    secondaryDim = Color(0xFF424B5C),
    tertiaryDim = Color(0xFF5A4260),
)

/** 日间基调：户外跑操时太阳下要看得清。 */
private val dayNeutralBase: ColorScheme = ColorScheme(
    primary = Color(0xFF1F1F1F),
    onPrimary = Color(0xFF1F1F1F),
    primaryContainer = Color(0xFF1F1F1F),
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondary = Color(0xFF1F1F1F),
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF1F1F1F),
    onTertiary = Color(0xFF1F1F1F),
    tertiaryContainer = Color(0xFF1F1F1F),
    onTertiaryContainer = Color(0xFF1F1F1F),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1B1B1F),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLow = Color(0xFFF2F3F7),
    surfaceContainer = Color(0xFFECEDF1),
    surfaceContainerHigh = Color(0xFFE6E7EB),
    primaryDim = Color(0xFFBFC8DB),
    secondaryDim = Color(0xFFC7CDD8),
    tertiaryDim = Color(0xFFD6C8DE),
)

/** 强调色种子。整张 ColorScheme 都从这些颜色派生。 */
object AccentSeeds {
    val attendanceClear = Color(0xFF5FD3A0)
    val attendanceWarn = Color(0xFFF0C264)
    val attendanceAlert = Color(0xFFFF8272)
    val homework = Color(0xFF77C4FF)
    val stats = Color(0xFFB39DFF)
    val roster = Color(0xFF7FD8C6)
}

private fun blend(base: Color, tint: Color, amount: Float): Color = Color(
    red = base.red * (1f - amount) + tint.red * amount,
    green = base.green * (1f - amount) + tint.green * amount,
    blue = base.blue * (1f - amount) + tint.blue * amount,
    alpha = 1f,
)

private fun toHsl(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h, s, l)
}

private fun hueToRgb(p: Float, q: Float, t0: Float): Float {
    var t = t0
    if (t < 0f) t += 1f
    if (t > 1f) t -= 1f
    return when {
        t < 1f / 6f -> p + (q - p) * 6f * t
        t < 1f / 2f -> q
        t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
        else -> p
    }
}

private fun fromHsl(h: Float, s: Float, l: Float): Color {
    if (s == 0f) return Color(l, l, l, 1f)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(
        red = hueToRgb(p, q, h + 1f / 3f),
        green = hueToRgb(p, q, h),
        blue = hueToRgb(p, q, h - 1f / 3f),
        alpha = 1f,
    )
}

private fun surfaceTint(
    hue: Float,
    sat: Float,
    baseL: Float,
    minS: Float,
    isDay: Boolean,
): Color = fromHsl(
    h = hue,
    s = (sat * if (isDay) 0.30f else 0.45f).coerceAtLeast(minS),
    l = if (isDay) baseL.coerceIn(0.86f, 0.99f) else baseL.coerceIn(0.08f, 0.90f),
)

/**
 * 整套 Material3 配色都从 [accent] 派生：
 * primary 提亮、background 用“强调色 + 低亮度 + 低饱和”合成，
 * surfaceContainer 分三级，文字/描边往强调色偏一点避免脱节。
 */
fun buildAccentScheme(
    base: ColorScheme,
    accent: Color,
    isDay: Boolean = false,
): ColorScheme {
    val (h, s, l) = toHsl(accent)

    val primary = if (isDay) {
        fromHsl(h, s.coerceAtLeast(0.55f), 0.42f)
    } else if (l < 0.45f) {
        fromHsl(h, s, 0.78f)
    } else {
        fromHsl(h, s, (l + 0.15f).coerceAtMost(0.88f))
    }
    val onPrimary = if (isDay) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)

    val primaryContainer = if (isDay) {
        fromHsl(h, (s * 0.60f).coerceAtLeast(0.30f), 0.86f)
    } else {
        fromHsl(h, (s * 0.85f).coerceAtLeast(0.35f), 0.22f)
    }
    val onPrimaryContainer = if (isDay) {
        fromHsl(h, (s * 0.80f).coerceAtLeast(0.35f), 0.22f)
    } else {
        fromHsl(h, (s * 0.35f).coerceAtLeast(0.12f), 0.90f)
    }

    val secondary = if (isDay) {
        fromHsl(h, (s * 0.50f).coerceAtLeast(0.22f), 0.48f)
    } else {
        fromHsl(h, (s * 0.55f).coerceAtLeast(0.25f), 0.82f)
    }
    val onSecondary = if (isDay) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)

    val secondaryContainer = if (isDay) {
        fromHsl(h, (s * 0.40f).coerceAtLeast(0.18f), 0.88f)
    } else {
        fromHsl(h, (s * 0.50f).coerceAtLeast(0.22f), 0.26f)
    }
    val onSecondaryContainer = if (isDay) {
        fromHsl(h, (s * 0.70f).coerceAtLeast(0.30f), 0.28f)
    } else {
        fromHsl(h, (s * 0.30f).coerceAtLeast(0.10f), 0.91f)
    }

    val tertiary = if (isDay) {
        fromHsl(h, (s * 0.40f).coerceAtLeast(0.18f), 0.52f)
    } else {
        fromHsl(h, (s * 0.45f).coerceAtLeast(0.20f), 0.85f)
    }
    val onTertiary = if (isDay) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)

    val tertiaryContainer = if (isDay) {
        fromHsl(h, (s * 0.35f).coerceAtLeast(0.15f), 0.90f)
    } else {
        fromHsl(h, (s * 0.40f).coerceAtLeast(0.18f), 0.30f)
    }
    val onTertiaryContainer = if (isDay) {
        fromHsl(h, (s * 0.65f).coerceAtLeast(0.28f), 0.30f)
    } else {
        fromHsl(h, (s * 0.28f).coerceAtLeast(0.09f), 0.92f)
    }

    val bgL = if (isDay) 0.96f else 0.095f
    val sclL = if (isDay) 0.93f else 0.12f
    val scL = if (isDay) 0.90f else 0.15f
    val schL = if (isDay) 0.87f else 0.18f

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = surfaceTint(h, s, bgL, 0.06f, isDay),
        onBackground = blend(base.onBackground, accent, if (isDay) 0.08f else 0.12f),
        onSurface = blend(base.onSurface, accent, if (isDay) 0.08f else 0.12f),
        onSurfaceVariant = blend(base.onSurfaceVariant, accent, if (isDay) 0.15f else 0.22f),
        outline = blend(base.outline, accent, if (isDay) 0.35f else 0.50f),
        outlineVariant = blend(base.outlineVariant, accent, if (isDay) 0.30f else 0.42f),
        surfaceContainerLow = surfaceTint(h, s, sclL, 0.08f, isDay),
        surfaceContainer = surfaceTint(h, s, scL, 0.08f, isDay),
        surfaceContainerHigh = surfaceTint(h, s, schL, 0.10f, isDay),
        primaryDim = primaryContainer,
        secondaryDim = secondaryContainer,
        tertiaryDim = tertiaryContainer,
    )
}

fun darkBaseScheme(): ColorScheme = darkNeutralBase

fun dayBaseScheme(): ColorScheme = dayNeutralBase
