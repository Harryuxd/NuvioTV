package com.nuvio.tv.data.local.iptv

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nuvio.tv.data.local.InternalPlayerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.iptvDataStore by preferencesDataStore(name = "iptv_preferences")

enum class IptvBufferProfile(val label: String, val minBufferMs: Int, val maxBufferMs: Int) {
    LOW_LATENCY("Low Latency (Fast Zapping)", 2000, 5000),
    STANDARD("Standard (15s)", 15000, 30000),
    HIGH_STABILITY("High Stability (30s)", 30000, 60000)
}

@Singleton
class IptvPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACTIVE_PLAYLIST_ID = stringPreferencesKey("active_playlist_id")
        private val LAST_SELECTED_GROUP_ID = stringPreferencesKey("last_selected_group_id")
        private val IPTV_PLAYER_ENGINE = stringPreferencesKey("iptv_player_engine")
        private val EPG_AUTO_SYNC_ENABLED = booleanPreferencesKey("epg_auto_sync_enabled")
        private val IPTV_BUFFER_PROFILE = stringPreferencesKey("iptv_buffer_profile")
    }

    val activePlaylistId: Flow<String?> = context.iptvDataStore.data.map { prefs ->
        prefs[ACTIVE_PLAYLIST_ID]
    }

    suspend fun setActivePlaylistId(playlistId: String) {
        context.iptvDataStore.edit { prefs ->
            prefs[ACTIVE_PLAYLIST_ID] = playlistId
        }
    }

    val lastSelectedGroupId: Flow<String?> = context.iptvDataStore.data.map { prefs ->
        prefs[LAST_SELECTED_GROUP_ID]
    }

    suspend fun setLastSelectedGroupId(groupId: String) {
        context.iptvDataStore.edit { prefs ->
            prefs[LAST_SELECTED_GROUP_ID] = groupId
        }
    }

    val iptvPlayerEngine: Flow<InternalPlayerEngine> = context.iptvDataStore.data.map { prefs ->
        prefs[IPTV_PLAYER_ENGINE]?.let {
            runCatching { InternalPlayerEngine.valueOf(it) }.getOrDefault(InternalPlayerEngine.EXOPLAYER)
        } ?: InternalPlayerEngine.EXOPLAYER
    }

    suspend fun setIptvPlayerEngine(engine: InternalPlayerEngine) {
        context.iptvDataStore.edit { prefs ->
            prefs[IPTV_PLAYER_ENGINE] = engine.name
        }
    }

    val epgAutoSyncEnabled: Flow<Boolean> = context.iptvDataStore.data.map { prefs ->
        prefs[EPG_AUTO_SYNC_ENABLED] ?: true
    }

    suspend fun setEpgAutoSyncEnabled(enabled: Boolean) {
        context.iptvDataStore.edit { prefs ->
            prefs[EPG_AUTO_SYNC_ENABLED] = enabled
        }
    }

    val iptvBufferProfile: Flow<IptvBufferProfile> = context.iptvDataStore.data.map { prefs ->
        prefs[IPTV_BUFFER_PROFILE]?.let {
            runCatching { IptvBufferProfile.valueOf(it) }.getOrDefault(IptvBufferProfile.STANDARD)
        } ?: IptvBufferProfile.STANDARD
    }

    suspend fun setIptvBufferProfile(profile: IptvBufferProfile) {
        context.iptvDataStore.edit { prefs ->
            prefs[IPTV_BUFFER_PROFILE] = profile.name
        }
    }
}
