package com.example.wedsnap20.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wedsnap20.core.links.InviteLinks
import com.example.wedsnap20.services.QrActions

@Composable
fun QRCodeDialog(
    content: String,                 // eventId
    onDismiss: () -> Unit,
    onViewAlbum: (String) -> Unit = {}  // NEW: delegate navigation to caller
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val inviteLink = remember(content) { InviteLinks.forEvent(content) }
    val qrPayload  = inviteLink

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                // NEW: View Album action
                TextButton(onClick = { onViewAlbum(content) }) {
                    Text("View Album")
                }

                TextButton(
                    enabled = qrBitmap != null,
                    onClick = { qrBitmap?.let { QrActions.shareQr(context, it, inviteLink) } }
                ) { Text("Share") }

                TextButton(
                    enabled = qrBitmap != null,
                    onClick = {
                        qrBitmap?.let { bmp ->
                            val uri = QrActions.downloadQr(
                                context,
                                bmp,
                                filename = "wedsnap_qr_${content}.png"
                            )
                            uri?.let {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(it, "image/png")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try { context.startActivity(viewIntent) } catch (_: Exception) { }
                            }
                        }
                    }
                ) { Text("Download") }
            }
        },
        title = { Text("QR Code") },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                QRCodeImage(
                    content = content, // or qrPayload to encode deep link
                    modifier = Modifier.size(240.dp),
                    onBitmapReady = { qrBitmap = it }
                )
            }
        }
    )
}
