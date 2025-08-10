package com.example.wedsnap20.ui.screens.guest

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.wedsnap20.services.AlbumService
import com.example.wedsnap20.services.PhotoItem
import com.example.wedsnap20.services.ReactionService
import com.example.wedsnap20.ui.components.ImageDialog
import com.example.wedsnap20.ui.components.LeaderboardCard
import com.example.wedsnap20.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestGalleryScreen(eventId: String, navController: NavController, viewModel: AuthViewModel) {
    val albumService = remember { AlbumService() }
    val reactionService = remember { ReactionService() }
    val scope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var showOnlyLiked by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf<String?>(null) }

    // local state
    val hearted = remember { mutableStateMapOf<String, Boolean>() }

    // ensure user (anon ok)
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) try { auth.signInAnonymously().await() } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        viewModel.getCurrentUserName { name -> currentUserName = name }
    }

    // load photos + preload heart states
    LaunchedEffect(eventId) {
        albumService.fetchPhotos(eventId) { items ->
            photos = items
            items.forEach { item ->
                scope.launch {
                    try { hearted[item.id] = reactionService.isHearted(eventId, item.id) }
                    catch (_: Exception) { hearted[item.id] = false }
                }
            }
        }
    }

    val visible = remember(showOnlyLiked, photos, hearted) {
        if (!showOnlyLiked) photos
        else photos.filter { hearted[it.id] == true }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guest Album", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { showOnlyLiked = !showOnlyLiked },
                    label = { Text(if (showOnlyLiked) "Liked only" else "All photos") },
                    leadingIcon = { Text(if (showOnlyLiked) "❤️" else "🤍") }
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(visible) { index, item ->
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        Image(
                            painter = rememberAsyncImagePainter(item.url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    // map index back to original photos list
                                    val actualIndex = photos.indexOfFirst { it.id == item.id }
                                    selectedIndex = if (actualIndex >= 0) actualIndex else index
                                    showDialog = true
                                }
                        )
                        Text(
                            text = if (hearted[item.id] == true) "❤️" else "🤍",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clickable {
                                    val current = hearted[item.id] == true
                                    hearted[item.id] = !current
                                    scope.launch {
                                        val serverState = reactionService.toggleHeart(eventId, item.id)
                                        hearted[item.id] = serverState
                                    }
                                }
                        )
                    }
                }
            }
        }

        SmallFloatingActionButton(
            onClick = { showLeaderboard = !showLeaderboard },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Text("🏆") }
    }

    if (showLeaderboard) {
        ModalBottomSheet(onDismissRequest = { showLeaderboard = false }) {
            LeaderboardCard(
                eventId = eventId,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }

    if (showDialog) {
        ImageDialog(
            eventId = eventId,
            photos = photos,
            initialIndex = selectedIndex,
            onDismiss = { showDialog = false },
            isHearted = { pid -> hearted[pid] == true },
            onHeartChanged = { pid, newState -> hearted[pid] = newState },
            currentUserName = currentUserName   // <-- important
        )
    }
}