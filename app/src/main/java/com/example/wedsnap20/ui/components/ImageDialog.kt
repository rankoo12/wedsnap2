package com.example.wedsnap20.ui.components

import android.app.DownloadManager
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import androidx.core.net.toUri
import androidx.core.content.FileProvider
import com.example.wedsnap20.services.PhotoItem
import com.example.wedsnap20.services.ReactionService
import com.example.wedsnap20.services.CommentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDialog(
    eventId: String,
    photos: List<PhotoItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    isHearted: (photoId: String) -> Boolean,
    onHeartChanged: (photoId: String, newState: Boolean) -> Unit,
    currentUserName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reactionService = remember { ReactionService() }
    val commentService = remember { CommentService() }

    val pageCount = photos.size
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { pageCount })
    val authority = context.packageName + ".fileprovider"


    // counts + UI state
    val heartCounts  = remember { mutableStateMapOf<String, Int>() } // photoId -> like count
    var commentsCount by remember { mutableStateOf(0) }
    var showComments by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }

    suspend fun refreshHeartCount(photoId: String) {
        try { heartCounts[photoId] = reactionService.getHeartCountFast(eventId, photoId) } catch (_: Exception) {}
    }
    suspend fun refreshCommentsCount(photoId: String) {
        try { commentsCount = commentService.countFast(eventId, photoId) } catch (_: Exception) {}
    }

    // fetch counts when page changes
    LaunchedEffect(pagerState.currentPage) {
        val pid = photos.getOrNull(pagerState.currentPage)?.id ?: return@LaunchedEffect
        refreshHeartCount(pid)
        refreshCommentsCount(pid)
    }

    // Constrain dialog height so content never gets cut
    val cfg = LocalConfiguration.current
    val dialogMaxHeight = (cfg.screenHeightDp.dp * 0.92f)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)       // set 1.0f for full width
                .widthIn(max = 760.dp)     // bump to 900.dp on tablets if you want
                .heightIn(max = dialogMaxHeight)
                .padding(12.dp)
                .background(Color(0xFFFEF9EC), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

                // Image pager at the top (tap → fullscreen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .heightIn(min = 260.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        if (page < pageCount) {
                            val url = photos[page].url
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showFullScreen = true },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Fullscreen overlay (unchanged)
                if (showFullScreen) {
                    Dialog(onDismissRequest = { showFullScreen = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                val url = photos[page].url
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                // ===== Actions row (icons only) =====
                val pid = photos.getOrNull(pagerState.currentPage)?.id
                val liked = pid?.let { isHearted(it) } ?: false
                val likeCount = pid?.let { heartCounts[it] ?: 0 } ?: 0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()) // ensure Comments never disappears
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Download
                    IconButton(onClick = {
                        val url = photos[pagerState.currentPage].url
                        val request = DownloadManager.Request(url.toUri()).apply {
                            setTitle("WedSnap Photo")
                            setDescription("Downloading image...")
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_PICTURES,
                                "wedsnap_${System.currentTimeMillis()}.jpg"
                            )
                            setAllowedOverMetered(true)
                        }
                        val dm = context.getSystemService(DownloadManager::class.java)
                        dm.enqueue(request)
                    }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Download")
                    }

                    // Share
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val imageUrl = photos[pagerState.currentPage].url

                                // MUST be inside cacheDir/images because file_paths.xml has <cache-path path="images/">
                                val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
                                val outFile = File(imagesDir, "shared_image_${System.currentTimeMillis()}.jpg")

                                URL(imageUrl).openStream().use { input ->
                                    outFile.outputStream().use { output -> input.copyTo(output) }
                                }

                                val authority = context.packageName + ".fileprovider" // matches manifest
                                val uri = FileProvider.getUriForFile(context, authority, outFile)

                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                withContext(Dispatchers.Main) {
                                    context.startActivity(
                                        Intent.createChooser(share, "Share image via")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            } catch (_: Exception) { /* log if you want */ }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }



                    // Like with badge
                    BadgedBox(badge = { if (likeCount > 0) Badge { Text("$likeCount") } }) {
                        IconButton(onClick = {
                            if (pid == null) return@IconButton
                            val target = !liked
                            onHeartChanged(pid, target)
                            heartCounts[pid] = (heartCounts[pid] ?: 0) + if (target) 1 else -1
                            scope.launch {
                                val serverState = reactionService.toggleHeart(eventId, pid)
                                onHeartChanged(pid, serverState)
                                refreshHeartCount(pid)
                            }
                        }) {
                            Icon(
                                imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like"
                            )
                        }
                    }

                    // Comments icon with badge (uses emoji to avoid extra icon deps)
                    BadgedBox(badge = { if (commentsCount > 0) Badge { Text("$commentsCount") } }) {
                        IconButton(onClick = {
                            showComments = !showComments
                            if (showComments && pid != null) {
                                // refresh exact count when opening
                                scope.launch { refreshCommentsCount(pid) }
                            }
                        }) {
                            Text("💬")
                        }
                    }
                }

                // Comments block (shown only when toggled on)
                val currentPid = photos.getOrNull(pagerState.currentPage)?.id
                if (showComments && currentPid != null) {
                    CommentsSection(
                        eventId = eventId,
                        photoId = currentPid,
                        onCountChanged = { commentsCount = it },
                        currentUserName = currentUserName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}
