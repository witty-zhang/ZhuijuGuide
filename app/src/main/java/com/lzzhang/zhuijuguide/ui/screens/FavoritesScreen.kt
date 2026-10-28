package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.lzzhang.zhuijuguide.model.WorkSummary
import com.lzzhang.zhuijuguide.ui.GuideUiState
import com.lzzhang.zhuijuguide.ui.components.PosterCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: GuideUiState,
    onWorkClick: (WorkSummary) -> Unit,
    onFavoriteClick: (WorkSummary) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("收藏") }) },
    ) { innerPadding ->
        if (state.favorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                    "在海报上点心形按钮即可收藏",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.favorites, key = WorkSummary::id) { work ->
                    PosterCard(
                        work = work,
                        isFavorite = true,
                        onClick = { onWorkClick(work) },
                        onFavoriteClick = { onFavoriteClick(work) },
                    )
                }
            }
        }
    }
}
