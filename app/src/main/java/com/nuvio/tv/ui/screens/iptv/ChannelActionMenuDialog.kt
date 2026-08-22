package com.nuvio.tv.ui.screens.iptv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.ui.components.IptvChannelLogo
import com.nuvio.tv.ui.theme.NuvioTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelActionMenuDialog(
    channel: IptvChannel,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            onClick = {},
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(NuvioTheme.radii.lg)),
            shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.lg)),
            colors = CardDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundElevated
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IptvChannelLogo(
                        name = channel.name,
                        logoUrl = channel.logoUrl,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NuvioTheme.colors.TextPrimary,
                            maxLines = 1
                        )
                        if (!channel.groupTitle.isNullOrBlank()) {
                            Text(
                                text = channel.groupTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = NuvioTheme.colors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Actions
                ChannelActionItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Play Fullscreen",
                    subtitle = "Watch this channel immediately",
                    onClick = {
                        onDismiss()
                        onPlay()
                    }
                )

                Spacer(Modifier.height(8.dp))

                ChannelActionItem(
                    icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    title = if (channel.isFavorite) "Remove from Favourites" else "Add to Favourites",
                    subtitle = if (channel.isFavorite) "Channel is currently in favourites" else "Pin to your favourites row",
                    iconTint = if (channel.isFavorite) NuvioTheme.colors.Secondary else NuvioTheme.colors.TextPrimary,
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    }
                )

                Spacer(Modifier.height(8.dp))

                ChannelActionItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Stream URL",
                    subtitle = "Copy direct media stream URL to clipboard",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Stream URL", channel.streamUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Stream URL copied", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                if (!channel.tvgId.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NuvioTheme.colors.BackgroundCard, RoundedCornerShape(NuvioTheme.radii.sm))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NuvioTheme.colors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "EPG ID: ${channel.tvgId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NuvioTheme.colors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = NuvioTheme.colors.TextPrimary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.sm)),
        colors = CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.sm)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NuvioTheme.colors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = NuvioTheme.colors.TextSecondary
                )
            }
        }
    }
}
