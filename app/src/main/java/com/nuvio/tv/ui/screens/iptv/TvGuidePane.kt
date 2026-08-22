package com.nuvio.tv.ui.screens.iptv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.components.IptvChannelLogo
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GUIDE_MINUTES = 180
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
    onMoveWindow: (Int) -> Unit,
    onNow: () -> Unit,
    onSelectChannel: (IptvChannel) -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onToggleFavorite: (IptvChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusedProgram by remember { mutableStateOf<IptvEpgProgram?>(null) }
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val detailProgram = focusedProgram ?: currentProgram
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val guideWidth = (GUIDE_MINUTES * PIXELS_PER_MINUTE).dp

    Column(modifier = modifier.fillMaxHeight()) {
        GuideDetailHeader(
            channel = selectedChannel,
            program = detailProgram,
            nextProgram = nextProgram,
            onPlay = { selectedChannel?.let(onPlayChannel) },
            onFavorite = { selectedChannel?.let(onToggleFavorite) }
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("TV GUIDE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NuvioTheme.colors.TextPrimary)
            Spacer(Modifier.weight(1f))
            GuideControl(Icons.Default.ChevronLeft, "Earlier") { onMoveWindow(-90) }
            Surface(
                onClick = onNow,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.full)),
                colors = ClickableSurfaceDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, focusedContainerColor = NuvioTheme.colors.FocusBackground),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) { Text("NOW", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NuvioTheme.colors.Secondary) }
            GuideControl(Icons.Default.ChevronRight, "Later") { onMoveWindow(90) }
            if (isRefreshing) { Spacer(Modifier.width(10.dp)); LoadingIndicator(Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Box(Modifier.width(220.dp).fillMaxHeight().padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text(timeFormat.format(Date(windowStart)), style = MaterialTheme.typography.labelMedium, color = NuvioTheme.colors.TextSecondary)
            }
            GuideTimeHeader(windowStart, guideWidth)
        }
        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No channels in this category", color = NuvioTheme.colors.TextSecondary) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(channels, key = { it.id }) { channel ->
                    GuideRow(
                        channel = channel, programs = schedules[channel.id].orEmpty(), windowStart = windowStart,
                        guideWidth = guideWidth, now = now, selected = selectedChannel?.id == channel.id,
                        onChannel = { onSelectChannel(channel) },
                        onProgram = { program -> focusedProgram = program; onSelectChannel(channel) },
                        onPlay = { onPlayChannel(channel) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable private fun GuideDetailHeader(channel: IptvChannel?, program: IptvEpgProgram?, nextProgram: IptvEpgProgram?, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(NuvioTheme.radii.lg)).background(NuvioTheme.colors.BackgroundElevated).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(118.dp).clip(RoundedCornerShape(NuvioTheme.radii.md)).background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
            if (!program?.posterUrl.isNullOrBlank()) AsyncImage(program?.posterUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (channel != null) IptvChannelLogo(channel.name, channel.logoUrl, Modifier.size(82.dp))
            else Icon(Icons.Default.LiveTv, null, tint = NuvioTheme.colors.Secondary, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(channel?.name ?: "Select a channel", style = MaterialTheme.typography.labelLarge, color = NuvioTheme.colors.Secondary)
            Text(program?.title ?: "No guide information", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, color = NuvioTheme.colors.TextPrimary)
            if (program != null) {
                val f = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                Text("${f.format(Date(program.startEpochMs))} – ${f.format(Date(program.endEpochMs))}", style = MaterialTheme.typography.bodyMedium, color = NuvioTheme.colors.TextSecondary)
                if (!program.description.isNullOrBlank()) Text(program.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = NuvioTheme.colors.TextSecondary)
            } else Text(nextProgram?.title ?: "Add an XMLTV source in IPTV settings to see programme schedules.", style = MaterialTheme.typography.bodySmall, color = NuvioTheme.colors.TextSecondary)
        }
        GuideControl(Icons.Default.PlayArrow, "Play", onPlay)
        Spacer(Modifier.width(8.dp))
        GuideControl(if (channel?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", onFavorite)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable private fun GuideControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = ClickableSurfaceDefaults.shape(CircleShape), colors = ClickableSurfaceDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard, focusedContainerColor = NuvioTheme.colors.FocusBackground), border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = CircleShape)), modifier = Modifier.size(38.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, label, tint = NuvioTheme.colors.TextPrimary, modifier = Modifier.size(20.dp)) }
    }
}

@Composable private fun GuideTimeHeader(start: Long, width: androidx.compose.ui.unit.Dp) {
    val formatter = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    Row(Modifier.width(width).fillMaxHeight()) {
        repeat(7) { index -> Box(Modifier.width(90.dp).fillMaxHeight().border(BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = .25f))), contentAlignment = Alignment.CenterStart) { Text(formatter.format(Date(start + index * 30 * 60 * 1000L)), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = NuvioTheme.colors.TextTertiary) } }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable private fun GuideRow(channel: IptvChannel, programs: List<IptvEpgProgram>, windowStart: Long, guideWidth: androidx.compose.ui.unit.Dp, now: Long, selected: Boolean, onChannel: () -> Unit, onProgram: (IptvEpgProgram) -> Unit, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(76.dp)) {
        Card(onClick = { onChannel(); onPlay() }, modifier = Modifier.width(220.dp).fillMaxHeight(), shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)), colors = CardDefaults.colors(containerColor = if (selected) NuvioTheme.colors.BackgroundElevated else NuvioTheme.colors.BackgroundCard), border = CardDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = RoundedCornerShape(NuvioTheme.radii.md)))) {
            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(channel.channelNumber?.toString() ?: "•", modifier = Modifier.width(28.dp), color = NuvioTheme.colors.TextTertiary, style = MaterialTheme.typography.labelMedium)
                IptvChannelLogo(channel.name, channel.logoUrl, Modifier.size(38.dp))
                Spacer(Modifier.width(9.dp)); Text(channel.name, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), color = NuvioTheme.colors.TextPrimary)
            }
        }
        Box(Modifier.width(guideWidth).fillMaxHeight().clip(RoundedCornerShape(NuvioTheme.radii.md)).background(NuvioTheme.colors.BackgroundCard.copy(alpha = .5f))) {
            if (programs.isEmpty()) Text("No guide data", modifier = Modifier.padding(14.dp), color = NuvioTheme.colors.TextTertiary, style = MaterialTheme.typography.bodySmall)
            programs.forEach { program ->
                val offsetMinutes = ((program.startEpochMs - windowStart) / 60_000L).coerceAtLeast(0).toInt()
                val visibleEnd = minOf(program.endEpochMs, windowStart + GUIDE_MINUTES * 60_000L)
                val visibleStart = maxOf(program.startEpochMs, windowStart)
                val durationMinutes = ((visibleEnd - visibleStart) / 60_000L).coerceAtLeast(15).toInt()
                Card(onClick = { onProgram(program) }, modifier = Modifier.offset(x = (offsetMinutes * PIXELS_PER_MINUTE).dp).width((durationMinutes * PIXELS_PER_MINUTE).dp).fillMaxHeight().padding(horizontal = 2.dp), shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.sm)), colors = CardDefaults.colors(containerColor = if (now in program.startEpochMs..program.endEpochMs) NuvioTheme.colors.Secondary.copy(alpha = .22f) else NuvioTheme.colors.BackgroundElevated), border = CardDefaults.border(focusedBorder = Border(border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs), shape = RoundedCornerShape(NuvioTheme.radii.sm)))) {
                    Column(Modifier.padding(10.dp)) { Text(program.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = NuvioTheme.colors.TextPrimary); Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(program.startEpochMs)), style = MaterialTheme.typography.labelSmall, color = NuvioTheme.colors.TextSecondary) }
                }
            }
            val marker = ((now - windowStart).toFloat() / (GUIDE_MINUTES * 60_000f)).coerceIn(0f, 1f)
            if (marker in 0f..1f && now in windowStart..(windowStart + GUIDE_MINUTES * 60_000L)) Box(Modifier.offset(x = (guideWidth.value * marker).dp).fillMaxHeight().width(2.dp).background(NuvioTheme.colors.Secondary).align(Alignment.CenterStart))
        }
    }
}
