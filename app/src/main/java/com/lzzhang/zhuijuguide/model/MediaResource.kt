package com.lzzhang.zhuijuguide.model

enum class ResourceType {
    WEB,
    APP,
    PLAYLIST,
    VIDEO,
    OPEN_SOURCE,
}

enum class Availability {
    AVAILABLE,
    RESTRICTED,
    OFFLINE,
    UNKNOWN,
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN,
}

data class MediaResource(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val category: String,
    val type: ResourceType,
    val availability: Availability,
    val riskLevel: RiskLevel,
    val rating: Float?,
    val lastChecked: String?,
    val tags: List<String> = emptyList(),
    val sourceLabel: String = "内置资源",
    val isImported: Boolean = false,
) {
    val isDirectMedia: Boolean
        get() = type == ResourceType.VIDEO

    val availabilityLabel: String
        get() = when (availability) {
            Availability.AVAILABLE -> "可用"
            Availability.RESTRICTED -> "受限"
            Availability.OFFLINE -> "失效"
            Availability.UNKNOWN -> "待检测"
        }
}

data class PlaylistEntry(
    val title: String,
    val url: String,
    val group: String? = null,
    val logoUrl: String? = null,
)

/** 首页固定的四个分类：全部、内置在线影视目录、直播源订阅、本地导入的 M3U / M3U8。 */
object ResourceCategory {
    const val ALL = "全部"
    const val ONLINE = "在线影视"
    const val LIVE = "直播源"
    const val LOCAL = "M3U / M3U8"

    val tabs = listOf(ALL, ONLINE, LIVE, LOCAL)
}
