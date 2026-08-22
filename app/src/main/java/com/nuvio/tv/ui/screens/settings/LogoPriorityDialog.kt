package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

internal data class LogoSourceInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

internal val AVAILABLE_LOGO_SOURCES = listOf(
    LogoSourceInfo(
        id = "TV_LOGOS",
        title = "TV-Logos",
        description = "10,000+ sleek, transparent PNGs tailored for 10-ft TV dark UI",
        icon = Icons.Default.Tv
    ),
    LogoSourceInfo(
        id = "IPTV_ORG",
        title = "IPTV-ORG Catalog",
        description = "Global open-source vector (SVG) and high-res channel logos",
        icon = Icons.Default.Image
    ),
    LogoSourceInfo(
        id = "PROVIDER",
        title = "Provider Artwork",
        description = "Original channel logos supplied by your M3U / Xtream playlist",
        icon = Icons.Default.LiveTv
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun LogoPriorityDialog(
    priorityOrder: List<String>,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDismiss: () -> Unit,
    onReResolve: () -> Unit
) {
    val items = priorityOrder.mapNotNull { id ->
        AVAILABLE_LOGO_SOURCES.find { it.id.equals(id, ignoreCase = true) }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = "Logo Source Priority",
        subtitle = "Arrange the search order. Sources at the top will be used first for channel artwork.",
        width = 620.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, source ->
                val isFirst = index == 0
                val isLast = index == items.size - 1

                Card(
                    onClick = { /* No-op, row is informational */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                    colors = CardDefaults.colors(
                        containerColor = NuvioTheme.colors.BackgroundElevated,
                        focusedContainerColor = NuvioTheme.colors.BackgroundElevated
                    ),
                    border = CardDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(NuvioTheme.radii.md)
                        ),
                        focusedBorder = Border.None
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isFirst) NuvioTheme.colors.Primary.copy(alpha = 0.25f)
                                    else NuvioTheme.colors.SurfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isFirst) NuvioTheme.colors.Primary else NuvioTheme.colors.TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            imageVector = source.icon,
                            contentDescription = null,
                            tint = if (isFirst) NuvioTheme.colors.Primary else NuvioTheme.colors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NuvioTheme.colors.TextPrimary
                            )
                            Text(
                                text = source.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.TextTertiary,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Up button
                        if (!isFirst) {
                            ReorderButton(
                                icon = Icons.Default.ArrowUpward,
                                contentDescription = "Move ${source.title} up",
                                onClick = { onMoveUp(source.id) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Down button
                        if (!isLast) {
                            ReorderButton(
                                icon = Icons.Default.ArrowDownward,
                                contentDescription = "Move ${source.title} down",
                                onClick = { onMoveDown(source.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onReResolve()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioTheme.colors.Primary,
                        contentColor = Color.White,
                        focusedContainerColor = NuvioTheme.colors.PrimaryVariant,
                        focusedContentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Apply & Re-resolve Logos")
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioTheme.colors.BackgroundCard,
                        contentColor = NuvioTheme.colors.TextPrimary,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground,
                        focusedContentColor = NuvioTheme.colors.TextPrimary
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md))
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReorderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, NuvioTheme.colors.Border.copy(alpha = 0.4f)),
                shape = CircleShape
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = CircleShape
            )
        ),
        modifier = Modifier.size(34.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = NuvioTheme.colors.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
