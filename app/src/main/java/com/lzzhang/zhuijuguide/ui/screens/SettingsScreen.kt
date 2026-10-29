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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onCheck: () -> Unit,
    onImportPlaylist: (String) -> Unit,
    onPostMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isReadingPlaylist by remember { mutableStateOf(false) }

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
                    title = "可用性检测",
                    description = "打开 APP 不会自动检测；点首页右上角刷新，或下面的按钮才发起。200–399 判为可用，401/403/429/451 判为受限，其余判为失效。",
                    icon = { Icon(Icons.Rounded.NetworkCheck, contentDescription = null) },
                ) {
                    Text(
                        text = "上次检测：${state.lastCheckedText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.totalStatusSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onCheck,
                        enabled = !state.isChecking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isChecking) {
                                "检测中 ${state.checkedCount}/${state.checkTotal}"
                            } else {
                                "立即检测"
                            },
                        )
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                SettingsSection(
                    title = "本地播放列表",
                    description = "导入自行提供的 M3U / M3U8 频道列表；频道只保存在当前设备。",
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
                            text = if (isReadingPlaylist) "正在读取…" else "选择 M3U / M3U8 文件",
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
