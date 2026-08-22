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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onPlayChannel: (IptvChannel) -> Unit = {},
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

    var contextMenuChannel by remember { mutableStateOf<IptvChannel?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val groupsLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val channelsLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Single unified ExoPlayer instance for both Preview and Fullscreen playback
    LaunchedEffect(selectedChannel?.id, selectedChannel?.streamUrl, isFullscreen, previewAudioEnabled) {
        val streamUrl = selectedChannel?.streamUrl
        if (streamUrl.isNullOrBlank()) {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            isPlaying = false
            return@LaunchedEffect
        }

        val currentUri = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
        if (exoPlayer != null && currentUri == streamUrl) {
            exoPlayer?.volume = if (isFullscreen || previewAudioEnabled) 1f else 0f
            return@LaunchedEffect
        }

        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        isPlaying = false

        if (!isFullscreen) {
            delay(500) // Debounce fast channel scrolling
        }

        val player = exoPlayer ?: ExoPlayer.Builder(context).build().also { exoPlayer = it }
        player.apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            volume = if (isFullscreen || previewAudioEnabled) 1f else 0f
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isPlaying = true
                    }
                }
            })
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                exoPlayer?.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            exoPlayer = null
        }
    }

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
                        onClick = onOpenPlaylistManager,
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.iptv_add_playlist),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        } else {
            // Main Live TV Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 24.dp)
            ) {
                // Left Sidebar: Playlist Header + Groups/Categories
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                ) {
                    // Playlist Header + Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE TV",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = NuvioTheme.colors.TextPrimary
                            )
                            Text(
                                text = activePlaylist?.name ?: "Playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.Secondary,
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
                    player = exoPlayer,
                    isPlaying = isPlaying,
                    lazyListState = channelsLazyListState,
                    onMoveWindow = viewModel::moveGuideWindow,
                    onNow = viewModel::jumpGuideToNow,
                    onSelectChannel = viewModel::selectChannel,
                    onExpandFullscreen = {
                        selectedChannel?.let { viewModel.recordWatched(it) }
                        isFullscreen = true
                    },
                    onLongClickChannel = { contextMenuChannel = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Fullscreen Unified Player Overlay
        if (isFullscreen) {
            val currentIndex = channels.indexOfFirst { it.id == selectedChannel?.id }
            LiveTvFullscreenPlayerOverlay(
                player = exoPlayer,
                channel = selectedChannel,
                currentProgram = currentEpg,
                isPlaying = isPlaying,
                onToggleFavorite = {
                    selectedChannel?.let { viewModel.toggleFavorite(it) }
                },
                onTogglePlayPause = {
                    if (exoPlayer?.isPlaying == true) {
                        exoPlayer?.pause()
                    } else {
                        exoPlayer?.play()
                    }
                },
                onNextChannel = {
                    if (channels.isNotEmpty() && currentIndex >= 0) {
                        val next = channels[(currentIndex + 1) % channels.size]
                        viewModel.selectChannel(next)
                    }
                },
                onPreviousChannel = {
                    if (channels.isNotEmpty() && currentIndex >= 0) {
                        val prev = channels[(currentIndex - 1 + channels.size) % channels.size]
                        viewModel.selectChannel(prev)
                    }
                },
                onExitFullscreen = {
                    isFullscreen = false
                }
            )
        }

        contextMenuChannel?.let { ch ->
            ChannelActionMenuDialog(
                channel = ch,
                onDismiss = { contextMenuChannel = null },
                onPlay = {
                    viewModel.selectChannel(ch)
                    viewModel.recordWatched(ch)
                    isFullscreen = true
                },
                onToggleFavorite = { viewModel.toggleFavorite(ch) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GroupNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    channelCount: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.sm)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) NuvioTheme.colors.BackgroundElevated else Color.Transparent,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.sm)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) NuvioTheme.colors.Secondary else NuvioTheme.colors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextSecondary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            if (channelCount != null && channelCount > 0) {
                Text(
                    text = channelCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NuvioTheme.colors.TextTertiary
                )
            }
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
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, NuvioTheme.colors.Border),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NuvioTheme.colors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.iptv_search_channels),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NuvioTheme.colors.TextTertiary
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = NuvioTheme.colors.TextPrimary
                    ),
                    cursorBrush = SolidColor(NuvioTheme.colors.Secondary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
