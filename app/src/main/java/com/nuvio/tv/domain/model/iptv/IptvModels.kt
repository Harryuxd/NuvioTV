package com.nuvio.tv.domain.model.iptv

import androidx.compose.runtime.Immutable

enum class IptvPlaylistType {
    M3U,
    XTREAM
}

enum class IptvStreamFormat {
    HLS,
    MPEGTS,
    MP4,
    UNKNOWN
}

enum class IptvEpgSourceKind { MANUAL, PLAYLIST }
enum class IptvEpgSyncStatus { NEVER_SYNCED, SUCCESS, FAILED }

@Immutable
data class IptvEpgSource(
    val id: String,
    val name: String,
    val location: String,
    val kind: IptvEpgSourceKind,
    val playlistId: String? = null,
    val lastRefreshedEpochMs: Long = 0L,
    val lastError: String? = null,
    val syncStatus: IptvEpgSyncStatus = IptvEpgSyncStatus.NEVER_SYNCED
)

@Immutable
data class IptvPlaylist(
    val id: String,
    val name: String,
    val type: IptvPlaylistType,
    val sourceLocation: String, // Remote URL or local file path
    val epgUrl: String? = null,
    val refreshIntervalHours: Int = 24,
    val lastRefreshedEpochMs: Long = 0L,
    val channelCount: Int = 0,
    val isEnabled: Boolean = true
)

@Immutable
data class IptvGroup(
    val id: String,
    val playlistId: String,
    val title: String,
    val orderIndex: Int = 0,
    val channelCount: Int = 0
)

@Immutable
data class IptvChannel(
    val id: String,
    val playlistId: String,
    val groupId: String,
    val groupTitle: String,
    val name: String,
    val streamUrl: String,
    /** Original artwork supplied by the playlist, kept for an eventual manual override. */
    val providerLogoUrl: String? = null,
    /** The quality-approved artwork displayed by Nuvio. */
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val channelNumber: Int? = null,
    val headers: Map<String, String> = emptyMap(),
    val streamFormat: IptvStreamFormat = IptvStreamFormat.UNKNOWN,
    val isFavorite: Boolean = false,
    val lastWatchedEpochMs: Long? = null
)

@Immutable
data class IptvEpgProgram(
    val id: String,
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val category: String? = null,
    val posterUrl: String? = null
) {
    val progress: Float
        get() {
            val now = System.currentTimeMillis()
            if (now < startEpochMs) return 0f
            if (now >= endEpochMs) return 1f
            val total = endEpochMs - startEpochMs
            if (total <= 0) return 0f
            return ((now - startEpochMs).toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    val isLiveNow: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startEpochMs until endEpochMs
        }
}

@Immutable
data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val expDate: String? = null,
    val isTrial: Boolean = false,
    val maxConnections: Int? = null
)
