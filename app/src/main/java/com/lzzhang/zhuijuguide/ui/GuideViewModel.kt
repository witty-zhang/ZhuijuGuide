package com.lzzhang.zhuijuguide.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lzzhang.zhuijuguide.data.GuidePreferences
import com.lzzhang.zhuijuguide.data.sources.NasaVideoSource
import com.lzzhang.zhuijuguide.data.sources.RssFeedSource
import com.lzzhang.zhuijuguide.data.sources.SourceProvider
import com.lzzhang.zhuijuguide.data.sources.SourceRegistry
import com.lzzhang.zhuijuguide.model.Episode
import com.lzzhang.zhuijuguide.model.WorkSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourceInfo(
    val id: String,
    val name: String,
    val tagline: String,
)

data class GuideUiState(
    val recommendations: List<WorkSummary> = emptyList(),
    val results: List<WorkSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val favorites: List<WorkSummary> = emptyList(),
    val selectedWork: WorkSummary? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val sources: List<SourceInfo> = emptyList(),
    val isAddingFeed: Boolean = false,
    val message: String? = null,
) {
    val isSearchMode: Boolean get() = query.isNotBlank()

    val visibleWorks: List<WorkSummary>
        get() = if (isSearchMode) results else recommendations

    val favoriteIds: Set<String> get() = favorites.map(WorkSummary::id).toSet()
}

class GuideViewModel(
    private val preferences: GuidePreferences,
    private val baseProviders: List<SourceProvider> = listOf(NasaVideoSource()),
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private var registry: SourceRegistry = SourceRegistry(baseProviders)

    private var searchJob: Job? = null

    init {
        val favorites = preferences.favoriteWorks()
        _uiState.update { it.copy(favorites = favorites) }
        rebuildRegistry()
        registry.remember(favorites)
        loadRecommendations()
    }

    private fun rebuildRegistry() {
        registry = SourceRegistry(baseProviders + preferences.feedUrls().map { RssFeedSource(it) })
        refreshSourceInfo()
    }

    private fun refreshSourceInfo() {
        _uiState.update { state ->
            state.copy(
                sources = registry.providers.map { provider ->
                    SourceInfo(provider.id, provider.name, provider.tagline)
                },
            )
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            val featured = coroutineScope {
                registry.providers.map { provider ->
                    async { runCatching { provider.featured() }.getOrDefault(emptyList()) }
                }.awaitAll()
            }.flatten()
            registry.remember(featured)
            _uiState.update {
                it.copy(
                    recommendations = featured,
                    isLoading = false,
                    message = if (featured.isEmpty()) "推荐内容加载失败，请检查网络后重试" else null,
                )
            }
            // 订阅源的名字要等抓取完成后才知道
            refreshSourceInfo()
        }
    }

    fun reloadRecommendations() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        loadRecommendations()
    }

    /** 添加 RSS / 播客订阅：先抓一次确认能读到内容，再保存。 */
    fun addFeed(rawUrl: String) {
        val url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            postMessage("请填写以 http:// 或 https:// 开头的订阅地址")
            return
        }
        if (_uiState.value.isAddingFeed) return
        if (preferences.feedUrls().any { it.equals(url, ignoreCase = true) }) {
            postMessage("这个订阅已经添加过了")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingFeed = true) }
            val provider = RssFeedSource(url)
            runCatching { provider.featured() }
                .onSuccess { works ->
                    if (works.isEmpty()) {
                        _uiState.update { it.copy(isAddingFeed = false) }
                        postMessage("这个地址里没有可播放的音频或视频，检查一下是不是音频/视频类订阅")
                    } else {
                        preferences.saveFeedUrls(preferences.feedUrls() + url)
                        rebuildRegistry()
                        _uiState.update { it.copy(isAddingFeed = false, isLoading = true) }
                        loadRecommendations()
                        postMessage("已添加订阅：${works.size} 个节目")
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingFeed = false) }
                    postMessage(error.message ?: "订阅地址读取失败")
                }
        }
    }

    fun removeFeed(sourceId: String) {
        val urls = preferences.feedUrls()
        val target = urls.firstOrNull { RssFeedSource(it).id == sourceId } ?: return
        preferences.saveFeedUrls(urls - target)
        rebuildRegistry()
        _uiState.update { it.copy(isLoading = true) }
        loadRecommendations()
        postMessage("已移除订阅")
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        val needle = value.trim()
        if (needle.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false, searchError = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            val outcome = runCatching {
                coroutineScope {
                    registry.providers.map { provider ->
                        async { runCatching { provider.search(needle) }.getOrDefault(emptyList()) }
                    }.awaitAll()
                }.flatten()
            }
            outcome.onSuccess { works ->
                registry.remember(works)
                _uiState.update { it.copy(results = works, isSearching = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isSearching = false, searchError = error.message ?: "搜索失败")
                }
            }
        }
    }

    fun openWork(work: WorkSummary) {
        _uiState.update {
            it.copy(selectedWork = work, episodes = emptyList(), isLoadingEpisodes = true)
        }
        viewModelScope.launch {
            val episodes = runCatching {
                registry.providers.firstOrNull { it.id == work.sourceId }?.episodes(work)
                    ?: emptyList()
            }.getOrDefault(emptyList())
            _uiState.update { state ->
                if (state.selectedWork?.id != work.id) return@update state
                state.copy(
                    episodes = episodes,
                    isLoadingEpisodes = false,
                    message = if (episodes.isEmpty()) "没有取到可播放的地址" else null,
                )
            }
        }
    }

    fun toggleFavorite(work: WorkSummary) {
        val current = _uiState.value.favorites
        val updated = if (current.any { it.id == work.id }) {
            current.filterNot { it.id == work.id }
        } else {
            listOf(work) + current
        }
        preferences.saveFavoriteWorks(updated)
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
                preferences = GuidePreferences(appContext),
            ) as T
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
