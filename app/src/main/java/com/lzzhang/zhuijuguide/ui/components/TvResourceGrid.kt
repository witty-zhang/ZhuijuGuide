package com.lzzhang.zhuijuguide.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.model.MediaResource

/**
 * 电视端资源网格。
 *
 * 列数按屏幕宽度决定：1080p 电视横向放 3 张卡，分辨率更高时自动变 4 张。
 */
@Composable
fun TvResourceGrid(
    resources: List<MediaResource>,
    favorites: Set<String>,
    onResourceClick: (MediaResource) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val firstId = resources.firstOrNull()?.id
    val firstCard = rememberTvInitialFocus(firstId)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(resources, key = { it.id }) { resource ->
            TvResourceCard(
                resource = resource,
                isFavorite = resource.id in favorites,
                onClick = { onResourceClick(resource) },
                onFavoriteClick = { onFavoriteClick(resource.id) },
                focusRequester = if (resource.id == firstId) firstCard else null,
            )
        }
    }
}
