package com.nuvio.tv.ui.screens.iptv.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.iptv.IptvPlaylist
import com.nuvio.tv.domain.model.iptv.IptvPlaylistType
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.screens.iptv.LiveTvViewModel
import com.nuvio.tv.ui.screens.settings.SettingsStandaloneScaffold
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<IptvPlaylist?>(null) }
    var credentialsToEdit by remember { mutableStateOf<XtreamCredentials?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    BackHandler {
        onNavigateBack()
    }

    SettingsStandaloneScaffold(
        title = stringResource(R.string.iptv_manage_playlists),
        subtitle = "Add, edit, refresh, and switch between your M3U and Xtream IPTV accounts"
    ) {
        // Action Bar (Add button + Refresh indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    playlistToEdit = null
                    credentialsToEdit = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary,
                    focusedContainerColor = NuvioTheme.colors.FocusBackground,
                    focusedContentColor = NuvioTheme.colors.TextPrimary
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                border = ButtonDefaults.border(
                    border = Border(
                        border = BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    ),
                    focusedBorder = Border(
                        border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    )
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NuvioTheme.colors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.iptv_add_playlist),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NuvioTheme.colors.TextPrimary
                    )
                }
            }

            AnimatedVisibility(visible = isRefreshing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingIndicator(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.iptv_refreshing),
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.Secondary
                    )
                }
            }
        }

        // Error Banner
        errorMessage?.let { error ->
            Card(
                onClick = { viewModel.clearError() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                colors = CardDefaults.colors(
                    containerColor = NuvioTheme.colors.Error.copy(alpha = 0.15f)
                ),
                border = CardDefaults.border(
                    border = Border(
                        border = BorderStroke(1.dp, NuvioTheme.colors.Error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.Error,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelSmall,
                        color = NuvioTheme.colors.TextSecondary
                    )
                }
            }
        }

        // Playlists List or Empty State
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = NuvioTheme.colors.Secondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.iptv_no_playlists),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NuvioTheme.colors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.iptv_no_playlists_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuvioTheme.colors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            playlistToEdit = null
                            credentialsToEdit = null
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = NuvioTheme.colors.BackgroundCard,
                            contentColor = NuvioTheme.colors.TextPrimary,
                            focusedContainerColor = NuvioTheme.colors.FocusBackground,
                            focusedContentColor = NuvioTheme.colors.TextPrimary
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                        border = ButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(NuvioTheme.radii.md)
                            ),
                            focusedBorder = Border(
                                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                shape = RoundedCornerShape(NuvioTheme.radii.md)
                            )
                        )
                    ) {
                        Text(
                            text = "Add Your First Playlist",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NuvioTheme.colors.TextPrimary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    val isActive = activePlaylist?.id == playlist.id

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Card(
                        onClick = { viewModel.selectPlaylist(playlist.id) },
                        modifier = Modifier.weight(1f),
                        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.lg)),
                        colors = CardDefaults.colors(
                            containerColor = if (isActive) NuvioTheme.colors.BackgroundElevated else NuvioTheme.colors.BackgroundCard,
                            focusedContainerColor = if (isActive) NuvioTheme.colors.BackgroundElevated else NuvioTheme.colors.FocusBackground
                        ),
                        border = CardDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    if (isActive) 1.5.dp else 1.dp,
                                    if (isActive) NuvioTheme.colors.Secondary.copy(alpha = 0.6f) else NuvioTheme.colors.Border.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(NuvioTheme.radii.lg)
                            ),
                            focusedBorder = Border(
                                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                shape = RoundedCornerShape(NuvioTheme.radii.lg)
                            )
                        ),
                        scale = CardDefaults.scale(focusedScale = 1.01f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status / Check Icon without background fill
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = NuvioTheme.colors.TextTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            // Playlist Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NuvioTheme.colors.TextPrimary
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Type Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NuvioTheme.colors.SurfaceVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (playlist.type == IptvPlaylistType.M3U) "M3U" else "Xtream Codes",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = NuvioTheme.colors.TextSecondary
                                        )
                                    }

                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NuvioTheme.colors.SurfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = NuvioTheme.colors.TextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "${playlist.channelCount} channels",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NuvioTheme.colors.TextSecondary
                                    )

                                    if (playlist.lastRefreshedEpochMs > 0) {
                                        Text(
                                            text = "Synced: ${dateFormat.format(Date(playlist.lastRefreshedEpochMs))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NuvioTheme.colors.TextTertiary
                                        )
                                    }
                                }
                            }

                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    PlaylistActionButton(Icons.Default.Edit, "Edit") {
                        coroutineScope.launch {
                            credentialsToEdit = viewModel.getPlaylistCredentials(playlist.id)
                            playlistToEdit = playlist
                            showAddDialog = true
                        }
                    }
                    PlaylistActionButton(Icons.Default.Refresh, "Refresh") {
                        viewModel.selectPlaylist(playlist.id)
                        viewModel.refreshActivePlaylist()
                    }
                    PlaylistActionButton(Icons.Default.Delete, "Delete", NuvioTheme.colors.Error) {
                        viewModel.deletePlaylist(playlist.id)
                    }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditPlaylistDialog(
            initialPlaylist = playlistToEdit,
            initialCredentials = credentialsToEdit,
            onDismiss = {
                showAddDialog = false
                playlistToEdit = null
                credentialsToEdit = null
            },
            onSave = { playlist, credentials ->
                viewModel.addOrUpdatePlaylist(playlist, credentials) {
                    showAddDialog = false
                    playlistToEdit = null
                    credentialsToEdit = null
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaylistActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = NuvioTheme.colors.TextSecondary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = NuvioTheme.colors.FocusBackground),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = CircleShape)),
        modifier = Modifier.size(42.dp)
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp)) } }
}
