package com.nuvio.tv.ui.screens.iptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.screens.iptv.playlist.AddEditPlaylistDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onPlayChannel: (IptvChannel) -> Unit,
    onOpenPlaylistManager: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val (currentEpg, nextEpg) = viewModel.selectedChannelEpg.collectAsState().value
    val guideSchedules by viewModel.guideSchedules.collectAsState()
    val guideWindowStart by viewModel.guideWindowStart.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val previewAudioEnabled by viewModel.previewPlayerAudioEnabled.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var contextMenuChannel by remember { mutableStateOf<IptvChannel?>(null) }

    val groupsLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val channelsLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
    ) {
        if (isInitialLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(Modifier.size(40.dp))
            }
        } else if (playlists.isEmpty()) {
            // Empty State
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
                        tint = NuvioTheme.colors.Secondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.iptv_no_playlists),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = NuvioTheme.colors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add an M3U or Xtream playlist in settings to start watching live TV.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NuvioTheme.colors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.colors(
                            containerColor = NuvioTheme.colors.Secondary,
                            contentColor = NuvioTheme.colors.OnSecondary,
                            focusedContainerColor = NuvioTheme.colors.SecondaryVariant,
                            focusedContentColor = NuvioTheme.colors.OnSecondaryVariant
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                        border = ButtonDefaults.border(
                            focusedBorder = Border(
                                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                shape = RoundedCornerShape(NuvioTheme.radii.md)
                            )
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NuvioTheme.colors.OnSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.iptv_add_playlist),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NuvioTheme.colors.OnSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // Main Live TV 3-Column Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp)
            ) {
                // Column 1: Groups & Categories (Width ~240dp)
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                ) {
                    // Header with Playlist Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.iptv_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = NuvioTheme.colors.TextPrimary
                            )
                            Text(
                                text = activePlaylist?.name ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.TextSecondary,
                                maxLines = 1
                            )
                        }

                        Surface(
                            onClick = onOpenPlaylistManager,
                            shape = ClickableSurfaceDefaults.shape(CircleShape),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = NuvioTheme.colors.BackgroundCard,
                                focusedContainerColor = NuvioTheme.colors.FocusBackground
                            ),
                            border = ClickableSurfaceDefaults.border(
                                border = Border(
                                    border = BorderStroke(1.dp, NuvioTheme.colors.Border),
                                    shape = CircleShape
                                ),
                                focusedBorder = Border(
                                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                    shape = CircleShape
                                )
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Manage",
                                    tint = NuvioTheme.colors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Search input
                    LiveTvSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Groups List
                    LazyColumn(
                        state = groupsLazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            GroupNavItem(
                                title = stringResource(R.string.iptv_group_all),
                                icon = Icons.Default.List,
                                channelCount = activePlaylist?.channelCount,
                                isSelected = selectedGroupId == GROUP_ALL,
                                onClick = { viewModel.selectGroup(GROUP_ALL) }
                            )
                        }

                        item {
                            GroupNavItem(
                                title = stringResource(R.string.iptv_group_favorites),
                                icon = Icons.Default.Favorite,
                                isSelected = selectedGroupId == GROUP_FAVORITES,
                                onClick = { viewModel.selectGroup(GROUP_FAVORITES) }
                            )
                        }

                        item {
                            GroupNavItem(
                                title = stringResource(R.string.iptv_group_recents),
                                icon = Icons.Default.History,
                                isSelected = selectedGroupId == GROUP_RECENTS,
                                onClick = { viewModel.selectGroup(GROUP_RECENTS) }
                            )
                        }

                        if (groups.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "CATEGORIES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = NuvioTheme.colors.TextTertiary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            items(groups, key = { it.id }) { grp ->
                                GroupNavItem(
                                    title = grp.title,
                                    channelCount = grp.channelCount,
                                    isSelected = selectedGroupId == grp.id,
                                    onClick = { viewModel.selectGroup(grp.id) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                TvGuidePane(
                    channels = channels,
                    schedules = guideSchedules,
                    windowStart = guideWindowStart,
                    selectedChannel = selectedChannel,
                    currentProgram = currentEpg,
                    nextProgram = nextEpg,
                    isRefreshing = isRefreshing,
                    previewAudioEnabled = previewAudioEnabled,
                    lazyListState = channelsLazyListState,
                    onMoveWindow = viewModel::moveGuideWindow,
                    onNow = viewModel::jumpGuideToNow,
                    onSelectChannel = viewModel::selectChannel,
                    onPlayChannel = { channel -> viewModel.recordWatched(channel); onPlayChannel(channel) },
                    onLongClickChannel = { contextMenuChannel = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        contextMenuChannel?.let { ch ->
            ChannelActionMenuDialog(
                channel = ch,
                onDismiss = { contextMenuChannel = null },
                onPlay = { onPlayChannel(ch) },
                onToggleFavorite = { viewModel.toggleFavorite(ch) }
            )
        }

        if (showAddDialog) {
            AddEditPlaylistDialog(
                onDismiss = { showAddDialog = false },
                onSave = { playlist, credentials ->
                    viewModel.addOrUpdatePlaylist(playlist, credentials) {
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveTvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        onClick = { focusRequester.requestFocus() },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NuvioTheme.radii.full)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundElevated,
            focusedContainerColor = NuvioTheme.colors.BackgroundElevated
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.full)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(NuvioTheme.radii.full)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.full)
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = NuvioTheme.colors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search channels...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.TextTertiary
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = NuvioTheme.colors.TextPrimary
                    ),
                    cursorBrush = SolidColor(NuvioTheme.colors.Secondary)
                )
            }

            if (query.isNotEmpty()) {
                Surface(
                    onClick = { onQueryChange("") },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground
                    ),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NuvioTheme.colors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
