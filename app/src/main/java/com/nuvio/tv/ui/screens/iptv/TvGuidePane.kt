package com.nuvio.tv.ui.screens.iptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.IptvChannelLogo
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GUIDE_MINUTES = 24 * 60 // 24 hours of timeline
private const val PIXELS_PER_MINUTE = 3

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun TvGuidePane(
    channels: List<IptvChannel>,
    schedules: Map<String, List<IptvEpgProgram>>,
    windowStart: Long,
    selectedChannel: IptvChannel?,
    currentProgram: IptvEpgProgram?,
    nextProgram: IptvEpgProgram?,
    isRefreshing: Boolean,
    previewAudioEnabled: Boolean,
    onMoveWindow: (Int) -> Unit,
    onNow: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onLongClickChannel: (IptvChannel) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    var focusedProgram by remember { mutableStateOf<IptvEpgProgram?>(null) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val detailProgram = focusedProgram ?: currentProgram
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val guideWidth = (GUIDE_MINUTES * PIXELS_PER_MINUTE).dp
    val horizontalScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxHeight()) {
        GuideDetailHeader(
            channel = selectedChannel,
            program = detailProgram,
            nextProgram = nextProgram,
            previewAudioEnabled = previewAudioEnabled,
            onPlay = { selectedChannel?.let(onPlayChannel) }
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("TV GUIDE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NuvioTheme.colors.TextPrimary)
            Spacer(Modifier.weight(1f))
            GuideControl(Icons.Default.ChevronLeft, "Earlier") {
                scope.launch {
                    val target = (horizontalScrollState.value - 90 * PIXELS_PER_MINUTE * 2).coerceAtLeast(0)
                    horizontalScrollState.animateScrollTo(target)
                }
            }
            Surface(
                onClick = {
                    onNow()
                    scope.launch {
                        val minutesFromStart = ((now - windowStart) / 60_000f).coerceAtLeast(0f)
                        val targetPx = (minutesFromStart * PIXELS_PER_MINUTE * 2.5f).toInt()
                        horizontalScrollState.animateScrollTo(targetPx)
                    }
                },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.full)),
                colors = ClickableSurfaceDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, focusedContainerColor = NuvioTheme.colors.FocusBackground),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { Text("NOW", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NuvioTheme.colors.Secondary) }
            GuideControl(Icons.Default.ChevronRight, "Later") {
                scope.launch {
                    val target = (horizontalScrollState.value + 90 * PIXELS_PER_MINUTE * 2).coerceAtMost(horizontalScrollState.maxValue)
                    horizontalScrollState.animateScrollTo(target)
                }
            }
            if (isRefreshing) { Spacer(Modifier.width(10.dp)); LoadingIndicator(Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(8.dp))

        // Guide Grid Header
        Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Box(Modifier.width(200.dp).fillMaxHeight().padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text(timeFormat.format(Date(windowStart)), style = MaterialTheme.typography.labelMedium, color = NuvioTheme.colors.TextSecondary)
            }
            Box(Modifier.weight(1f).fillMaxHeight().horizontalScroll(horizontalScrollState)) {
                GuideTimeHeader(windowStart, guideWidth)
            }
        }

        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No channels in this category", color = NuvioTheme.colors.TextSecondary) }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    val channelPrograms = remember(channel.id, schedules) {
                        val tvgIdLower = channel.tvgId?.trim()?.lowercase()
                        val tvgBase = tvgIdLower?.substringBeforeLast('.')
                        val tvgNameLower = channel.tvgName?.trim()?.lowercase()
                        val nameLower = channel.name.trim().lowercase()
                        val normName = com.nuvio.tv.data.iptv.epg.XmltvParser.normalize(channel.name)

                        schedules[tvgIdLower]
                            ?: (if (tvgBase != null) schedules[tvgBase] else null)
                            ?: (if (tvgNameLower != null) schedules[tvgNameLower] else null)
                            ?: schedules[nameLower]
                            ?: schedules[normName]
                            ?: schedules[channel.id]
                            ?: emptyList()
                    }
                    GuideRow(
                        channel = channel,
                        programs = channelPrograms,
                        windowStart = windowStart,
                        guideWidth = guideWidth,
                        now = now,
                        selected = selectedChannel?.id == channel.id,
                        horizontalScrollState = horizontalScrollState,
                        onChannelClick = {
                            if (selectedChannel?.id == channel.id) {
                                onPlayChannel(channel)
                            } else {
                                onSelectChannel(channel)
                            }
                        },
                        onChannelLongClick = { onLongClickChannel(channel) },
                        onProgram = { program -> focusedProgram = program; onSelectChannel(channel) }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveTvPreviewPlayer(
    channel: IptvChannel?,
    previewAudioEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(channel?.id, channel?.streamUrl, previewAudioEnabled) {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        isPlaying = false
        val streamUrl = channel?.streamUrl
        if (streamUrl.isNullOrBlank()) return@LaunchedEffect

        delay(700) // Debounce fast scrolling

        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            volume = if (previewAudioEnabled) 1f else 0f
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
        exoPlayer = player
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

    Card(
        onClick = {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
            onClick()
        },
        modifier = modifier
            .width(230.dp)
            .height(130.dp),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        colors = CardDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.55f)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (exoPlayer != null && isPlaying) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (channel != null) {
                    IptvChannelLogo(channel.name, channel.logoUrl, Modifier.size(54.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = NuvioTheme.colors.Secondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                if (channel != null && !channel.streamUrl.isNullOrBlank()) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                        LoadingIndicator(Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideDetailHeader(
    channel: IptvChannel?,
    program: IptvEpgProgram?,
    nextProgram: IptvEpgProgram?,
    previewAudioEnabled: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(NuvioTheme.radii.lg))
            .background(NuvioTheme.colors.BackgroundElevated)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveTvPreviewPlayer(
            channel = channel,
            previewAudioEnabled = previewAudioEnabled,
            onClick = onPlay
        )

        Spacer(Modifier.width(18.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = channel?.name ?: "Select a channel",
                style = MaterialTheme.typography.labelLarge,
                color = NuvioTheme.colors.Secondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = program?.title ?: "No guide information",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = NuvioTheme.colors.TextPrimary
            )
            if (program != null) {
                val f = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                Text(
                    text = "${f.format(Date(program.startEpochMs))} – ${f.format(Date(program.endEpochMs))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary
                )
                if (!program.description.isNullOrBlank()) {
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = NuvioTheme.colors.TextSecondary
                    )
                }
            } else {
                Text(
                    text = nextProgram?.title ?: "Add an XMLTV source in IPTV settings to see programme schedules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = ClickableSurfaceDefaults.shape(CircleShape), colors = ClickableSurfaceDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard, focusedContainerColor = NuvioTheme.colors.FocusBackground), border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = CircleShape)), modifier = Modifier.size(38.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, label, tint = NuvioTheme.colors.TextPrimary, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun GuideTimeHeader(start: Long, width: androidx.compose.ui.unit.Dp) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Row(Modifier.width(width).fillMaxHeight()) {
        repeat(48) { index ->
            Box(Modifier.width(90.dp).fillMaxHeight().border(BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = .25f))), contentAlignment = Alignment.CenterStart) {
                Text(formatter.format(Date(start + index * 30 * 60 * 1000L)), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = NuvioTheme.colors.TextTertiary)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GuideRow(
    channel: IptvChannel,
    programs: List<IptvEpgProgram>,
    windowStart: Long,
    guideWidth: androidx.compose.ui.unit.Dp,
    now: Long,
    selected: Boolean,
    horizontalScrollState: ScrollState,
    onChannelClick: () -> Unit,
    onChannelLongClick: () -> Unit,
    onProgram: (IptvEpgProgram) -> Unit
) {
    val windowEnd = windowStart + GUIDE_MINUTES * 60_000L

    // Clean, sort, and deduplicate overlapping EPG intervals so cards never run into each other
    val cleanPrograms = remember(programs, windowStart) {
        val valid = programs.filter { it.endEpochMs > it.startEpochMs && it.endEpochMs > windowStart && it.startEpochMs < windowEnd }
            .sortedBy { it.startEpochMs }
        val nonOverlapping = mutableListOf<IptvEpgProgram>()
        var lastEnd = windowStart
        for (prog in valid) {
            if (prog.startEpochMs >= lastEnd || nonOverlapping.isEmpty()) {
                nonOverlapping.add(prog)
                lastEnd = prog.endEpochMs
            } else if (prog.endEpochMs > lastEnd && prog.startEpochMs >= (nonOverlapping.lastOrNull()?.startEpochMs ?: 0L) + 5 * 60_000L) {
                nonOverlapping.add(prog)
                lastEnd = prog.endEpochMs
            }
        }
        nonOverlapping
    }

    Row(Modifier.fillMaxWidth().height(52.dp)) {
        Card(
            onClick = onChannelClick,
            onLongClick = onChannelLongClick,
            modifier = Modifier.width(200.dp).fillMaxHeight(),
            shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.sm)),
            colors = CardDefaults.colors(containerColor = if (selected) NuvioTheme.colors.BackgroundElevated else NuvioTheme.colors.BackgroundCard),
            border = CardDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = RoundedCornerShape(NuvioTheme.radii.sm)))
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(channel.channelNumber?.toString() ?: "•", modifier = Modifier.width(22.dp), color = NuvioTheme.colors.TextTertiary, style = MaterialTheme.typography.labelSmall)
                IptvChannelLogo(channel.name, channel.logoUrl, Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text(channel.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = NuvioTheme.colors.TextPrimary)
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
        ) {
            Box(
                Modifier
                    .width(guideWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(NuvioTheme.radii.sm))
                    .background(NuvioTheme.colors.BackgroundCard.copy(alpha = .5f))
            ) {
                if (cleanPrograms.isEmpty()) {
                    Text("No guide data", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = NuvioTheme.colors.TextTertiary, style = MaterialTheme.typography.bodySmall)
                } else {
                    cleanPrograms.forEach { program ->
                        val visibleStart = maxOf(program.startEpochMs, windowStart)
                        val visibleEnd = minOf(program.endEpochMs, windowEnd)
                        val offsetMinutes = ((visibleStart - windowStart) / 60_000f).coerceAtLeast(0f)
                        val durationMinutes = ((visibleEnd - visibleStart) / 60_000f).coerceAtLeast(0f)
                        val itemWidth = (durationMinutes * PIXELS_PER_MINUTE).dp

                        if (itemWidth > 2.dp) {
                            Card(
                                onClick = { onProgram(program) },
                                modifier = Modifier
                                    .offset(x = (offsetMinutes * PIXELS_PER_MINUTE).dp)
                                    .width(itemWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 1.dp),
                                shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.xs)),
                                colors = CardDefaults.colors(containerColor = if (now in program.startEpochMs..program.endEpochMs) NuvioTheme.colors.Secondary.copy(alpha = .22f) else NuvioTheme.colors.BackgroundElevated),
                                border = CardDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = RoundedCornerShape(NuvioTheme.radii.xs)))
                            ) {
                                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.Center) {
                                    Text(program.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = NuvioTheme.colors.TextPrimary)
                                    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(program.startEpochMs)), style = MaterialTheme.typography.labelSmall, color = NuvioTheme.colors.TextSecondary)
                                }
                            }
                        }
                    }
                }
                val marker = ((now - windowStart).toFloat() / (GUIDE_MINUTES * 60_000f)).coerceIn(0f, 1f)
                if (marker in 0f..1f && now in windowStart..windowEnd) {
                    Box(Modifier.offset(x = (guideWidth.value * marker).dp).fillMaxHeight().width(2.dp).background(NuvioTheme.colors.Secondary).align(Alignment.CenterStart))
                }
            }
        }
    }
}
