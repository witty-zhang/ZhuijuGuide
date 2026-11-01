package com.lzzhang.zhuijuguide.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * 当前是否运行在电视形态的设备上。
 *
 * 由 MainActivity 在根部提供，界面层用 [isTv] 决定布局密度与焦点策略。
 */
val LocalIsTv = staticCompositionLocalOf { false }

/**
 * 判断设备形态。
 *
 * 优先看 uiMode：Android TV / Google TV 都会把 UI_MODE_TYPE 置为 TELEVISION，
 * 这是最可靠的信号。少数电视盒子不设这个值，再用 leanback 特性且无触摸屏兜底。
 */
fun Context.isTelevision(): Boolean {
    val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true

    val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val hasTouch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    return hasLeanback && !hasTouch
}

/**
 * 组合函数里读取设备形态。配置变化（例如外接显示器切换）时会重新计算。
 */
@Composable
fun rememberIsTv(): Boolean {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    return remember(configuration.uiMode, configuration.screenWidthDp, context) {
        context.isTelevision()
    }
}
