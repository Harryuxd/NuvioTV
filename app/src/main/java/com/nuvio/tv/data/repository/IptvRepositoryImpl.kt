package com.nuvio.tv.data.repository

import com.nuvio.tv.data.iptv.m3u.M3uParser
import com.nuvio.tv.data.iptv.logo.IptvLogoResolver
import com.nuvio.tv.data.iptv.xtream.XtreamClient
import com.nuvio.tv.data.local.iptv.IptvCredentialStore
import com.nuvio.tv.data.local.iptv.IptvPreferencesDataStore
import com.nuvio.tv.data.local.iptv.db.IptvDatabaseHelper
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import com.nuvio.tv.domain.model.iptv.IptvEpgSourceKind
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvPlaylist
import com.nuvio.tv.domain.model.iptv.IptvPlaylistType
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import com.nuvio.tv.domain.repository.IptvEpgRepository
import com.nuvio.tv.domain.repository.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvRepositoryImpl @Inject constructor(
    private val dbHelper: IptvDatabaseHelper,
    private val credentialStore: IptvCredentialStore,
    private val preferencesDataStore: IptvPreferencesDataStore,
    private val xtreamClient: XtreamClient,
    private val epgRepository: IptvEpgRepository,
    private val logoResolver: IptvLogoResolver,
    private val okHttpClient: OkHttpClient
) : IptvRepository {

    /** Per-process cache: source panels reopen instantly until their playlist changes. */
    private val channelAlternativesCache = ConcurrentHashMap<String, List<IptvChannel>>()

    override fun getPlaylists(): Flow<List<IptvPlaylist>> {
        return dbHelper.getAllPlaylistsFlow()
    }

    override fun getActivePlaylistId(): Flow<String?> {
        return preferencesDataStore.activePlaylistId
    }

    override suspend fun setActivePlaylistId(playlistId: String) {
        preferencesDataStore.setActivePlaylistId(playlistId)
    }

    override suspend fun getPlaylistById(playlistId: String): IptvPlaylist? = withContext(Dispatchers.IO) {
        dbHelper.getPlaylistById(playlistId)
    }

    override suspend fun addOrUpdatePlaylist(
        playlist: IptvPlaylist,
        credentials: XtreamCredentials?
    ): Result<IptvPlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            var playlistToSave = playlist
            if (playlist.type == IptvPlaylistType.XTREAM && credentials != null) {
                // Test authentication first
                xtreamClient.authenticate(credentials).getOrThrow()
                credentialStore.saveCredentials(playlist.id, credentials)
                if (playlistToSave.epgUrl.isNullOrBlank()) {
                    val defaultXtreamEpgUrl = "${credentials.serverUrl.trimEnd('/')}/xmltv.php?username=${credentials.username}&password=${credentials.password}"
                    playlistToSave = playlistToSave.copy(epgUrl = defaultXtreamEpgUrl)
                }
            }

            dbHelper.insertOrUpdatePlaylist(playlistToSave)
            playlistToSave
        }
    }

    override suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        invalidateChannelAlternatives(playlistId)
        credentialStore.removeCredentials(playlistId)
        epgRepository.clearEpgForPlaylist(playlistId)
        dbHelper.deletePlaylist(playlistId)
    }

    override suspend fun refreshPlaylist(playlistId: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = dbHelper.getPlaylistById(playlistId)
                ?: throw IOException("Playlist not found: $playlistId")

            // Preserve existing favorites & watch history
            val existingFavorites = dbHelper.getFavoriteChannelIds(playlistId).toSet()
            val existingHistory = dbHelper.getRecentChannelHistories(playlistId)

            val (groups, channels, detectedEpgUrl) = when (playlist.type) {
                IptvPlaylistType.M3U -> {
                    val stream: InputStream = if (playlist.sourceLocation.startsWith("http://") || playlist.sourceLocation.startsWith("https://")) {
                        val req = Request.Builder().url(playlist.sourceLocation).build()
                        val resp = okHttpClient.newCall(req).execute()
                        if (!resp.isSuccessful) throw IOException("Failed to download M3U: HTTP ${resp.code}")
                        resp.body?.byteStream() ?: throw IOException("Empty M3U response")
                    } else {
                        val file = File(playlist.sourceLocation)
                        if (!file.exists()) throw IOException("Local M3U file not found: ${playlist.sourceLocation}")
                        FileInputStream(file)
                    }

                    stream.use { s ->
                        val result = M3uParser.parse(s, playlistId)
                        Triple(result.groups, result.channels, result.detectedEpgUrl)
                    }
                }
                IptvPlaylistType.XTREAM -> {
                    val creds = credentialStore.getCredentials(playlistId)
                        ?: throw IOException("Missing Xtream credentials for playlist $playlistId")
                    val (g, c) = xtreamClient.fetchLiveStreams(playlistId, creds).getOrThrow()
                    val defaultXtreamEpg = "${creds.serverUrl.trimEnd('/')}/xmltv.php?username=${creds.username}&password=${creds.password}"
                    Triple(g, c, defaultXtreamEpg)
                }
            }

            if (channels.isEmpty()) {
                throw IOException("No channels found in playlist")
            }

            // Restore favorites & watch history
            val priority = preferencesDataStore.logoPriorityOrder.firstOrNull() ?: listOf("TV_LOGOS", "IPTV_ORG", "PROVIDER")
            val fallback = preferencesDataStore.useProviderLogoFallback.firstOrNull() ?: true
            val updatedChannels = logoResolver.resolve(channels, priority, fallback).map { ch ->
                ch.copy(
                    isFavorite = ch.id in existingFavorites,
                    lastWatchedEpochMs = existingHistory[ch.id]
                )
            }

            dbHelper.replaceGroups(playlistId, groups)
            dbHelper.replaceChannels(playlistId, updatedChannels)
            invalidateChannelAlternatives(playlistId)

            val timestamp = System.currentTimeMillis()
            dbHelper.updateRefreshStats(playlistId, timestamp, channels.size)

            // Register & refresh EPG source if available
            val effectiveEpgUrl = playlist.epgUrl?.takeIf { it.isNotBlank() } ?: detectedEpgUrl
            if (!effectiveEpgUrl.isNullOrBlank()) {
                val epgSource = IptvEpgSource(
                    id = "playlist_$playlistId",
                    name = "${playlist.name} (Playlist Guide)",
                    location = effectiveEpgUrl,
                    kind = IptvEpgSourceKind.PLAYLIST,
                    playlistId = playlistId
                )
                dbHelper.upsertEpgSource(epgSource)
                epgRepository.refreshSource(epgSource)
            }

            channels.size
        }
    }

    override fun getGroups(playlistId: String): Flow<List<IptvGroup>> {
        return dbHelper.getGroupsFlow(playlistId)
    }

    override fun getChannels(
        playlistId: String,
        groupId: String?,
        query: String?
    ): Flow<List<IptvChannel>> {
        return dbHelper.getChannelsFlow(playlistId, groupId, query)
    }

    override fun getFavoriteChannels(playlistId: String): Flow<List<IptvChannel>> {
        return dbHelper.getFavoriteChannelsFlow(playlistId)
    }

    override fun getRecentChannels(playlistId: String, limit: Int): Flow<List<IptvChannel>> {
        return dbHelper.getRecentChannelsFlow(playlistId, limit)
    }

    override suspend fun getChannelById(channelId: String): IptvChannel? = withContext(Dispatchers.IO) {
        dbHelper.getChannelById(channelId)
    }

    override suspend fun getChannelAlternatives(channelId: String): List<IptvChannel> = withContext(Dispatchers.IO) {
        val baseChannel = dbHelper.getChannelById(channelId) ?: return@withContext emptyList()
        val playlistId = baseChannel.playlistId
        val cacheKey = "$playlistId|$channelId"
        channelAlternativesCache[cacheKey]?.let { return@withContext it }

        val allChannelsInPlaylist = dbHelper.getChannels(playlistId)
        val baseFamilyKey = baseChannel.name.toChannelFamilyKey()

        val alternatives = allChannelsInPlaylist.filter { candidate ->
            candidate.id != baseChannel.id &&
            candidate.name.toChannelFamilyKey() == baseFamilyKey
        }

        channelAlternativesCache[cacheKey] = alternatives
        alternatives
    }

    override suspend fun toggleFavorite(channelId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dbHelper.setFavorite(channelId, isFavorite)
    }

    override suspend fun recordChannelWatched(channelId: String) = withContext(Dispatchers.IO) {
        dbHelper.recordWatched(channelId, System.currentTimeMillis())
    }

    override suspend fun getCredentials(playlistId: String): XtreamCredentials? = withContext(Dispatchers.IO) {
        credentialStore.getCredentials(playlistId)
    }

    override suspend fun reResolveAllLogos(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val priority = preferencesDataStore.logoPriorityOrder.firstOrNull() ?: listOf("TV_LOGOS", "IPTV_ORG", "PROVIDER")
            val fallback = preferencesDataStore.useProviderLogoFallback.firstOrNull() ?: true
            val playlists = dbHelper.getAllPlaylists()
            var totalUpdated = 0

            for (playlist in playlists) {
                val channels = dbHelper.getChannels(playlist.id)
                if (channels.isNotEmpty()) {
                    val resolved = logoResolver.resolve(channels, priority, fallback)
                    dbHelper.replaceChannels(playlist.id, resolved)
                    invalidateChannelAlternatives(playlist.id)
                    totalUpdated += resolved.size
                }
            }
            totalUpdated
        }
    }

    private fun invalidateChannelAlternatives(playlistId: String) {
        channelAlternativesCache.keys.removeIf { it.startsWith("$playlistId|") }
    }
}

/** Collapses provider labels such as "CA EN: CP24 HD (R)" into "cp24". */
private fun String.toChannelFamilyKey(): String = lowercase()
    .replace(Regex("^[a-z]{2,}(?:\\s+[a-z]{2,})?\\s*:\\s*"), "")
    .replace(Regex("\\([^)]*\\)"), " ")
    .replace(Regex("\\b(?:uhd|fhd|hd|sd|4k|1080p|720p|backup|alternate|alt|feed|raw)\\b"), " ")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
