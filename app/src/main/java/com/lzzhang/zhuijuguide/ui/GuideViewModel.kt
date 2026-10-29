package com.lzzhang.zhuijuguide.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lzzhang.zhuijuguide.data.GuidePreferences
import com.lzzhang.zhuijuguide.data.ResourceRepository
import com.lzzhang.zhuijuguide.model.MediaResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuideUiState(
    val resources: List<MediaResource> = emptyList(),
    val importedResources: List<MediaResource> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val query: String = "",
    val category: String = ALL_CATEGORIES,
    val feedUrl: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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
            return allResources.filter { resource ->
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
        }

    val favoriteResources: List<MediaResource>
        get() = allResources.filter { it.id in favorites }

    companion object {
        const val ALL_CATEGORIES = "全部"
        const val MY_LIVE_CATEGORY = "我的直播"
    }
}

class GuideViewModel(
    private val repository: ResourceRepository,
    private val preferences: GuidePreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GuideUiState(
            favorites = preferences.favorites(),
            feedUrl = preferences.feedUrl,
        ),
    )
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val resources = repository.loadResources()
            val imported = repository.loadImportedPlaylist()
            _uiState.update {
                it.copy(
                    resources = resources,
                    importedResources = imported,
                    isLoading = false,
                )
            }
            refreshQuietly()
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setCategory(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun saveFeedUrl(value: String) {
        preferences.feedUrl = value
        _uiState.update { it.copy(feedUrl = value.trim(), message = "数据源地址已保存") }
    }

    fun refresh() {
        val url = _uiState.value.feedUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(message = "请先在设置中填写远程 JSON 数据源") }
            return
        }
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching { repository.refreshResources(url) }
                .onSuccess { resources ->
                    _uiState.update {
                        it.copy(
                            resources = resources,
                            isRefreshing = false,
                            message = "已更新 ${resources.size} 个资源",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = error.message ?: "资源更新失败",
                        )
                    }
                }
        }
    }

    private fun refreshQuietly() {
        val url = _uiState.value.feedUrl.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.refreshResources(url) }
                .onSuccess { resources ->
                    _uiState.update { it.copy(resources = resources) }
                }
        }
    }

    fun importPlaylist(content: String) {
        viewModelScope.launch {
            runCatching { repository.importPlaylist(content) }
                .onSuccess { imported ->
                    _uiState.update {
                        it.copy(
                            importedResources = imported,
                            category = GuideUiState.MY_LIVE_CATEGORY,
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
            ) as T
        }
    }
}
