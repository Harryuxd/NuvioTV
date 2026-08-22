@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.ui.components.LoadingIndicator
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun IptvAlternativesSidePanel(
    uiState: PlayerUiState,
    focusRequester: FocusRequester,
    onClose: () -> Unit,
    onReload: () -> Unit,
    onChannelSelected: (IptvChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.isLoadingIptvAlternatives, uiState.iptvAlternativeChannels) {
        if (!uiState.isLoadingIptvAlternatives && uiState.iptvAlternativeChannels.isNotEmpty()) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    Box(
        modifier = modifier.fillMaxHeight().width(520.dp)
            .clip(RoundedCornerShape(topStart = NuvioTheme.spacing.lg, bottomStart = NuvioTheme.spacing.lg))
            .background(NuvioTheme.colors.BackgroundElevated)
    ) {
        Column(Modifier.padding(NuvioTheme.spacing.xl)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Alternative channels", style = MaterialTheme.typography.headlineSmall, color = NuvioTheme.colors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                    DialogButton(text = "Refresh", onClick = onReload, isPrimary = false)
                    DialogButton(text = "Close", onClick = onClose, isPrimary = false)
                }
            }
            Spacer(Modifier.height(NuvioTheme.spacing.xs))
            Text(uiState.title, style = MaterialTheme.typography.bodyLarge, color = NuvioTheme.colors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(NuvioTheme.spacing.lg))
            when {
                uiState.isLoadingIptvAlternatives -> Box(Modifier.fillMaxWidth().padding(vertical = NuvioTheme.spacing.xl), contentAlignment = Alignment.Center) { LoadingIndicator() }
                uiState.iptvAlternativesError != null -> Text(uiState.iptvAlternativesError, color = NuvioTheme.colors.Error)
                uiState.iptvAlternativeChannels.size <= 1 -> Text("No alternative channels found", style = MaterialTheme.typography.bodyLarge, color = NuvioTheme.colors.TextSecondary)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm), contentPadding = PaddingValues(NuvioTheme.spacing.sm)) {
                    items(uiState.iptvAlternativeChannels, key = { it.id }) { channel ->
                        IptvAlternativeItem(
                            channel = channel,
                            isCurrent = channel.id == uiState.currentIptvChannelId,
                            focusRequester = focusRequester,
                            requestInitialFocus = channel.id == uiState.currentIptvChannelId,
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IptvAlternativeItem(
    channel: IptvChannel,
    isCurrent: Boolean,
    focusRequester: FocusRequester,
    requestInitialFocus: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().then(if (requestInitialFocus) Modifier.focusRequester(focusRequester) else Modifier),
        colors = CardDefaults.colors(containerColor = NuvioTheme.colors.BackgroundElevated, focusedContainerColor = NuvioTheme.colors.BackgroundElevated),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(NuvioTheme.spacing.hairline, if (isCurrent) NuvioTheme.colors.Primary.copy(alpha = 0.7f) else Color.Transparent),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(NuvioTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)) {
            AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f)) {
                Text(channel.name, style = MaterialTheme.typography.titleMedium, color = NuvioTheme.colors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.channelNumber?.let { "Channel $it" } ?: channel.groupTitle, style = MaterialTheme.typography.bodySmall, color = NuvioTheme.colors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isCurrent) Text("Playing", style = MaterialTheme.typography.labelMedium, color = NuvioTheme.colors.Primary)
        }
    }
}
