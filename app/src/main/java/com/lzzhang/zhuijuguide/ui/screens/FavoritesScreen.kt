package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.ui.GuideUiState
import com.lzzhang.zhuijuguide.ui.LocalIsTv
import com.lzzhang.zhuijuguide.ui.components.ResourceListItem
import com.lzzhang.zhuijuguide.ui.components.TvResourceGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: GuideUiState,
    onResourceClick: (MediaResource) -> Unit,
    onFavoriteClick: (String) -> Unit,
) {
    val isTv = LocalIsTv.current

    if (isTv) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "收藏",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 32.dp, top = 28.dp, bottom = 4.dp),
            )
            if (state.favoriteResources.isEmpty()) {
                EmptyFavorites(modifier = Modifier.fillMaxSize())
            } else {
                TvResourceGrid(
                    resources = state.favoriteResources,
                    favorites = state.favorites,
                    onResourceClick = onResourceClick,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("收藏") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.favoriteResources.isEmpty()) {
                item { EmptyFavorites() }
            } else {
                items(state.favoriteResources, key = { it.id }) { resource ->
                    ResourceListItem(
                        resource = resource,
                        isFavorite = true,
                        onClick = { onResourceClick(resource) },
                        onFavoriteClick = { onFavoriteClick(resource.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        Text("还没有收藏", style = MaterialTheme.typography.titleMedium)
        Text(
            "在资源列表点击心形按钮即可收藏",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
