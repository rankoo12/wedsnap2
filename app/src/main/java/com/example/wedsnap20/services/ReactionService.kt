package com.example.wedsnap20.services

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.AggregateSource

class ReactionService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun userHeartRef(eventId: String, photoId: String, uid: String) =
        db.collection("albums").document(eventId)
            .collection("photoReactions").document(photoId)
            .collection("users").document(uid)

    /** Toggle heart for the current user on a given photo. Returns new state. */
    suspend fun toggleHeart(eventId: String, photoId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val ref = userHeartRef(eventId, photoId, uid)
        val snap = ref.get().await()
        return if (snap.exists()) {
            ref.delete().await()
            false
        } else {
            ref.set(mapOf("type" to "heart", "createdAt" to Timestamp.now())).await()
            true
        }
    }

    /** Optional helper to pre-check UI state. */
    suspend fun isHearted(eventId: String, photoId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return userHeartRef(eventId, photoId, uid).get().await().exists()
    }
    /** One-shot count of hearts on a photo. */
    suspend fun getHeartCount(eventId: String, photoId: String): Int {
        val snap = db.collection("albums").document(eventId)
            .collection("photoReactions").document(photoId)
            .collection("users")
            .get()
            .await()
        return snap.size()
    }

    /** Realtime count listener (returns registration; caller should remove on dispose). */
    fun observeHeartCount(
        eventId: String,
        photoId: String,
        onChange: (Int) -> Unit
    ): ListenerRegistration {
        return db.collection("albums").document(eventId)
            .collection("photoReactions").document(photoId)
            .collection("users")
            .addSnapshotListener { qs, _ ->
                onChange(qs?.size() ?: 0)
            }
    }
    suspend fun getHeartCountFast(eventId: String, photoId: String): Int {
        val q = db.collection("albums").document(eventId)
            .collection("photoReactions").document(photoId)
            .collection("users")
        return q.count().get(AggregateSource.SERVER).await().count.toInt()
    }
}
