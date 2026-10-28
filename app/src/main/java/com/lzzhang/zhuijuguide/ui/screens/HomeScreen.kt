package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.model.WorkSummary
import com.lzzhang.zhuijuguide.ui.GuideUiState
import com.lzzhang.zhuijuguide.ui.components.PosterCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: GuideUiState,
    onQueryChange: (String) -> Unit,
    onReload: () -> Unit,
    onWorkClick: (WorkSummary) -> Unit,
    onFavoriteClick: (WorkSummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.isSearchMode) "搜索" else "推荐")
                        Text(
                            text = if (state.isSearchMode) {
                                state.query.trim()
                            } else {
                                "来自 ${state.sources.size} 个数据源"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .padding(3.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (!state.isSearchMode) {
                        IconButton(onClick = onReload) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "重新加载推荐")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    placeholder = { Text("搜索影片，例如 moon、launch") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            }

            if (state.isSearching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            when {
                state.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    GridStatus("正在加载推荐…", showSpinner = true)
                }

                state.isSearchMode && !state.isSearching && state.results.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptySearch(
                            title = "没有找到相关内容",
                            description = "换个关键词试试，例如 apollo、artemis",
                        )
                    }
                }

                !state.isSearchMode && state.recommendations.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptySearch(
                            title = "推荐加载失败",
                            description = "检查网络后点右上角重新加载",
                        )
                    }
                }

                else -> items(state.visibleWorks, key = WorkSummary::id) { work ->
                    PosterCard(
                        work = work,
                        isFavorite = work.id in state.favoriteIds,
                        onClick = { onWorkClick(work) },
                        onFavoriteClick = { onFavoriteClick(work) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GridStatus(text: String, showSpinner: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSpinner) CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptySearch(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
