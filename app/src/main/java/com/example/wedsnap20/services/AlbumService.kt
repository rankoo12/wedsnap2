package com.example.wedsnap20.services

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.wedsnap20.model.Album
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

data class PhotoItem(val id: String, val url: String)


class AlbumService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val leaderboard = LeaderboardService()

    private fun bumpLeaderboard(eventId: String, displayName: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val safeName = displayName.ifBlank { user.displayName ?: "Guest" }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                leaderboard.incrementPoint(eventId, uid, safeName)
            } catch (e: Exception) {
                Log.w("AlbumService", "Failed to increment leaderboard", e)
            }
        }
    }

    fun createAlbum(
        name: String,
        onResult: (Album?) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onResult(null)
        val eventId = UUID.randomUUID().toString().take(8)

        val album = Album(
            eventId = eventId,
            name = name,
            hostUid = uid
        )

        db.collection("albums").document(eventId)
            .set(album)
            .addOnSuccessListener { onResult(album) }
            .addOnFailureListener { onResult(null) }
    }

    fun getAlbumsByHost(
        onResult: (List<Album>) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyList())

        db.collection("albums")
            .whereEqualTo("hostUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val albums = snapshot.documents.mapNotNull { it.toObject(Album::class.java) }
                onResult(albums)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun uploadImageToAlbum(eventId: String, imageUri: Uri, context: Context, uploaderName: String?) {
        Log.d("AlbumService", "uploadImageToAlbum called with eventId=$eventId, uri=$imageUri")
        val user = auth.currentUser ?: run {
            Log.w("AlbumService", "No user – cannot upload")
            return
        }

        val safeName = (uploaderName ?: user.displayName ?: "Guest").ifBlank { "Guest" }
        val imageRef = storage.reference.child("albums/$eventId/${UUID.randomUUID()}.jpg")

        val meta = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("eventId", eventId)
            .setCustomMetadata("uploaderUid", user.uid)
            .setCustomMetadata("uploaderName", safeName)
            .build()

        imageRef.putFile(imageUri, meta)
            .addOnSuccessListener {
                Log.d("AlbumService", "Image uploaded successfully")
                bumpLeaderboard(eventId, safeName)
            }
            .addOnFailureListener { e ->
                Log.e("AlbumService", "Image upload failed", e)
            }
    }

    fun uploadBitmapToAlbum(eventId: String, bitmap: Bitmap, context: Context, uploaderName: String?) {
        Log.d("AlbumService", "uploadBitmapToAlbum called with eventId=$eventId")
        val user = auth.currentUser ?: run {
            Log.w("AlbumService", "No user – cannot upload")
            return
        }

        val safeName = (uploaderName ?: user.displayName ?: "Guest").ifBlank { "Guest" }
        val imageRef = storage.reference.child("albums/$eventId/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val meta = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("eventId", eventId)
            .setCustomMetadata("uploaderUid", user.uid)
            .setCustomMetadata("uploaderName", safeName)
            .build()

        imageRef.putBytes(data, meta)
            .addOnSuccessListener {
                Log.d("AlbumService", "Camera image uploaded successfully")
                bumpLeaderboard(eventId, safeName)
            }
            .addOnFailureListener { e ->
                Log.e("AlbumService", "Camera upload failed", e)
            }
    }

    fun fetchImageUrls(eventId: String, onResult: (List<String>) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("albums/$eventId/")
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                val urls = mutableListOf<String>()
                val total = listResult.items.size
                if (total == 0) return@addOnSuccessListener onResult(emptyList())

                var count = 0
                listResult.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        count++
                        if (count == total) onResult(urls)
                    }.addOnFailureListener {
                        count++
                        if (count == total) onResult(urls)
                    }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    fun fetchPhotos(eventId: String, onResult: (List<PhotoItem>) -> Unit) {
        val dirRef = FirebaseStorage.getInstance().reference.child("albums/$eventId/")
        dirRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                val items = mutableListOf<PhotoItem>()
                var done = 0
                val total = listResult.items.size
                listResult.items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            // Use the storage object name as photoId
                            items.add(PhotoItem(id = item.name, url = uri.toString()))
                            done++
                            if (done == total) onResult(items)
                        }
                        .addOnFailureListener {
                            done++
                            if (done == total) onResult(items)
                        }
                }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

}
