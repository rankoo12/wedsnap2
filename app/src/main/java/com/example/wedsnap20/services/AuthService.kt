package com.example.wedsnap20.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wedsnap20.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthService(private val context: Context, private val viewModel: AuthViewModel) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.wedsnap20.R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(data: Intent?, onResult: (Result<FirebaseUser>) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user!!
                    viewModel.setUser(user) // keep VM in sync
                    onResult(Result.success(user))
                }
                .addOnFailureListener { e ->
                    onResult(Result.failure(e))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    fun signInAnonymously(
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    viewModel.setUser(user)
                    onSuccess(user)
                } else {
                    onFailure(Exception("Anonymous user is null"))
                }
            }
            .addOnFailureListener { error ->
                val fallback = auth.currentUser
                if (fallback != null && fallback.isAnonymous) {
                    Log.w("AuthService", "signInAnonymously failed but fallback user exists")
                    viewModel.setUser(fallback)
                    onSuccess(fallback)
                } else {
                    onFailure(error)
                }
            }
    }

    fun createOrUpdateUser(
        user: FirebaseUser,
        name: String,
        type: String, // "guest" or "host"
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userData = mapOf(
            "uid" to user.uid,
            "name" to name,
            "type" to type,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserType(
        uid: String,
        onResult: (String?) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("type"))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun isUserExists(
        uid: String,
        onResult: (Boolean) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc -> onResult(doc.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    fun getCurrentUserNameFromDb(
        uid: String,
        onResult: (String?) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc -> onResult(doc.getString("name")) }
            .addOnFailureListener { onResult(null) }
    }

    fun ensureSignedIn(
        onReady: (FirebaseUser) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val existing = auth.currentUser
        if (existing != null) {
            viewModel.setUser(existing)
            onReady(existing)
            return
        }
        signInAnonymously(
            onSuccess = {
                viewModel.setUser(it)
                onReady(it)
            },
            onFailure = { onError(it) }
        )
    }

    /**
     * Ensure Google user has a persisted name in both FirebaseAuth and Firestore.
     * Resolution order: preferredName (typed) -> Google displayName -> friendly fallback.
     * Uses SetOptions.merge() to avoid overwriting existing fields like "type".
     */
    fun ensureGoogleProfileName(
        preferredName: String?,
        onDone: () -> Unit = {}
    ) {
        val user = auth.currentUser ?: return onDone()
        val resolved = when {
            !preferredName.isNullOrBlank() -> preferredName.trim()
            !user.displayName.isNullOrBlank() -> user.displayName!!.trim()
            else -> generateRandomFallbackName()
        }

        fun upsertFirestore() {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "name" to resolved,
                        "email" to (user.email ?: "")
                    ),
                    SetOptions.merge()
                )
                .addOnCompleteListener { onDone() }
        }

        if (user.displayName != resolved) {
            val updates = userProfileChangeRequest { displayName = resolved }
            user.updateProfile(updates)
                .addOnCompleteListener { upsertFirestore() }
        } else {
            upsertFirestore()
        }
    }

    private fun generateRandomFallbackName(): String =
        listOf(
            "Happy Guest", "Camera Ninja", "Party Panda",
            "Snap Wizard", "Sunny Koala", "Witty Otter"
        ).random()
}
