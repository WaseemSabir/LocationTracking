package com.example.location_experiment

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
