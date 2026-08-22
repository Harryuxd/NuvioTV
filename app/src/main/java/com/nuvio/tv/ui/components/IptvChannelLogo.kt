package com.nuvio.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.ui.theme.NuvioTheme

/** A consistent, legible fallback when no high-quality curated channel logo exists. */
@Composable
fun IptvChannelLogo(name: String, logoUrl: String?, modifier: Modifier = Modifier) {
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(model = logoUrl, contentDescription = name, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(
            modifier = modifier.clip(CircleShape).background(NuvioTheme.colors.BackgroundElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().split(Regex("\\s+")).take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "TV" },
                style = MaterialTheme.typography.labelSmall,
                color = NuvioTheme.colors.Secondary
            )
        }
    }
}
