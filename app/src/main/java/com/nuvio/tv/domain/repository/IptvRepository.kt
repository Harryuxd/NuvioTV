package com.nuvio.tv.domain.repository

import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvPlaylist
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import kotlinx.coroutines.flow.Flow

interface IptvRepository {
    fun getPlaylists(): Flow<List<IptvPlaylist>>
    fun getActivePlaylistId(): Flow<String?>
    suspend fun setActivePlaylistId(playlistId: String)
    suspend fun getPlaylistById(playlistId: String): IptvPlaylist?
    suspend fun addOrUpdatePlaylist(playlist: IptvPlaylist, credentials: XtreamCredentials? = null): Result<IptvPlaylist>
    suspend fun deletePlaylist(playlistId: String)
    suspend fun refreshPlaylist(playlistId: String): Result<Int>

    fun getGroups(playlistId: String): Flow<List<IptvGroup>>
    fun getChannels(playlistId: String, groupId: String? = null, query: String? = null): Flow<List<IptvChannel>>
    fun getFavoriteChannels(playlistId: String): Flow<List<IptvChannel>>
    fun getRecentChannels(playlistId: String, limit: Int = 20): Flow<List<IptvChannel>>
    suspend fun getChannelById(channelId: String): IptvChannel?
    /** Returns alternate feeds for a channel without crossing playlist/account boundaries. */
    suspend fun getChannelAlternatives(channelId: String): List<IptvChannel>

    suspend fun toggleFavorite(channelId: String, isFavorite: Boolean)
    suspend fun recordChannelWatched(channelId: String)
    suspend fun getCredentials(playlistId: String): XtreamCredentials?
}
