package com.example.wedsnap20.services

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class CommentDTO(
    val id: String = "",
    val uid: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

class CommentService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun col(eventId: String, photoId: String) =
        db.collection("albums").document(eventId)
            .collection("photoComments").document(photoId)
            .collection("items")

    suspend fun add(eventId: String, photoId: String, text: String, authorName: String) {
        val uid = auth.currentUser?.uid ?: return
        val doc = mapOf(
            "uid" to uid,
            "authorName" to authorName.ifBlank { "Guest" },
            "text" to text.trim(),
            "createdAt" to Timestamp.now()
        )
        col(eventId, photoId).add(doc).await()
    }

    suspend fun countFast(eventId: String, photoId: String): Int {
        return col(eventId, photoId).count().get(AggregateSource.SERVER).await().count.toInt()
    }

    fun listenPage(
        eventId: String,
        photoId: String,
        pageSize: Int = 20,
        onSnapshot: (List<CommentDTO>, lastSnapId: String?) -> Unit
    ): ListenerRegistration {
        val q = col(eventId, photoId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        return q.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map {
                CommentDTO(
                    id = it.id,
                    uid = it.getString("uid") ?: "",
                    authorName = it.getString("authorName") ?: "",
                    text = it.getString("text") ?: "",
                    createdAt = it.getTimestamp("createdAt")
                )
            }.orEmpty()
            val last = snap?.documents?.lastOrNull()?.id
            onSnapshot(list, last)
        }
    }

    suspend fun loadMore(
        eventId: String,
        photoId: String,
        afterId: String,
        pageSize: Int = 20
    ): List<CommentDTO> {
        val afterDoc = col(eventId, photoId).document(afterId).get().await()
        val snap = col(eventId, photoId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(afterDoc)
            .limit(pageSize.toLong())
            .get()
            .await()
        return snap.documents.map {
            CommentDTO(
                id = it.id,
                uid = it.getString("uid") ?: "",
                authorName = it.getString("authorName") ?: "",
                text = it.getString("text") ?: "",
                createdAt = it.getTimestamp("createdAt")
            )
        }
    }
}
