package com.lzzhang.zhuijuguide.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lzzhang.zhuijuguide.data.AvailabilityChecker
import com.lzzhang.zhuijuguide.data.AvailabilityRecord
import com.lzzhang.zhuijuguide.data.AvailabilityStore
import com.lzzhang.zhuijuguide.data.GuidePreferences
import com.lzzhang.zhuijuguide.data.ResourceRepository
import com.lzzhang.zhuijuguide.model.Availability
import com.lzzhang.zhuijuguide.model.MediaResource
import com.lzzhang.zhuijuguide.model.ResourceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class GuideUiState(
    val resources: List<MediaResource> = emptyList(),
    val liveSources: List<MediaResource> = emptyList(),
    val importedResources: List<MediaResource> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val category: String = ResourceCategory.ALL,
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val checkedCount: Int = 0,
    val checkTotal: Int = 0,
    val lastCheckedAt: Long? = null,
    val message: String? = null,
    val liveSource: MediaResource? = null,
    val liveChannels: List<MediaResource> = emptyList(),
    val liveQuery: String = "",
    val isLiveLoading: Boolean = false,
    val liveError: String? = null,
) {
    /** 固定四个分类，不再按数据里的分组自动长出新的 tab。 */
    val categories: List<String> = ResourceCategory.tabs

    val allResources: List<MediaResource>
        get() = (importedResources + resources + liveSources).distinctBy { it.id }

    val currentResources: List<MediaResource>
        get() = when (category) {
            ResourceCategory.LOCAL -> importedResources
            ResourceCategory.LIVE -> liveSources
            ResourceCategory.ONLINE -> resources
            else -> allResources
        }

    val visibleResources: List<MediaResource>
        get() {
            val needle = query.trim()
            return currentResources
                .filter { resource ->
                    needle.isBlank() ||
                        resource.title.contains(needle, ignoreCase = true) ||
                        resource.description.contains(needle, ignoreCase = true) ||
                        resource.category.contains(needle, ignoreCase = true) ||
                        resource.tags.any { it.contains(needle, ignoreCase = true) }
                }
                // 可用的排在前面，失效的沉底；同一状态内保持目录原顺序
                .sortedBy { it.availability.sortRank }
        }

    /** 直播源里搜出来的频道，量级上千，按名称和分组过滤。 */
    val visibleLiveChannels: List<MediaResource>
        get() {
            val needle = liveQuery.trim()
            if (needle.isBlank()) return liveChannels
            return liveChannels.filter { channel ->
                channel.title.contains(needle, ignoreCase = true) ||
                    channel.tags.any { it.contains(needle, ignoreCase = true) }
            }
        }

    val favoriteResources: List<MediaResource>
        get() = allResources.filter { it.id in favorites }

    /** 当前 tab 的统计，首页标题栏用。 */
    val statusSummary: String
        get() = currentResources.summary()

    /** 各个 tab 加起来的统计，设置页用。 */
    val totalStatusSummary: String
        get() = allResources.summary()

    val headerSummary: String
        get() = when (category) {
            ResourceCategory.LOCAL -> "已导入 ${importedResources.size} 个频道"
            ResourceCategory.LIVE -> "${liveSources.size} 个直播源 · $statusSummary"
            ResourceCategory.ONLINE -> "${resources.size} 个资源 · $statusSummary"
            else -> "${allResources.size} 个资源 · $statusSummary"
        }

    val lastCheckedText: String
        get() = lastCheckedAt?.let(::formatTimestamp) ?: "尚未检测"
}

class GuideViewModel(
    private val repository: ResourceRepository,
    private val preferences: GuidePreferences,
    private val availabilityStore: AvailabilityStore,
    private val checker: AvailabilityChecker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuideUiState(favorites = preferences.favorites()))
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val records = availabilityStore.read()
            val resources = repository.loadResources().applyRecords(records)
            val imported = repository.loadImportedPlaylist().applyRecords(records)
            val liveSources = repository.loadLiveSources().applyRecords(records)
            _uiState.update {
                it.copy(
                    resources = resources,
                    importedResources = imported,
                    liveSources = liveSources,
                    isLoading = false,
                    lastCheckedAt = records.values.maxOfOrNull(AvailabilityRecord::checkedAt),
                )
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setCategory(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    /**
     * 逐条在本机发起请求，检测结果一边返回一边显示；
     * 全部完成后把结果写进本地缓存，下次启动先显示旧结果再重新检测。
     */
    fun checkAvailability(announce: Boolean = false) {
        if (_uiState.value.isChecking) return
        val targets = _uiState.value.allResources
        if (targets.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isChecking = true, checkedCount = 0, checkTotal = targets.size)
            }
            val fresh = ConcurrentHashMap<String, AvailabilityRecord>()
            checker.checkAll(targets) { id, availability ->
                if (availability != Availability.UNKNOWN) {
                    fresh[id] = AvailabilityStore.toRecord(availability, System.currentTimeMillis())
                }
                _uiState.update { state ->
                    state.copy(
                        resources = state.resources.applyAvailability(id, availability),
                        importedResources = state.importedResources.applyAvailability(id, availability),
                        liveSources = state.liveSources.applyAvailability(id, availability),
                        checkedCount = state.checkedCount + 1,
                    )
                }
            }

            val known = _uiState.value.allResources.mapTo(HashSet()) { it.id }
            availabilityStore.write(
                availabilityStore.read().filterKeys { it in known } + fresh,
            )
            _uiState.update {
                it.copy(
                    isChecking = false,
                    lastCheckedAt = System.currentTimeMillis(),
                    message = if (announce) "检测完成：${it.statusSummary}" else it.message,
                )
            }
        }
    }

    fun importPlaylist(content: String) {
        viewModelScope.launch {
            runCatching { repository.importPlaylist(content) }
                .onSuccess { imported ->
                    val withStatus = imported.applyRecords(availabilityStore.read())
                    _uiState.update {
                        it.copy(
                            importedResources = withStatus,
                            category = ResourceCategory.LOCAL,
                            message = "已导入 ${imported.size} 个频道",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.message ?: "播放列表导入失败")
                    }
                }
        }
    }

    /**
     * 打开一个直播源：把清单下载下来展开成频道。频道只放在内存里，
     * 不重复检测，也不写进收藏。
     */
    fun openLiveSource(source: MediaResource) {
        if (_uiState.value.isLiveLoading) return
        _uiState.update {
            it.copy(
                liveSource = source,
                liveChannels = emptyList(),
                liveQuery = "",
                isLiveLoading = true,
                liveError = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.fetchLiveChannels(source) }
                .onSuccess { channels ->
                    _uiState.update {
                        it.copy(liveChannels = channels, isLiveLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLiveLoading = false,
                            liveError = error.message ?: "直播源加载失败",
                        )
                    }
                }
        }
    }

    fun reloadLiveSource() {
        _uiState.value.liveSource?.let(::openLiveSource)
    }

    fun setLiveQuery(value: String) {
        _uiState.update { it.copy(liveQuery = value) }
    }

    fun toggleFavorite(resourceId: String) {
        val updated = _uiState.value.favorites.toMutableSet().apply {
            if (!add(resourceId)) remove(resourceId)
        }
        preferences.saveFavorites(updated)
        _uiState.update { it.copy(favorites = updated) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun postMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(GuideViewModel::class.java))
            return GuideViewModel(
                repository = ResourceRepository(appContext),
                preferences = GuidePreferences(appContext),
                availabilityStore = AvailabilityStore(appContext),
                checker = AvailabilityChecker(appContext),
            ) as T
        }
    }
}

private fun List<MediaResource>.applyRecords(
    records: Map<String, AvailabilityRecord>,
): List<MediaResource> = map { resource ->
    val record = records[resource.id] ?: return@map resource
    resource.copy(
        availability = record.availability(),
        lastChecked = formatTimestamp(record.checkedAt),
    )
}

private fun List<MediaResource>.applyAvailability(
    id: String,
    availability: Availability,
): List<MediaResource> = map { resource ->
    if (resource.id == id) {
        resource.copy(
            availability = availability,
            lastChecked = formatTimestamp(System.currentTimeMillis()),
        )
    } else {
        resource
    }
}

private fun List<MediaResource>.summary(): String {
    fun statusCount(availability: Availability) = count { it.availability == availability }
    return "可用 ${statusCount(Availability.AVAILABLE)} · " +
        "受限 ${statusCount(Availability.RESTRICTED)} · " +
        "失效 ${statusCount(Availability.OFFLINE)} · " +
        "待检测 ${statusCount(Availability.UNKNOWN)}"
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
}

private val Availability.sortRank: Int
    get() = when (this) {
        Availability.AVAILABLE -> 0
        Availability.RESTRICTED -> 1
        Availability.UNKNOWN -> 2
        Availability.OFFLINE -> 3
    }
