package com.lzzhang.zhuijuguide.data

import android.content.Context
import com.lzzhang.zhuijuguide.model.Availability
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** 把上一次检测结果存在本地，下次打开 APP 先显示旧结果，再在后台重新检测。 */
class AvailabilityStore(context: Context) {
    private val file = File(context.filesDir, "availability_cache.json")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun read(): Map<String, AvailabilityRecord> {
        if (!file.isFile) return emptyMap()
        return runCatching {
            json.decodeFromString(AvailabilityCache.serializer(), file.readText()).records
        }.getOrDefault(emptyMap())
    }

    fun write(records: Map<String, AvailabilityRecord>) {
        runCatching {
            val payload = AvailabilityCache(records)
            file.writeText(json.encodeToString(AvailabilityCache.serializer(), payload))
        }
    }

    companion object {
        fun toRecord(availability: Availability, checkedAt: Long) = AvailabilityRecord(
            status = availability.name,
            checkedAt = checkedAt,
        )
    }
}

@Serializable
data class AvailabilityRecord(
    val status: String,
    val checkedAt: Long,
) {
    fun availability(): Availability =
        runCatching { Availability.valueOf(status) }.getOrDefault(Availability.UNKNOWN)
}

@Serializable
private data class AvailabilityCache(
    val records: Map<String, AvailabilityRecord> = emptyMap(),
)
