package com.example.wedsnap20.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.wedsnap20.ui.util.LocalActivity
import com.example.wedsnap20.ui.util.navGraphViewModel
import com.example.wedsnap20.viewmodel.DeepLinkViewModel
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

@Composable
fun ScannerScreen(navController: NavHostController) {
    val vm: DeepLinkViewModel = navGraphViewModel(navController)
    val pendingId = vm.pendingEventId.collectAsState().value
    val activity = LocalActivity()
    var scannedText by remember { mutableStateOf<String?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentResult: IntentResult =
            IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        scannedText = intentResult?.contents ?: "Scan cancelled"

        val eventId = parseEventId(scannedText)
        if (!hasNavigated && eventId != null) {
            hasNavigated = true
            navController.navigate("guest_album/$eventId")
        }
    }

    LaunchedEffect(Unit) {
        if(pendingId != null) {
            navController.navigate("guest_album/$pendingId")
        }
        val isEmulator = Build.FINGERPRINT.contains("emu")
        if (isEmulator) {
            // Dev shortcut on emulator
            if (!hasNavigated) {
                hasNavigated = true
                navController.navigate("guest_album/aa4f37ab")}

        } else {
            // Launch QR scanner once on real device
            val integrator = IntentIntegrator(activity)
            integrator.setOrientationLocked(true)
            integrator.setPrompt("Scan a QR code")
            launcher.launch(integrator.createScanIntent())
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("QR Scan Result:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(scannedText ?: "Waiting...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Accepts both:
 *  - New deep-link format: "wedsnap://join/{eventId}"
 *  - Legacy format:       "{eventId}" (raw)
 */
private fun parseEventId(scanned: String?): String? {
    if (scanned.isNullOrBlank() || scanned == "Scan cancelled") return null

    val prefix = "wedsnap://join/"
    val candidate = if (scanned.startsWith(prefix)) {
        scanned.removePrefix(prefix).trim()
    } else {
        scanned.trim()
    }
    return candidate.takeIf { it.isNotBlank() }
}
