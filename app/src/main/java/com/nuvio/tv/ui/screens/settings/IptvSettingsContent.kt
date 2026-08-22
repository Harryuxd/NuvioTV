@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import com.nuvio.tv.data.local.InternalPlayerEngine
import com.nuvio.tv.data.local.iptv.IptvBufferProfile
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.domain.model.iptv.IptvEpgSource

@Composable
internal fun IptvSettingsContent(
    onNavigateToPlaylistManager: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
    viewModel: IptvSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sourceBeingEdited by remember { mutableStateOf<IptvEpgSource?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        SettingsDetailHeader(
            title = "IPTV & Live TV",
            subtitle = "Manage playlists, EPG data, buffer profiles, and playback engines"
        )

        // ── Section 1: Playlists & Accounts ──
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            val playlistSummary = when {
                uiState.playlistCount == 0 -> "No playlists added"
                uiState.playlistCount == 1 -> "1 playlist (${uiState.channelCount} channels)"
                else -> "${uiState.playlistCount} playlists (${uiState.channelCount} channels)"
            }

            SettingsActionRow(
                title = "Manage Playlists",
                subtitle = "Add, remove, or update M3U & Xtream accounts",
                value = playlistSummary,
                leadingIcon = Icons.Default.LiveTv,
                onClick = onNavigateToPlaylistManager,
                modifier = if (initialFocusRequester != null) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                }
            )
        }

        // ── Section 2: Playback Engine & Buffer ──
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            val engineLabel = when (uiState.playerEngine) {
                InternalPlayerEngine.EXOPLAYER -> "ExoPlayer (Hardware)"
                InternalPlayerEngine.MVP_PLAYER -> "MPV Player (libmpv)"
                InternalPlayerEngine.AUTO -> "Auto (Recommended)"
            }

            SettingsActionRow(
                title = "Default Player Engine",
                subtitle = "Choose media engine for IPTV channels",
                value = engineLabel,
                leadingIcon = Icons.Rounded.PlayArrow,
                onClick = {
                    val nextEngine = when (uiState.playerEngine) {
                        InternalPlayerEngine.EXOPLAYER -> InternalPlayerEngine.MVP_PLAYER
                        InternalPlayerEngine.MVP_PLAYER -> InternalPlayerEngine.AUTO
                        InternalPlayerEngine.AUTO -> InternalPlayerEngine.EXOPLAYER
                    }
                    viewModel.setPlayerEngine(nextEngine)
                }
            )

            SettingsActionRow(
                title = "Live Buffer Profile",
                subtitle = "Tune stream buffering for faster zapping or stability",
                value = uiState.bufferProfile.label,
                leadingIcon = Icons.Default.Speed,
                onClick = {
                    val nextProfile = when (uiState.bufferProfile) {
                        IptvBufferProfile.LOW_LATENCY -> IptvBufferProfile.STANDARD
                        IptvBufferProfile.STANDARD -> IptvBufferProfile.HIGH_STABILITY
                        IptvBufferProfile.HIGH_STABILITY -> IptvBufferProfile.LOW_LATENCY
                    }
                    viewModel.setBufferProfile(nextProfile)
                }
            )
        }

        // ── Section 3: TV Guide & EPG ──
        SettingsDetailHeader(title = "TV Guide & EPG", subtitle = "Guide sources, refresh status, and cache")
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                title = "Auto-Sync TV Guide (EPG)",
                subtitle = "Refresh guide data when playlists change and every 24 hours",
                checked = uiState.epgAutoSync,
                onToggle = { viewModel.setEpgAutoSync(!uiState.epgAutoSync) }
            )

            val clearCacheStatus = when {
                uiState.isClearingCache -> "Clearing..."
                uiState.isCacheCleared -> "Cleared"
                else -> "Clean up"
            }

            SettingsActionRow(
                title = "Refresh EPG Sources",
                subtitle = "Download schedules from playlist and manually added sources",
                value = if (uiState.isRefreshingSources) "Refreshing…" else "Refresh",
                leadingIcon = Icons.Default.Refresh,
                onClick = { viewModel.refreshAllEpgSources() }
            )

            SettingsActionRow(
                title = "Clear EPG Cache",
                subtitle = "Remove downloaded programme schedules; sources are kept",
                value = clearCacheStatus,
                leadingIcon = Icons.Default.DeleteSweep,
                onClick = { viewModel.clearEpgCache() }
            )

            SettingsActionRow(
                title = "Add EPG Source",
                subtitle = "Use a global XMLTV URL or local file as a fallback guide",
                leadingIcon = Icons.Default.Add,
                onClick = { sourceBeingEdited = null; showSourceDialog = true }
            )
        }

        if (uiState.manualEpgSources.isNotEmpty()) {
            SettingsDetailHeader(title = "Added manually", subtitle = "Global sources matched to channels by TVG ID")
            SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
                uiState.manualEpgSources.forEach { source ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsActionRow(
                            title = source.name,
                            subtitle = source.lastError ?: source.location,
                            value = source.statusLabel(),
                            leadingIcon = Icons.Default.Tv,
                            onClick = { sourceBeingEdited = source; showSourceDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
                        EpgActionButton(
                            icon = Icons.Default.Refresh,
                            description = "Refresh",
                            onClick = { viewModel.refreshEpgSource(source) }
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.xs))
                        EpgActionButton(
                            icon = Icons.Default.Edit,
                            description = "Edit",
                            onClick = { sourceBeingEdited = source; showSourceDialog = true }
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.xs))
                        EpgActionButton(
                            icon = Icons.Default.Delete,
                            description = "Delete",
                            tint = NuvioTheme.colors.Error,
                            onClick = { viewModel.deleteEpgSource(source.id) }
                        )
                    }
                }
            }
        }
        if (uiState.playlistEpgSources.isNotEmpty()) {
            SettingsDetailHeader(title = "From playlists", subtitle = "Guide URLs configured by your playlists")
            SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
                uiState.playlistEpgSources.forEach { source ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsActionRow(
                            title = source.name,
                            subtitle = source.lastError ?: source.location,
                            value = source.statusLabel(),
                            leadingIcon = Icons.Default.LiveTv,
                            onClick = { sourceBeingEdited = source; showSourceDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.sm))
                        EpgActionButton(
                            icon = Icons.Default.Refresh,
                            description = "Refresh",
                            onClick = { viewModel.refreshEpgSource(source) }
                        )
                        Spacer(modifier = Modifier.width(NuvioTheme.spacing.xs))
                        EpgActionButton(
                            icon = Icons.Default.Edit,
                            description = "Edit",
                            onClick = { sourceBeingEdited = source; showSourceDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showSourceDialog) EpgSourceDialog(
        source = sourceBeingEdited,
        onDismiss = { showSourceDialog = false },
        onSave = { source -> viewModel.saveEpgSource(source) { showSourceDialog = false } },
        onRefresh = { source -> viewModel.refreshEpgSource(source) },
        onDelete = { source -> viewModel.deleteEpgSource(source.id); showSourceDialog = false }
    )
}

@Composable
private fun EpgActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = NuvioTheme.colors.TextSecondary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = CircleShape
            )
        ),
        modifier = Modifier.size(38.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun IptvEpgSource.statusLabel(): String = when (syncStatus) {
    com.nuvio.tv.domain.model.iptv.IptvEpgSyncStatus.SUCCESS -> "Updated"
    com.nuvio.tv.domain.model.iptv.IptvEpgSyncStatus.FAILED -> "Failed"
    com.nuvio.tv.domain.model.iptv.IptvEpgSyncStatus.NEVER_SYNCED -> "Not synced"
}
