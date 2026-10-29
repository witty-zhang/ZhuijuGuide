package com.lzzhang.zhuijuguide.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lzzhang.zhuijuguide.BuildConfig
import com.lzzhang.zhuijuguide.ui.GuideUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Reader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: GuideUiState,
    onSaveFeedUrl: (String) -> Unit,
    onRefresh: () -> Unit,
    onImportPlaylist: (String) -> Unit,
    onPostMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedDraft by remember { mutableStateOf(state.feedUrl) }
    var isReadingPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(state.feedUrl) {
        feedDraft = state.feedUrl
    }

    val playlistPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isReadingPlaylist = true
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { readBounded(it, MAX_PLAYLIST_CHARS) }
                        ?: error("无法读取所选文件")
                }
            }.onSuccess { content ->
                onImportPlaylist(content)
            }.onFailure { error ->
                onPostMessage(error.message ?: "播放列表读取失败")
            }
            isReadingPlaylist = false
        }
    }

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
                    title = "远程资源",
                    description = "配置兼容 JSON 地址后，可在首页刷新资源目录。",
                    icon = { Icon(Icons.Rounded.CloudSync, contentDescription = null) },
                ) {
                    OutlinedTextField(
                        value = feedDraft,
                        onValueChange = { feedDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("JSON 数据源地址") },
                        placeholder = { Text("https://example.com/resources.json") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onSaveFeedUrl(feedDraft) },
                            enabled = feedDraft.isNotBlank() && !state.isRefreshing,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null)
                            Text("保存地址", modifier = Modifier.padding(start = 8.dp))
                        }
                        FilledTonalButton(
                            onClick = onRefresh,
                            enabled = !state.isRefreshing,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("立即刷新")
                        }
                    }
                }
            }
            item {
                SettingsSection(
                    title = "本地播放列表",
                    description = "导入你自行提供的 M3U 文件，频道只保存在当前设备。",
                    icon = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
                ) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                playlistPicker.launch(arrayOf("*/*"))
                            }.onFailure {
                                onPostMessage("无法打开文件选择器")
                            }
                        },
                        enabled = !isReadingPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Text(
                            text = if (isReadingPlaylist) "正在读取…" else "选择 M3U 文件",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        text = "已导入 ${state.importedResources.size} 个频道",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                SettingsSection(
                    title = "自动刷新",
                    description = "配置远程 JSON 后，App 每次启动会尝试刷新，并在后台每 12 小时更新一次。",
                ) {
                    Text(
                        text = if (state.feedUrl.isBlank()) {
                            "尚未配置远程资源，自动刷新暂不执行。"
                        } else {
                            "已启用：${state.feedUrl}"
                        },
                        style = MaterialTheme.typography.bodySmall,
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
                        text = "资源目录方向参考 Awesome Zhuiju Free（CC BY 4.0）",
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

private fun readBounded(reader: Reader, maxChars: Int): String {
    val buffer = CharArray(8 * 1024)
    val output = StringBuilder()
    while (true) {
        val count = reader.read(buffer)
        if (count < 0) break
        check(output.length + count <= maxChars) { "播放列表文件超过 2 MB 限制" }
        output.append(buffer, 0, count)
    }
    return output.toString()
}

private const val MAX_PLAYLIST_CHARS = 2_000_000
