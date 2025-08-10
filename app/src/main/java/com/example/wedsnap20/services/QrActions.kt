package com.example.wedsnap20.services

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object QrActions {
    fun shareQr(context: Context, qrBitmap: Bitmap, inviteLink: String) {
        val cache = File(context.cacheDir, "images").apply { mkdirs() }
        val f = File(cache, "event_qr.png")
        FileOutputStream(f).use { qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Join our event: $inviteLink")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share invite"))
    }

    fun downloadQr(context: Context, qrBitmap: Bitmap, filename: String = "event_qr.png"): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WedSnap")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it).use { out: OutputStream? ->
                    out?.let { s -> qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, s) }
                }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "WedSnap").apply { mkdirs() }
            val f = File(dir, filename)
            FileOutputStream(f).use { qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Uri.fromFile(f)
        }
    }
}
