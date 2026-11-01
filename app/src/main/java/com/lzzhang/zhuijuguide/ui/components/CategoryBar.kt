package com.lzzhang.zhuijuguide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 分类筛选条。
 *
 * 电视端额外给每个 chip 加了焦点描边：FilterChip 自带的指示器在 3 米外几乎看不见，
 * 而这一行是遥控器上下移动时的必经节点，必须能看清光标落在哪个分类上。
 */
@Composable
fun CategoryBar(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isTv: Boolean = false,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selected, isTv) {
        if (isTv) {
            val index = categories.indexOf(selected)
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it }) { category ->
            var focused by remember { mutableStateOf(false) }

            FilterChip(
                selected = category == selected,
                onClick = { onSelected(category) },
                label = { Text(category) },
                modifier = Modifier
                    .onFocusChanged { focused = it.isFocused }
                    .then(
                        if (isTv && focused) {
                            Modifier.border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                                CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}
