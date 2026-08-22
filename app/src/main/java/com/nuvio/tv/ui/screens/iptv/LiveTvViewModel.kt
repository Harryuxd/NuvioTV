package com.nuvio.tv.ui.screens.iptv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.iptv.IptvPreferencesDataStore
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.domain.model.iptv.IptvGroup
import com.nuvio.tv.domain.model.iptv.IptvPlaylist
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import com.nuvio.tv.domain.repository.IptvEpgRepository
import com.nuvio.tv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveTvUiState(
    val playlists: List<IptvPlaylist> = emptyList(),
    val activePlaylist: IptvPlaylist? = null,
    val groups: List<IptvGroup> = emptyList(),
    val selectedGroupId: String = GROUP_ALL,
    val channels: List<IptvChannel> = emptyList(),
    val selectedChannel: IptvChannel? = null,
    val currentProgram: IptvEpgProgram? = null,
    val nextProgram: IptvEpgProgram? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

const val GROUP_ALL = "__all__"
const val GROUP_FAVORITES = "__favorites__"
const val GROUP_RECENTS = "__recents__"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val iptvRepository: IptvRepository,
    private val epgRepository: IptvEpgRepository,
    private val preferencesDataStore: IptvPreferencesDataStore
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow(GROUP_ALL)
    val selectedGroupId: StateFlow<String> = _selectedGroupId.asStateFlow()

    private val _selectedChannel = MutableStateFlow<IptvChannel?>(null)
    val selectedChannel: StateFlow<IptvChannel?> = _selectedChannel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _guideWindowStart = MutableStateFlow(roundToHalfHour(System.currentTimeMillis()))
    val guideWindowStart: StateFlow<Long> = _guideWindowStart.asStateFlow()
    private val guideWindowDurationMs = 3 * 60 * 60 * 1000L

    val playlists = iptvRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlaylistId = iptvRepository.getActivePlaylistId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePlaylist: StateFlow<IptvPlaylist?> = combine(playlists, activePlaylistId) { list, activeId ->
        if (list.isEmpty()) null
        else list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groups: StateFlow<List<IptvGroup>> = activePlaylist.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else iptvRepository.getGroups(playlist.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<IptvChannel>> = combine(
        activePlaylist,
        _selectedGroupId,
        _searchQuery
    ) { playlist, groupId, query ->
        Triple(playlist, groupId, query)
    }.flatMapLatest { (playlist, groupId, query) ->
        if (playlist == null) {
            flowOf(emptyList())
        } else {
            when {
                query.isNotBlank() -> iptvRepository.getChannels(playlist.id, query = query)
                groupId == GROUP_FAVORITES -> iptvRepository.getFavoriteChannels(playlist.id)
                groupId == GROUP_RECENTS -> iptvRepository.getRecentChannels(playlist.id)
                groupId == GROUP_ALL -> iptvRepository.getChannels(playlist.id)
                else -> iptvRepository.getChannels(playlist.id, groupId = groupId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val guideSchedules: StateFlow<Map<String, List<IptvEpgProgram>>> = combine(channels, _guideWindowStart) { visibleChannels, start ->
        visibleChannels to start
    }.flatMapLatest { (visibleChannels, start) ->
        flow {
            val end = start + guideWindowDurationMs
            emit(visibleChannels.associate { channel ->
                channel.id to channel.tvgId?.takeIf { it.isNotBlank() }
                    ?.let { epgRepository.getScheduleForChannel(it, start, end).first() }.orEmpty()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedChannelEpg: StateFlow<Pair<IptvEpgProgram?, IptvEpgProgram?>> = _selectedChannel.flatMapLatest { channel ->
        val tvgId = channel?.tvgId
        if (tvgId.isNullOrBlank()) {
            flowOf(Pair(null, null))
        } else {
            epgRepository.getCurrentAndNextProgram(tvgId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(null, null))

    init {
        // Auto-select first channel when channels load
        viewModelScope.launch {
            channels.collect { chList ->
                if (_selectedChannel.value == null || chList.none { it.id == _selectedChannel.value?.id }) {
                    _selectedChannel.value = chList.firstOrNull()
                }
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        viewModelScope.launch {
            iptvRepository.setActivePlaylistId(playlistId)
            _selectedGroupId.value = GROUP_ALL
            _selectedChannel.value = null
        }
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun selectChannel(channel: IptvChannel) {
        _selectedChannel.value = channel
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun moveGuideWindow(minutes: Int) {
        _guideWindowStart.value += minutes * 60 * 1000L
    }

    fun jumpGuideToNow() {
        _guideWindowStart.value = roundToHalfHour(System.currentTimeMillis())
    }

    fun toggleFavorite(channel: IptvChannel) {
        viewModelScope.launch {
            iptvRepository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun recordWatched(channel: IptvChannel) {
        viewModelScope.launch {
            iptvRepository.recordChannelWatched(channel.id)
        }
    }

    fun refreshActivePlaylist() {
        val playlist = activePlaylist.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            val result = iptvRepository.refreshPlaylist(playlist.id)
            result.onFailure {
                _errorMessage.value = it.message ?: "Failed to refresh playlist"
            }
            _isRefreshing.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addOrUpdatePlaylist(
        playlist: IptvPlaylist,
        credentials: XtreamCredentials?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            val result = iptvRepository.addOrUpdatePlaylist(playlist, credentials)
            result.onSuccess { added ->
                iptvRepository.setActivePlaylistId(added.id)
                // Trigger initial sync
                iptvRepository.refreshPlaylist(added.id)
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add playlist"
            }
            _isRefreshing.value = false
        }
    }

    /** Returns the Xtream login associated with a playlist when it is being edited. */
    suspend fun getPlaylistCredentials(playlistId: String): XtreamCredentials? {
        return iptvRepository.getCredentials(playlistId)
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            iptvRepository.deletePlaylist(playlistId)
            _selectedChannel.value = null
            _selectedGroupId.value = GROUP_ALL
        }
    }

    private fun roundToHalfHour(timeMs: Long): Long {
        val halfHour = 30 * 60 * 1000L
        return timeMs / halfHour * halfHour
    }
}
