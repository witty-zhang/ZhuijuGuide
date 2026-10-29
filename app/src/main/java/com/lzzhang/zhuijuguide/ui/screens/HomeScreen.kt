package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lzzhang.zhuijuguide.ui.GuideUiState
import com.lzzhang.zhuijuguide.ui.components.CategoryBar
import com.lzzhang.zhuijuguide.ui.components.ResourceListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: GuideUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCheck: () -> Unit,
    onResourceClick: (com.lzzhang.zhuijuguide.model.MediaResource) -> Unit,
    onFavoriteClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("追剧指南")
                        Text(
                            text = if (state.isChecking) {
                                "本机检测中 ${state.checkedCount}/${state.checkTotal}"
                            } else {
                                "${state.allResources.size} 个资源 · ${state.statusSummary}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (state.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onCheck) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "重新检测可用性")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.isChecking) {
                item {
                    LinearProgressIndicator(
                        progress = {
                            if (state.checkTotal == 0) {
                                0f
                            } else {
                                state.checkedCount.toFloat() / state.checkTotal
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    placeholder = { Text("搜索资源、分类或标签") },
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

            item {
                CategoryBar(
                    categories = state.categories,
                    selected = state.category,
                    onSelected = onCategoryChange,
                )
            }

            when {
                state.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.visibleResources.isEmpty() -> {
                    item {
                        EmptySearch("没有匹配的资源", "换个关键词或分类再试一次")
                    }
                }

                else -> items(state.visibleResources, key = { it.id }) { resource ->
                    ResourceListItem(
                        resource = resource,
                        isFavorite = resource.id in state.favorites,
                        onClick = { onResourceClick(resource) },
                        onFavoriteClick = { onFavoriteClick(resource.id) },
                    )
                }
            }
        }
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
