package com.example.wedsnap20.ui.util

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun LocalActivity(): Activity {
    val context = LocalContext.current
    return generateSequence(context) {
        if (it is android.content.ContextWrapper) it.baseContext else null
    }.filterIsInstance<Activity>().first()
}
