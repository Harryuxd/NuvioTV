package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.InternalPlayerEngine
import com.nuvio.tv.data.local.iptv.IptvBufferProfile
import com.nuvio.tv.data.local.iptv.IptvPreferencesDataStore
import com.nuvio.tv.domain.repository.IptvEpgRepository
import com.nuvio.tv.domain.repository.IptvRepository
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import com.nuvio.tv.domain.model.iptv.IptvEpgSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IptvSettingsUiState(
    val playerEngine: InternalPlayerEngine = InternalPlayerEngine.EXOPLAYER,
    val epgAutoSync: Boolean = true,
    val bufferProfile: IptvBufferProfile = IptvBufferProfile.STANDARD,
    val playlistCount: Int = 0,
    val channelCount: Int = 0,
    val isClearingCache: Boolean = false,
    val isCacheCleared: Boolean = false,
    val manualEpgSources: List<IptvEpgSource> = emptyList(),
    val playlistEpgSources: List<IptvEpgSource> = emptyList(),
    val isRefreshingSources: Boolean = false
)

@HiltViewModel
class IptvSettingsViewModel @Inject constructor(
    private val iptvPreferencesDataStore: IptvPreferencesDataStore,
    private val iptvRepository: IptvRepository,
    private val epgRepository: IptvEpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IptvSettingsUiState())
    val uiState: StateFlow<IptvSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            iptvPreferencesDataStore.iptvPlayerEngine.collectLatest { engine ->
                _uiState.update { it.copy(playerEngine = engine) }
            }
        }
        viewModelScope.launch {
            epgRepository.getSources().collectLatest { sources ->
                _uiState.update { it.copy(
                    manualEpgSources = sources.filter { source -> source.kind == IptvEpgSourceKind.MANUAL },
                    playlistEpgSources = sources.filter { source -> source.kind == IptvEpgSourceKind.PLAYLIST }
                ) }
            }
        }
        viewModelScope.launch {
            iptvPreferencesDataStore.epgAutoSyncEnabled.collectLatest { enabled ->
                _uiState.update { it.copy(epgAutoSync = enabled) }
            }
        }
        viewModelScope.launch {
            iptvPreferencesDataStore.iptvBufferProfile.collectLatest { profile ->
                _uiState.update { it.copy(bufferProfile = profile) }
            }
        }
        viewModelScope.launch {
            iptvRepository.getPlaylists().collectLatest { playlists ->
                val totalChannels = playlists.sumOf { it.channelCount }
                _uiState.update {
                    it.copy(
                        playlistCount = playlists.size,
                        channelCount = totalChannels
                    )
                }
            }
        }
    }

    fun setPlayerEngine(engine: InternalPlayerEngine) {
        viewModelScope.launch {
            iptvPreferencesDataStore.setIptvPlayerEngine(engine)
        }
    }

    fun setEpgAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            iptvPreferencesDataStore.setEpgAutoSyncEnabled(enabled)
        }
    }

    fun setBufferProfile(profile: IptvBufferProfile) {
        viewModelScope.launch {
            iptvPreferencesDataStore.setIptvBufferProfile(profile)
        }
    }

    fun clearEpgCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true, isCacheCleared = false) }
            runCatching {
                epgRepository.clearAllEpg()
            }
            _uiState.update { it.copy(isClearingCache = false, isCacheCleared = true) }
        }
    }

    fun saveEpgSource(source: IptvEpgSource, onComplete: () -> Unit) {
        viewModelScope.launch { epgRepository.saveManualSource(source).onSuccess { saved -> epgRepository.refreshSource(saved); onComplete() } }
    }

    fun refreshEpgSource(source: IptvEpgSource) {
        viewModelScope.launch { epgRepository.refreshSource(source) }
    }

    fun refreshAllEpgSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSources = true) }
            epgRepository.refreshManualSources()
            _uiState.update { it.copy(isRefreshingSources = false) }
        }
    }

    fun deleteEpgSource(sourceId: String) {
        viewModelScope.launch { epgRepository.deleteManualSource(sourceId) }
    }
}
