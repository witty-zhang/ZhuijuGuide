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
    val importedResources: List<MediaResource> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val category: String = ALL_CATEGORIES,
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val checkedCount: Int = 0,
    val checkTotal: Int = 0,
    val lastCheckedAt: Long? = null,
    val message: String? = null,
) {
    val allResources: List<MediaResource>
        get() = (importedResources + resources).distinctBy { it.id }

    val categories: List<String>
        get() = buildList {
            add(ALL_CATEGORIES)
            addAll(allResources.map(MediaResource::category).distinct().sorted())
            if (MY_LIVE_CATEGORY !in this) add(MY_LIVE_CATEGORY)
        }

    val visibleResources: List<MediaResource>
        get() {
            val needle = query.trim()
            return allResources
                .filter { resource ->
                    val matchesCategory = category == ALL_CATEGORIES ||
                        resource.category == category ||
                        (category == MY_LIVE_CATEGORY && resource.isImported)
                    val matchesSearch = needle.isBlank() ||
                        resource.title.contains(needle, ignoreCase = true) ||
                        resource.description.contains(needle, ignoreCase = true) ||
                        resource.category.contains(needle, ignoreCase = true) ||
                        resource.tags.any { it.contains(needle, ignoreCase = true) }
                    matchesCategory && matchesSearch
                }
                // 可用的排在前面，失效的沉底；同一状态内保持目录原顺序
                .sortedBy { it.availability.sortRank }
        }

    val favoriteResources: List<MediaResource>
        get() = allResources.filter { it.id in favorites }

    fun countOf(availability: Availability): Int =
        allResources.count { it.availability == availability }

    val statusSummary: String
        get() = "可用 ${countOf(Availability.AVAILABLE)} · " +
            "受限 ${countOf(Availability.RESTRICTED)} · " +
            "失效 ${countOf(Availability.OFFLINE)} · " +
            "待检测 ${countOf(Availability.UNKNOWN)}"

    val lastCheckedText: String
        get() = lastCheckedAt?.let(::formatTimestamp) ?: "尚未检测"

    companion object {
        const val ALL_CATEGORIES = "全部"
        const val MY_LIVE_CATEGORY = "我的直播"
    }
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
            _uiState.update {
                it.copy(
                    resources = resources,
                    importedResources = imported,
                    isLoading = false,
                    lastCheckedAt = records.values.maxOfOrNull(AvailabilityRecord::checkedAt),
                )
            }
            checkAvailability()
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
                            category = GuideUiState.MY_LIVE_CATEGORY,
                            message = "已导入 ${imported.size} 个频道",
                        )
                    }
                    checkAvailability()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(message = error.message ?: "播放列表导入失败")
                    }
                }
        }
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
