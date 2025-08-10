package com.example.wedsnap20.ui.screens.guest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.wedsnap20.services.AlbumService
import com.example.wedsnap20.services.AuthService
import kotlinx.coroutines.launch
import com.example.wedsnap20.viewmodel.AuthViewModel


@Composable
fun GuestAlbumScreen(eventId: String, navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val albumService = remember { AlbumService() }
    val authService = remember(context) { AuthService(context, viewModel) }
    val scope = rememberCoroutineScope()

    var authReady by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getCurrentUserName { name -> userName = name }
    }

    LaunchedEffect(eventId) {
        authService.ensureSignedIn(
            onReady = { authReady = true },
            onError = { e -> authError = e.message ?: "Authentication failed" }
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (!authReady) return@rememberLauncherForActivityResult
        uris?.forEach { uri ->
            scope.launch {
                try {
                    Log.d("UPLOAD", "Selected URI: $uri")
                    albumService.uploadImageToAlbum(eventId, uri, context, userName ?: "Guest")
                } catch (e: Exception) {
                    Log.e("UPLOAD", "Gallery upload failed", e)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (!authReady) return@rememberLauncherForActivityResult
        bitmap?.let {
            scope.launch {
                try {
                    albumService.uploadBitmapToAlbum(eventId, it, context, userName ?: "Guest")
                } catch (e: Exception) {
                    Log.e("UPLOAD", "Camera upload failed", e)
                }
            }
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Log.w("PERMISSION", "Camera permission denied")
        }
    }

    fun launchCameraWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            cameraLauncher.launch(null)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            authError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Auth error: $authError")
            }
            !authReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Album: $eventId", style = MaterialTheme.typography.titleLarge)

                    Button(onClick = { galleryLauncher.launch("image/*") }, enabled = authReady) {
                        Text("Upload from Gallery")
                    }

                    Button(onClick = { launchCameraWithPermission() }, enabled = authReady) {
                        Text("Take Photo")
                    }

                    Button(
                        onClick = { navController.navigate("guest_gallery/$eventId") },
                        enabled = authReady
                    ) {
                        Text("View Album")
                    }
                }
            }
        }
    }

}
