package com.example.location_experiment

import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.ClipboardManager
import androidx.core.content.ContextCompat

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun setClipboard(context: Context, label: String, text: String) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
