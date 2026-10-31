package com.juhao.rollcall.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec

enum class ScreenShape { ROUND, SQUARE }

val LocalScreenShape = staticCompositionLocalOf { ScreenShape.SQUARE }

@Composable
fun rememberScreenShape(): ScreenShape {
    val configuration = LocalConfiguration.current
    return if ((configuration.screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK) ==
        Configuration.SCREENLAYOUT_ROUND_YES
    ) {
        ScreenShape.ROUND
    } else {
        ScreenShape.SQUARE
    }
}

/**
 * 圆屏用默认曲线，方屏用“小屏”变体：方屏上下留白少，默认曲线会把卡片挤得太小。
 */
@Composable
fun rememberAdaptiveTransformationSpec(square: Boolean): TransformationSpec {
    return if (square) {
        rememberTransformationSpec(
            ResponsiveTransformationSpec.smallScreen(
                minTransitionAreaHeightFraction = 0f,
                maxTransitionAreaHeightFraction = 0f,
            )
        )
    } else {
        rememberTransformationSpec()
    }
}

@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(enabled, activity) {
        val window = activity?.window
        if (window != null && enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
