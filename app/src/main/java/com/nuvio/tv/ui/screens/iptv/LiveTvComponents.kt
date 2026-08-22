package com.nuvio.tv.ui.screens.iptv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.iptv.IptvChannel
import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import com.nuvio.tv.ui.theme.NuvioTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelRowItem(
    channel: IptvChannel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    currentProgramTitle: String? = null
) {
    Card(
        onClick = {
            onSelect()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) NuvioTheme.colors.BackgroundElevated else NuvioTheme.colors.BackgroundCard.copy(alpha = 0.6f),
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NuvioTheme.colors.Secondary.copy(alpha = 0.6f) else NuvioTheme.colors.Border.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.015f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number
            if (channel.channelNumber != null && channel.channelNumber > 0) {
                Text(
                    text = channel.channelNumber.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = NuvioTheme.colors.TextTertiary,
                    modifier = Modifier.width(32.dp)
                )
            }

            // Channel Logo
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(NuvioTheme.radii.sm))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.3f), RoundedCornerShape(NuvioTheme.radii.sm)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = NuvioTheme.colors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Channel Name & Live Program
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextPrimary.copy(alpha = 0.9f)
                )

                if (!currentProgramTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentProgramTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = NuvioTheme.colors.Secondary
                    )
                }
            }

            // Favorite Icon
            Surface(
                onClick = onToggleFavorite,
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
                modifier = Modifier.size(32.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (channel.isFavorite) Color(0xFFFF4081) else NuvioTheme.colors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GroupNavItem(
    title: String,
    icon: ImageVector? = null,
    channelCount: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) NuvioTheme.colors.BackgroundElevated else Color.Transparent,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NuvioTheme.colors.Secondary.copy(alpha = 0.5f) else Color.Transparent
                ),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.01f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) NuvioTheme.colors.TextPrimary else NuvioTheme.colors.TextSecondary
                )
            }

            if (channelCount != null && channelCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NuvioTheme.radii.full))
                        .background(NuvioTheme.colors.BackgroundCard)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = channelCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = NuvioTheme.colors.TextTertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelDetailPanel(
    channel: IptvChannel,
    currentProgram: IptvEpgProgram?,
    nextProgram: IptvEpgProgram?,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(NuvioTheme.radii.xl))
            .background(NuvioTheme.colors.BackgroundElevated)
            .border(1.dp, NuvioTheme.colors.Border, RoundedCornerShape(NuvioTheme.radii.xl))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with Channel Logo & Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(NuvioTheme.radii.md))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.4f), RoundedCornerShape(NuvioTheme.radii.md)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .size(58.dp)
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = NuvioTheme.colors.Secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NuvioTheme.colors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NuvioTheme.radii.xs))
                            .background(NuvioTheme.colors.BackgroundCard)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = channel.groupTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = NuvioTheme.colors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Program Guide Section
            if (currentProgram != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE50914))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = NuvioTheme.colors.Secondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentProgram.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NuvioTheme.colors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val startStr = timeFormat.format(Date(currentProgram.startEpochMs))
                val endStr = timeFormat.format(Date(currentProgram.endEpochMs))
                Text(
                    text = "$startStr – $endStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NuvioTheme.colors.BackgroundCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(currentProgram.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(NuvioTheme.colors.Secondary)
                    )
                }

                if (!currentProgram.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentProgram.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.colors.TextSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NuvioTheme.radii.md))
                        .background(NuvioTheme.colors.BackgroundCard)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Live Stream",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NuvioTheme.colors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No electronic program guide available for this channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = NuvioTheme.colors.TextSecondary
                        )
                    }
                }
            }

            // Next Program Preview
            if (nextProgram != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "UP NEXT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = NuvioTheme.colors.TextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nextProgram.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = NuvioTheme.colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val nextStartStr = timeFormat.format(Date(nextProgram.startEpochMs))
                val nextEndStr = timeFormat.format(Date(nextProgram.endEpochMs))
                Text(
                    text = "$nextStartStr – $nextEndStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Primary Watch Button
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.Secondary,
                    focusedContainerColor = NuvioTheme.colors.SecondaryVariant
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch",
                        tint = NuvioTheme.colors.OnSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Channel",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = NuvioTheme.colors.OnSecondary
                    )
                }
            }
        }
    }
}
