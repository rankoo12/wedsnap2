package com.example.wedsnap20.services

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val points: Long
)

class LeaderboardService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun userPointsRef(eventId: String, userId: String) =
        db.collection("albums").document(eventId)
            .collection("stats").document("userPoints")
            .collection("entries").document(userId) // ← if you prefer flat: use .../userPoints/{userId}

    // If you prefer the flatter path from the rules block, use it consistently in both places.
    private fun flatUserPointsRef(eventId: String, userId: String) =
        db.collection("albums").document(eventId)
            .collection("stats").document("userPoints") // document
            .collection("users").document(userId)       // or just .collection("users") if you want one more level
    // To match the rules shown above, use this instead:
    private fun rulesAlignedRef(eventId: String, userId: String) =
        db.collection("albums").document(eventId)
            .collection("stats").document("userPoints")
            .collection("users").document(userId)

    suspend fun incrementPoint(eventId: String, userId: String, displayName: String?) {
        val ref = rulesAlignedRef(eventId, userId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val now = Timestamp.now()
            if (snap.exists()) {
                val current = snap.getLong("points") ?: 0L
                tx.update(ref, mapOf(
                    "points" to current + 1,
                    "displayName" to (displayName ?: snap.getString("displayName") ?: "Guest"),
                    "lastUpdated" to now
                ))
            } else {
                tx.set(ref, mapOf(
                    "points" to 1L,
                    "displayName" to (displayName ?: "Guest"),
                    "lastUpdated" to now
                ))
            }
            null
        }.await()
    }

    suspend fun topUsers(eventId: String, limit: Long = 10): List<LeaderboardEntry> {
        val col = db.collection("albums").document(eventId)
            .collection("stats").document("userPoints")
            .collection("users")
        val q = col.orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()

        return q.documents.map {
            LeaderboardEntry(
                userId = it.id,
                displayName = it.getString("displayName") ?: "Guest",
                points = it.getLong("points") ?: 0L
            )
        }
    }
}
