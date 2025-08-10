package com.example.wedsnap20.ui.screens.host

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.wedsnap20.core.links.InviteLinks
import com.example.wedsnap20.model.Album
import com.example.wedsnap20.services.AlbumService
import com.example.wedsnap20.ui.components.AlbumListItem
import com.example.wedsnap20.ui.components.QRCodeDialog
import com.example.wedsnap20.ui.components.QRCodeImage
import com.example.wedsnap20.viewmodel.AuthViewModel

enum class HostScreenState { Idle, Create, View }

@Composable
fun HostPanelScreen(viewModel: AuthViewModel, navController: NavHostController) {
    val albumService = remember { AlbumService() }

    var screenState by remember { mutableStateOf(HostScreenState.Idle) }
    var albumName by remember { mutableStateOf(TextFieldValue("")) }
    var album by remember { mutableStateOf<Album?>(null) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var userName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getCurrentUserName { name -> userName = name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome, ${userName ?: "Loading..."}", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { screenState = HostScreenState.Create }) {
            Text("Create New Album")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            albumService.getAlbumsByHost {
                albums = it
                screenState = HostScreenState.View
            }
        }) {
            Text("View My Albums")
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (screenState) {
            HostScreenState.Create -> {
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text("Album Name") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    albumService.createAlbum(albumName.text) { created ->
                        album = created
                    }
                }) {
                    Text("Confirm Create")
                }

                Spacer(modifier = Modifier.height(24.dp))

                album?.let {
                    Text("Event ID: ${it.eventId}")
                    Spacer(modifier = Modifier.height(16.dp))
                    // Display QR that encodes a deep link
                    QRCodeImage(content = InviteLinks.forEvent(it.eventId))
                }
            }

            HostScreenState.View -> {
                if (albums.isEmpty()) {
                    Text("No albums found.")
                } else {
                    Text("My Albums", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    var qrToShow by remember { mutableStateOf<String?>(null) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(albums) { item ->
                            AlbumListItem(
                                album = item,
                                onQRCodeClick = { qrToShow = it } // passes raw eventId
                            )
                        }
                    }

                    qrToShow?.let { eventId ->
                        QRCodeDialog(
                            content = eventId,
                            onDismiss = { qrToShow = null },
                            onViewAlbum = { id ->
                                qrToShow = null
                                navController.navigate("guest_album/$id")
                            }
                        )
                    }
                }
            }

            HostScreenState.Idle -> Unit
        }
    }
}
