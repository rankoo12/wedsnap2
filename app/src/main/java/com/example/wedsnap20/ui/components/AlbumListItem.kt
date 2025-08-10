package com.example.wedsnap20.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wedsnap20.core.links.InviteLinks
import com.example.wedsnap20.model.Album

@Composable
fun AlbumListItem(
    album: Album,
    onQRCodeClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onQRCodeClick(album.eventId) }, // pass raw eventId to dialog
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Album: ${album.name}", style = MaterialTheme.typography.bodyLarge)
                Text("Event ID: ${album.eventId}", style = MaterialTheme.typography.bodySmall)
            }

            Box(
                modifier = Modifier.size(72.dp)
            ) {
                // Thumbnail QR encodes the deep link (consistent everywhere)
                QRCodeImage(
                    content = InviteLinks.forEvent(album.eventId),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Divider()
    }
}
