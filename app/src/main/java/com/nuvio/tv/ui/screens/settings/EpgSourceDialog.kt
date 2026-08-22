package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.iptv.IptvEpgSource
import com.nuvio.tv.domain.model.iptv.IptvEpgSourceKind
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun EpgSourceDialog(
    source: IptvEpgSource? = null,
    onDismiss: () -> Unit,
    onSave: (IptvEpgSource) -> Unit,
    onRefresh: ((IptvEpgSource) -> Unit)? = null,
    onDelete: ((IptvEpgSource) -> Unit)? = null
) {
    var name by remember { mutableStateOf(source?.name.orEmpty()) }
    var location by remember { mutableStateOf(source?.location.orEmpty()) }
    val isPlaylistSource = source?.kind == IptvEpgSourceKind.PLAYLIST
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            location = it.toString()
        }
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = if (source == null) "Add EPG source" else "Edit EPG source",
        subtitle = "Add a global XMLTV URL or local XML/XML.GZ file",
        width = 560.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), enabled = !isPlaylistSource,
                singleLine = true, label = { Text("Source name") }, shape = RoundedCornerShape(NuvioTheme.radii.md),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = NuvioTheme.colors.TextPrimary, unfocusedTextColor = NuvioTheme.colors.TextPrimary,
                    focusedContainerColor = NuvioTheme.colors.BackgroundElevated, unfocusedContainerColor = NuvioTheme.colors.BackgroundElevated
                )
            )
            if (!isPlaylistSource) Button(onClick = { filePicker.launch(arrayOf("application/xml", "text/xml", "application/gzip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard)) { Text("Choose XMLTV file") }
            OutlinedTextField(
                value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth(), enabled = !isPlaylistSource,
                singleLine = true, label = { Text("XMLTV URL or file path") }, shape = RoundedCornerShape(NuvioTheme.radii.md),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = NuvioTheme.colors.TextPrimary, unfocusedTextColor = NuvioTheme.colors.TextPrimary,
                    focusedContainerColor = NuvioTheme.colors.BackgroundElevated, unfocusedContainerColor = NuvioTheme.colors.BackgroundElevated
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                if (source != null) {
                    Button(onClick = { onRefresh?.invoke(source) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.BackgroundCard)) { Text("Refresh") }
                    if (!isPlaylistSource) Button(onClick = { onDelete?.invoke(source) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.Error)) { Text("Delete") }
                }
                if (!isPlaylistSource) Button(
                    onClick = { if (name.isNotBlank() && location.isNotBlank()) onSave(IptvEpgSource(source?.id.orEmpty(), name.trim(), location.trim(), IptvEpgSourceKind.MANUAL)) },
                    modifier = Modifier.weight(if (source == null) 1f else 1.3f),
                    colors = ButtonDefaults.colors(containerColor = NuvioTheme.colors.Secondary)
                ) { Text(if (source == null) "Add source" else "Save", color = Color.White) }
            }
        }
    }
}
