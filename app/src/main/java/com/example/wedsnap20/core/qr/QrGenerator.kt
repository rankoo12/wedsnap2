package com.example.wedsnap20.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QrGenerator {
    fun generate(content: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = createBitmap(size, size)
        for (x in 0 until size) for (y in 0 until size) {
            bmp[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        return bmp
    }
}
