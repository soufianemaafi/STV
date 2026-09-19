package com.example.stv.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.stv.R
import com.example.stv.core.domain.model.VideoTrackInfo

/**
 * Dialog de sélection de la qualité vidéo.
 * Affiche la liste des pistes disponibles (Auto, 1080p, 720p, etc.).
 */
@Composable
fun QualitySelectionDialog(
    tracks: List<VideoTrackInfo>,
    currentTrackName: String,
    onDismiss: () -> Unit,
    onTrackSelected: (VideoTrackInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.quality_dialog_title)) },
        text = {
            LazyColumn {
                items(tracks) { track ->
                    val isSelected = if (track.group == null) {
                        currentTrackName.startsWith("Auto")
                    } else {
                        track.name == currentTrackName
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onTrackSelected(track) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (track.group == null && currentTrackName.startsWith("Auto")) currentTrackName else track.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        }
    )
}

