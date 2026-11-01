package com.lzzhang.zhuijuguide.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 把初始焦点交给列表第一项。
 *
 * 电视上没有指针，若进入页面后焦点悬空，用户按方向键不会有任何反应，
 * 看起来就像遥控器失灵。这里用逐帧重试的方式等待第一项完成布局。
 */
@Composable
fun rememberTvInitialFocus(key: Any?): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(key) {
        if (key == null) return@LaunchedEffect
        repeat(6) {
            withFrameNanos { }
            if (runCatching { requester.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    return requester
}

/**
 * 电视端可聚焦卡片。
 *
 * 遥控器没有指针，「焦点在哪」全靠视觉表达，所以这里做了三件事：
 * 1. 获得焦点时描一圈 2dp 主色边框
 * 2. 轻微放大 5%，制造「被选中」的空间感
 * 3. 底色切换到 primaryContainer，远距离也能分辨
 *
 * 手机端不会用到这个组件，[TvResourceCard] 等只在 isTv 分支里调用。
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        label = "tv-focus-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        label = "tv-focus-color",
    )
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .border(BorderStroke(2.dp, borderColor), shape),
        shape = shape,
        color = containerColor,
    ) {
        Column {
            content()
        }
    }
}
