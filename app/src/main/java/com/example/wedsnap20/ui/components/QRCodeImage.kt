package com.example.wedsnap20.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.example.wedsnap20.core.qr.QrGenerator

@Composable
fun QRCodeImage(
    content: String,                 // this is the actual string to encode in QR
    modifier: Modifier = Modifier,
    onBitmapReady: (Bitmap) -> Unit = {}
) {
    val bmp = remember(content) { QrGenerator.generate(content, size = 512) }
    LaunchedEffect(bmp) { onBitmapReady(bmp) }
    Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR Code", modifier = modifier)
}
