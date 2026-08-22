package com.nuvio.tv.ui.screens.iptv.playlist

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.iptv.IptvPlaylist
import com.nuvio.tv.domain.model.iptv.IptvPlaylistType
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme
import java.util.UUID

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddEditPlaylistDialog(
    initialPlaylist: IptvPlaylist? = null,
    initialCredentials: XtreamCredentials? = null,
    onDismiss: () -> Unit,
    onSave: (IptvPlaylist, XtreamCredentials?) -> Unit
) {
    var selectedTab by remember {
        mutableIntStateOf(if (initialPlaylist?.type == IptvPlaylistType.XTREAM) 1 else 0)
    }

    var name by remember { mutableStateOf(initialPlaylist?.name ?: "") }
    var m3uUrl by remember { mutableStateOf(if (initialPlaylist?.type == IptvPlaylistType.M3U) initialPlaylist.sourceLocation else "") }
    var epgUrl by remember { mutableStateOf(initialPlaylist?.epgUrl ?: "") }

    var xcServer by remember { mutableStateOf(if (initialPlaylist?.type == IptvPlaylistType.XTREAM) initialPlaylist.sourceLocation else "") }
    var xcUser by remember { mutableStateOf(initialCredentials?.username ?: "") }
    var xcPass by remember { mutableStateOf(initialCredentials?.password ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    NuvioDialog(
        onDismiss = onDismiss,
        title = if (initialPlaylist == null) stringResource(R.string.iptv_add_playlist) else stringResource(R.string.iptv_edit_playlist),
        subtitle = "Connect your M3U playlist stream URL or Xtream Codes login",
        width = 580.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Pill Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NuvioTheme.radii.full))
                    .background(NuvioTheme.colors.BackgroundCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selectedTab == 0) NuvioTheme.colors.Secondary.copy(alpha = 0.25f) else Color.Transparent,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.full)),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, if (selectedTab == 0) NuvioTheme.colors.Secondary else Color.Transparent),
                            shape = RoundedCornerShape(NuvioTheme.radii.full)
                        ),
                        focusedBorder = Border(
                            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                            shape = RoundedCornerShape(NuvioTheme.radii.full)
                        )
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.iptv_type_m3u),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selectedTab == 0) Color.White else NuvioTheme.colors.TextSecondary
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selectedTab == 1) NuvioTheme.colors.Secondary.copy(alpha = 0.25f) else Color.Transparent,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.full)),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, if (selectedTab == 1) NuvioTheme.colors.Secondary else Color.Transparent),
                            shape = RoundedCornerShape(NuvioTheme.radii.full)
                        ),
                        focusedBorder = Border(
                            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                            shape = RoundedCornerShape(NuvioTheme.radii.full)
                        )
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.iptv_type_xtream),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selectedTab == 1) Color.White else NuvioTheme.colors.TextSecondary
                        )
                    }
                }
            }

            // Playlist Name Input
            NuvioInputField(
                label = stringResource(R.string.iptv_playlist_name),
                value = name,
                placeholder = "My IPTV Playlist",
                onValueChange = { name = it }
            )

            if (selectedTab == 0) {
                // M3U URL
                NuvioInputField(
                    label = stringResource(R.string.iptv_m3u_url),
                    value = m3uUrl,
                    placeholder = "https://example.com/playlist.m3u8",
                    keyboardType = KeyboardType.Uri,
                    onValueChange = { m3uUrl = it }
                )

                // EPG URL (Optional)
                NuvioInputField(
                    label = stringResource(R.string.iptv_epg_url),
                    value = epgUrl,
                    placeholder = "https://example.com/epg.xml.gz",
                    keyboardType = KeyboardType.Uri,
                    onValueChange = { epgUrl = it }
                )
            } else {
                // Xtream Codes Server
                NuvioInputField(
                    label = stringResource(R.string.iptv_xtream_server),
                    value = xcServer,
                    placeholder = "http://example.com:8080",
                    keyboardType = KeyboardType.Uri,
                    onValueChange = { xcServer = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NuvioInputField(
                            label = stringResource(R.string.iptv_xtream_user),
                            value = xcUser,
                            placeholder = "Username",
                            onValueChange = { xcUser = it }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NuvioInputField(
                            label = stringResource(R.string.iptv_xtream_pass),
                            value = xcPass,
                            placeholder = "Password",
                            isPassword = true,
                            onValueChange = { xcPass = it }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.Error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioTheme.colors.BackgroundCard,
                        focusedContainerColor = NuvioTheme.colors.FocusBackground
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                    border = ButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, NuvioTheme.colors.Border),
                            shape = RoundedCornerShape(NuvioTheme.radii.md)
                        ),
                        focusedBorder = Border(
                            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                            shape = RoundedCornerShape(NuvioTheme.radii.md)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.iptv_cancel),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = NuvioTheme.colors.TextSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Please enter a playlist name"
                            return@Button
                        }
                        if (selectedTab == 0) {
                            if (m3uUrl.isBlank()) {
                                errorMessage = "Please enter an M3U playlist URL"
                                return@Button
                            }
                            val playlist = (initialPlaylist ?: IptvPlaylist(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = IptvPlaylistType.M3U,
                                sourceLocation = m3uUrl
                            )).copy(
                                name = name.trim(),
                                type = IptvPlaylistType.M3U,
                                sourceLocation = m3uUrl.trim(),
                                epgUrl = epgUrl.trim().ifBlank { null }
                            )
                            onSave(playlist, null)
                        } else {
                            if (xcServer.isBlank() || xcUser.isBlank() || xcPass.isBlank()) {
                                errorMessage = "Please complete all Xtream server, username, and password fields"
                                return@Button
                            }
                            val playlist = (initialPlaylist ?: IptvPlaylist(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = IptvPlaylistType.XTREAM,
                                sourceLocation = xcServer
                            )).copy(
                                name = name.trim(),
                                type = IptvPlaylistType.XTREAM,
                                sourceLocation = xcServer.trim()
                            )
                            val creds = XtreamCredentials(
                                serverUrl = xcServer.trim(),
                                username = xcUser.trim(),
                                password = xcPass.trim()
                            )
                            onSave(playlist, creds)
                        }
                    },
                    modifier = Modifier.weight(1.3f),
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (initialPlaylist == null) stringResource(R.string.iptv_add_playlist) else stringResource(R.string.iptv_save),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NuvioTheme.colors.OnSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioInputField(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = NuvioTheme.colors.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            onClick = { isEditing = true },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NuvioTheme.radii.md)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = NuvioTheme.colors.BackgroundElevated,
                focusedContainerColor = NuvioTheme.colors.BackgroundElevated
            ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
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
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (!it.isFocused && isEditing) {
                            isEditing = false
                            keyboardController?.hide()
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        isEditing = false
                        keyboardController?.hide()
                    },
                    onDone = {
                        isEditing = false
                        keyboardController?.hide()
                    }
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = NuvioTheme.colors.TextPrimary
                ),
                cursorBrush = SolidColor(if (isEditing) NuvioTheme.colors.Secondary else Color.Transparent),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NuvioTheme.colors.TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
