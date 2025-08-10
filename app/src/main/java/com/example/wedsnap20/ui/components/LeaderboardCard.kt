package com.example.wedsnap20.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wedsnap20.services.LeaderboardEntry
import com.example.wedsnap20.services.LeaderboardService

@Composable
fun LeaderboardCard(
    eventId: String,
    modifier: Modifier = Modifier
) {
    val service = remember { LeaderboardService() }
    var rows by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        error = null
        loading = true
        try {
            rows = service.topUsers(eventId, 10)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load leaderboard"
        } finally {
            loading = false
        }
    }

    // Auto-load each time the sheet opens (card enters composition)
    LaunchedEffect(eventId) { load() }

    Card(modifier = modifier, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text("Top Contributors", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when {
                loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                rows.isEmpty() -> Text("Be the first to upload!")
                else -> {
                    rows.forEachIndexed { i, e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${i + 1}. ${e.displayName}")
                            Text("${e.points}")
                        }
                        if (i < rows.lastIndex) Divider(Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
