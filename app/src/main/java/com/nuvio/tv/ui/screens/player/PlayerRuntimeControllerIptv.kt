package com.nuvio.tv.ui.screens.player

import androidx.media3.common.util.UnstableApi
import com.nuvio.tv.domain.model.iptv.IptvChannel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun PlayerRuntimeController.loadIptvAlternatives(forceRefresh: Boolean) {
    val channelId = currentIptvChannelId ?: return
    val current = _uiState.value
    if (!forceRefresh && current.isIptvSourcesPanel && current.iptvAlternativeChannels.isNotEmpty()) return
    _uiState.update {
        it.copy(
            isIptvSourcesPanel = true,
            isLoadingIptvAlternatives = true,
            iptvAlternativesError = null,
            iptvAlternativeChannels = if (forceRefresh) emptyList() else it.iptvAlternativeChannels
        )
    }
    scope.launch {
        runCatching { iptvRepository.getChannelAlternatives(channelId) }
            .onSuccess { channels ->
                _uiState.update { it.copy(isLoadingIptvAlternatives = false, iptvAlternativeChannels = channels) }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingIptvAlternatives = false,
                        iptvAlternativesError = error.message ?: "Unable to load alternative channels"
                    )
                }
            }
    }
}

@OptIn(UnstableApi::class)
internal fun PlayerRuntimeController.switchToIptvAlternative(channel: IptvChannel) {
    if (channel.id == currentIptvChannelId) {
        dismissSourcesPanel()
        return
    }
    stopTorrentStream()
    flushPlaybackSnapshotForSwitchOrExit()
    val request = PlayerMediaSourceFactory.normalizePlaybackRequest(channel.streamUrl, channel.headers)
    currentStreamUrl = request.url
    currentHeaders = request.headers
    currentFilename = request.url.substringBefore('?').substringAfterLast('/').takeIf { it.contains('.') }
    currentStreamMimeType = PlayerMediaSourceFactory.inferMimeType(request.url, currentFilename, emptyMap())
    currentStreamResponseHeaders = emptyMap()
    currentIptvChannelId = channel.id
    resetErrorRetryState()
    resetLoadingOverlayForNewStream()
    releasePlayer(flushPlaybackState = false)
    _uiState.update {
        it.copy(
            title = channel.name,
            contentName = channel.groupTitle,
            currentStreamName = channel.name,
            currentStreamUrl = request.url,
            currentStreamInfoHash = null,
            currentStreamFileIdx = null,
            currentStreamAddonName = null,
            logo = channel.logoUrl,
            backdrop = channel.logoUrl,
            isBuffering = true,
            error = null,
            showSourcesPanel = false,
            isIptvSourcesPanel = false,
            isLoadingIptvAlternatives = false,
            currentIptvChannelId = channel.id,
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            selectedAudioTrackIndex = -1,
            selectedSubtitleTrackIndex = -1,
            isTorrentStream = false
        )
    }
    scope.launch { iptvRepository.recordChannelWatched(channel.id) }
    initializePlayer(request.url, request.headers)
}
