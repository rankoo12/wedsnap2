package com.example.wedsnap20.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wedsnap20.services.CommentDTO
import com.example.wedsnap20.services.CommentService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun CommentsSection(
    eventId: String,
    photoId: String,
    onCountChanged: (Int) -> Unit,
    currentUserName: String? = null,                // <-- NEW (optional)
    modifier: Modifier = Modifier
) {
    val service = remember { CommentService() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    var comments by remember { mutableStateOf<List<CommentDTO>>(emptyList()) }
    var lastId by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }

    // realtime first page
    DisposableEffect(eventId, photoId) {
        val reg = service.listenPage(eventId, photoId, onSnapshot = { list, last ->
            comments = list
            lastId = last
            onCountChanged(list.size) // cheap first-page count; use countFast for exact if needed
        })
        onDispose { reg.remove() }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(comments) { c ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.authorName.ifBlank { "Guest" }, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(c.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                if (lastId != null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val more = service.loadMore(eventId, photoId, lastId!!)
                                comments = comments + more
                                lastId = more.lastOrNull()?.id
                                onCountChanged(service.countFast(eventId, photoId))
                            }
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) { Text("Load more") }
                }
            }
        }

        // Input
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(500) },
                placeholder = { Text("Add a comment…") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        scope.launch {
                            // resolved display name: prefer the one provided by parent (DB via ViewModel),
                            // else Firebase displayName, else "Guest"
                            val author = when {
                                !currentUserName.isNullOrBlank() -> currentUserName
                                !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser!!.displayName!!
                                else -> "Guest"
                            }
                            service.add(eventId, photoId, text, author)
                            input = ""
                            onCountChanged(service.countFast(eventId, photoId))
                        }
                    }
                }
            ) { Text("Post") }
        }
    }
}
