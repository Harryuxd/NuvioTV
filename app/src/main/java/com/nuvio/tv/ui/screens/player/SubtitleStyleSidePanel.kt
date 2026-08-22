@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.data.local.SubtitleStyleSettings
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.R

private val PANEL_TEXT_COLORS = listOf(
    Color.White,
    Color(0xFFD9D9D9),
    Color(0xFFFFD700),
    Color(0xFF00E5FF),
    Color(0xFFFF5C5C),
    Color(0xFF00FF88)
)

private val PANEL_CONTAINER_COLORS = listOf(
    Color.Black,
    Color(0xFF1A1A1A),
    Color(0xFF1E293B),
    Color(0xFF0F172A),
    Color.Transparent
)

private val PANEL_OUTLINE_COLORS = listOf(
    Color.Black,
    Color.White,
    Color(0xFF00E5FF),
    Color(0xFFFF5C5C)
)

private val StyleCardWidth = 220.dp
private val StyleCardHeight = 110.dp
private val StyleCardGap = NuvioTheme.spacing.md
private val StyleGridWidth = (StyleCardWidth * 4) + (StyleCardGap * 3)

@Composable
internal fun SubtitleStyleSidePanel(
    subtitleStyle: SubtitleStyleSettings,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = modifier
            .width(960.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(Color(0xFF101010))
            .padding(start = NuvioTheme.spacing.lg, end = NuvioTheme.spacing.lg, top = 20.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier.width(StyleGridWidth),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.subtitle_style_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StyleCardGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top
        ) {
            // Column 1: Size & Bold
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_font_size),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow {
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Remove,
                            onClick = { onEvent(PlayerEvent.OnSetSubtitleSize(subtitleStyle.size - 10)) },
                            modifier = Modifier.focusRequester(firstItemFocusRequester)
                        )
                        SubtitleStyleValueDisplay(text = "${subtitleStyle.size}%")
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Add,
                            onClick = { onEvent(PlayerEvent.OnSetSubtitleSize(subtitleStyle.size + 10)) }
                        )
                    }
                }
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_bold),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow(label = stringResource(R.string.subtitle_style_weight)) {
                        SubtitleStyleToggleButton(
                            isEnabled = subtitleStyle.bold,
                            onClick = { onEvent(PlayerEvent.OnSetSubtitleBold(!subtitleStyle.bold)) }
                        )
                    }
                }
            }

            // Column 2: Text Color & Opacity
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_text_color),
                    centerContent = false,
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    val currentAlphaPercent = (Color(subtitleStyle.textColor).alpha * 100f).roundToInt().coerceIn(0, 100)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)) {
                            PANEL_TEXT_COLORS.forEach { color ->
                                SubtitleStyleColorChip(
                                    color = color,
                                    isSelected = Color(subtitleStyle.textColor).copy(alpha = 1f).toArgb() == color.copy(alpha = 1f).toArgb(),
                                    onClick = {
                                        val currentAlpha = Color(subtitleStyle.textColor).alpha
                                        onEvent(PlayerEvent.OnSetSubtitleTextColor(color.copy(alpha = currentAlpha).toArgb()))
                                    }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Remove,
                                onClick = {
                                    val newAlpha = (currentAlphaPercent - 10).coerceAtLeast(0) / 100f
                                    onEvent(PlayerEvent.OnSetSubtitleTextColor(Color(subtitleStyle.textColor).copy(alpha = newAlpha).toArgb()))
                                }
                            )
                            SubtitleStyleValueDisplay(text = "$currentAlphaPercent%")
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Add,
                                onClick = {
                                    val newAlpha = (currentAlphaPercent + 10).coerceAtMost(100) / 100f
                                    onEvent(PlayerEvent.OnSetSubtitleTextColor(Color(subtitleStyle.textColor).copy(alpha = newAlpha).toArgb()))
                                }
                            )
                        }
                    }
                }

                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_bottom_offset),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow {
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Remove,
                            onClick = { onEvent(PlayerEvent.OnSetSubtitleVerticalOffset(subtitleStyle.verticalOffset - 5)) }
                        )
                        SubtitleStyleValueDisplay(text = subtitleStyle.verticalOffset.toString())
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Add,
                            onClick = { onEvent(PlayerEvent.OnSetSubtitleVerticalOffset(subtitleStyle.verticalOffset + 5)) }
                        )
                    }
                }
            }

            // Column 3: Container Fill Color & Container Opacity
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = "Container Fill & Opacity",
                    centerContent = false,
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    val currentBg = Color(subtitleStyle.backgroundColor)
                    val currentBgAlpha = currentBg.alpha
                    val currentBgAlphaPercent = (currentBgAlpha * 100f).roundToInt().coerceIn(0, 100)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)) {
                            PANEL_CONTAINER_COLORS.forEach { color ->
                                val isSelected = if (color == Color.Transparent) {
                                    currentBg.alpha == 0f
                                } else {
                                    currentBg.alpha > 0f && (color.toArgb() and 0x00FFFFFF == currentBg.toArgb() and 0x00FFFFFF)
                                }
                                SubtitleStyleColorChip(
                                    color = if (color == Color.Transparent) Color(0x33FFFFFF) else color,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (color == Color.Transparent) {
                                            onEvent(PlayerEvent.OnSetSubtitleBackgroundColor(Color.Transparent.toArgb()))
                                        } else {
                                            val alphaToUse = if (currentBgAlpha <= 0.05f) 0.8f else currentBgAlpha
                                            onEvent(PlayerEvent.OnSetSubtitleBackgroundColor(color.copy(alpha = alphaToUse).toArgb()))
                                        }
                                    }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Remove,
                                onClick = {
                                    val newAlpha = (currentBgAlphaPercent - 10).coerceAtLeast(0) / 100f
                                    val base = if (currentBg == Color.Transparent || currentBg.alpha == 0f) Color.Black else currentBg
                                    onEvent(PlayerEvent.OnSetSubtitleBackgroundColor(base.copy(alpha = newAlpha).toArgb()))
                                }
                            )
                            SubtitleStyleValueDisplay(text = "$currentBgAlphaPercent%")
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Add,
                                onClick = {
                                    val newAlpha = (currentBgAlphaPercent + 10).coerceAtMost(100) / 100f
                                    val base = if (currentBg == Color.Transparent || currentBg.alpha == 0f) Color.Black else currentBg
                                    onEvent(PlayerEvent.OnSetSubtitleBackgroundColor(base.copy(alpha = newAlpha).toArgb()))
                                }
                            )
                        }
                    }
                }

                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_defaults),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    Card(
                        onClick = { onEvent(PlayerEvent.OnResetSubtitleDefaults) },
                        colors = CardDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md))
                    ) {
                        Text(
                            text = stringResource(R.string.subtitle_style_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = NuvioTheme.spacing.lg, vertical = 10.dp)
                        )
                    }
                }
            }

            // Column 4: Outline
            Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_outline),
                    centerContent = false,
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubtitleStyleToggleButton(
                                isEnabled = subtitleStyle.outlineEnabled,
                                onClick = { onEvent(PlayerEvent.OnSetSubtitleOutlineEnabled(!subtitleStyle.outlineEnabled)) }
                            )
                            Text(
                                text = stringResource(R.string.subtitle_style_color),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                            PANEL_OUTLINE_COLORS.forEach { color ->
                                SubtitleStyleColorChip(
                                    color = color.copy(alpha = if (subtitleStyle.outlineEnabled) 1f else 0.35f),
                                    isSelected = subtitleStyle.outlineColor == color.toArgb(),
                                    onClick = {
                                        if (!subtitleStyle.outlineEnabled) {
                                            onEvent(PlayerEvent.OnSetSubtitleOutlineEnabled(true))
                                        }
                                        onEvent(PlayerEvent.OnSetSubtitleOutlineColor(color.toArgb()))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleStyleSection(
    title: String,
    centerContent: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(NuvioTheme.spacing.md)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .then(if (centerContent) Modifier.align(Alignment.Center) else Modifier.align(Alignment.TopStart)),
            contentAlignment = if (centerContent) Alignment.Center else Alignment.TopStart
        ) {
            content()
        }
    }
}

@Composable
private fun SubtitleStyleSettingRow(
    label: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        content()
    }
}

@Composable
private fun SubtitleStyleStepperButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.colors(
            containerColor = if (isFocused) NuvioTheme.colors.FocusBackground else Color.White.copy(alpha = 0.1f),
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        modifier = modifier
            .size(28.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SubtitleStyleValueDisplay(text: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 40.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun SubtitleStyleToggleButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isEnabled) {
                if (isFocused) NuvioTheme.colors.FocusBackground else NuvioTheme.colors.Secondary
            } else {
                if (isFocused) NuvioTheme.colors.FocusBackground else Color.White.copy(alpha = 0.1f)
            },
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        modifier = Modifier
            .height(28.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnabled) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (isFocused) {
                    Color.Black
                } else if (isEnabled) {
                    NuvioTheme.colors.OnSecondary
                } else {
                    Color.White.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@Composable
private fun SubtitleStyleColorChip(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = color,
            focusedContainerColor = color
        ),
        shape = CardDefaults.shape(CircleShape),
        modifier = Modifier
            .size(24.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, Color.White, CircleShape)
                } else if (isSelected) {
                    Modifier.border(2.dp, NuvioTheme.colors.Secondary, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                }
            )
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (color == Color.White || color == Color(0xFFD9D9D9) || color == Color(0xFFFFD700)) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
