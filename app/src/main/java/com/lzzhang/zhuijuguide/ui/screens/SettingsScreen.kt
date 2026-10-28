package com.lzzhang.zhuijuguide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.BuildConfig
import com.lzzhang.zhuijuguide.ui.GuideUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: GuideUiState,
    onAddFeed: (String) -> Unit,
    onRemoveFeed: (String) -> Unit,
) {
    var feedDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsSection(
                    title = "订阅源",
                    description = "粘贴 RSS / 播客的 feed 地址，APP 会读取其中的音视频并加入推荐和搜索。",
                ) {
                    OutlinedTextField(
                        value = feedDraft,
                        onValueChange = { feedDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Feed 地址") },
                        placeholder = { Text("https://example.com/feed.xml") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )
                    Button(
                        onClick = {
                            onAddFeed(feedDraft)
                            feedDraft = ""
                        },
                        enabled = !state.isAddingFeed && feedDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isAddingFeed) "正在读取订阅…" else "添加订阅")
                    }
                }
            }
            item {
                SettingsSection(
                    title = "已接入的数据源",
                    description = "每个数据源实现同一套接口，推荐、搜索、播放会自动聚合。",
                ) {
                    state.sources.forEach { source ->
                        val isUserFeed = source.id.startsWith("rss-")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = source.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = source.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isUserFeed) {
                                    IconButton(onClick = { onRemoveFeed(source.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "移除订阅",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "新增来源只需要实现 SourceProvider 接口并注册，不需要改页面代码。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                SettingsSection(
                    title = "使用原则",
                    icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                ) {
                    Text(
                        text = "本应用自带播放器，只播放数据源提供的地址。请只添加你有权访问的内容源，并遵守当地法律与服务条款。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                SettingsSection(
                    title = "关于",
                    icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                ) {
                    Text(
                        text = "追剧指南 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Kotlin · Jetpack Compose · Media3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "内置示例源：NASA 影像库（公共领域）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            if (icon != null) {
                Spacer(Modifier.padding(end = 4.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        content()
    }
}
