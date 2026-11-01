package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.ResourceCategory
import com.lzzhang.zhuijuguide.ui.GuideUiState
import com.lzzhang.zhuijuguide.ui.components.CategoryBar
import com.lzzhang.zhuijuguide.ui.components.TvResourceGrid

/**
 * 电视端首页。
 *
 * 与手机端的差异：
 * - 单列列表换成 3 列网格，一屏能看到 6 张以上
 * - 去掉 TopAppBar（电视不需要状态栏占位），标题直接排在内容里
 * - 分类条和卡片都带焦点描边
 */
@Composable
fun TvHomeScreen(
    state: GuideUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCheck: () -> Unit,
    onResourceClick: (MediaResource) -> Unit,
    onFavoriteClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "追剧指南",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (state.isChecking) {
                        "本机检测中 ${state.checkedCount}/${state.checkTotal}"
                    } else {
                        state.headerSummary
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            if (state.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                )
            } else {
                IconButton(onClick = onCheck) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "重新检测可用性")
                }
            }
        }

        if (state.isChecking) {
            LinearProgressIndicator(
                progress = {
                    if (state.checkTotal == 0) 0f else state.checkedCount.toFloat() / state.checkTotal
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
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

        CategoryBar(
            categories = state.categories,
            selected = state.category,
            onSelected = onCategoryChange,
            modifier = Modifier.padding(horizontal = 32.dp),
            isTv = true,
        )

        Spacer(Modifier.size(4.dp))

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.visibleResources.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.query.isBlank() &&
                        state.category == ResourceCategory.LOCAL &&
                        state.importedResources.isEmpty()
                    ) {
                        EmptySearch(
                            title = "还没有导入播放列表",
                            description = "在「设置 → 本地播放列表」里选择 M3U / M3U8 文件",
                        )
                    } else {
                        EmptySearch("没有匹配的资源", "换个关键词再试一次")
                    }
                }
            }

            else -> TvResourceGrid(
                resources = state.visibleResources,
                favorites = state.favorites,
                onResourceClick = onResourceClick,
                onFavoriteClick = onFavoriteClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
