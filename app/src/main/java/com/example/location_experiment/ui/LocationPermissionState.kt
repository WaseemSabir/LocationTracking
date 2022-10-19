package com.example.location_experiment.ui

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private val locationPermissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            permission.ACCESS_COARSE_LOCATION,
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION)
    }

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal fun Activity.shouldShowRationaleFor(permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

class LocationPermissionState(
    private val activity: ComponentActivity,
    private val onResult: (LocationPermissionState) -> Unit
) {
    var accessCoarseLocationGranted by mutableStateOf(false)
        private set

    var accessCoarseLocationNeedsRationale by mutableStateOf(false)
        private set

    var accessFineLocationGranted by mutableStateOf(false)
        private set

    var accessFineLocationNeedsRationale by mutableStateOf(false)
        private set

    var accessBackgroundLocationGranted by mutableStateOf(false)

    var accessBackgroundLocationNeedsRationale by mutableStateOf(false)
        private set

    var showDegradedExperience by mutableStateOf(false)
        private set

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            updateState()
            showDegradedExperience = !hasPermission()
            onResult(this)
        }

    init {
        updateState()
    }

    private fun updateState() {
        accessCoarseLocationGranted = activity.hasPermission(permission.ACCESS_COARSE_LOCATION)
        accessCoarseLocationNeedsRationale =
            activity.shouldShowRationaleFor(permission.ACCESS_COARSE_LOCATION)
        accessFineLocationGranted = activity.hasPermission(permission.ACCESS_FINE_LOCATION)
        accessFineLocationNeedsRationale =
            activity.shouldShowRationaleFor(permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            accessBackgroundLocationGranted =
                activity.hasPermission(permission.ACCESS_BACKGROUND_LOCATION)
            accessBackgroundLocationNeedsRationale =
                activity.shouldShowRequestPermissionRationale(permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun requestPermissions() {
        permissionLauncher.launch(locationPermissions)
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }

    fun hasPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        (accessCoarseLocationGranted || accessFineLocationGranted) && accessBackgroundLocationGranted
    } else (accessCoarseLocationGranted || accessFineLocationGranted)


    fun showRationale(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        (accessCoarseLocationNeedsRationale || accessFineLocationNeedsRationale) && accessBackgroundLocationNeedsRationale
    } else (accessCoarseLocationNeedsRationale || accessFineLocationNeedsRationale)

    fun shouldShowRationale(): Boolean =
        !hasPermission() && showRationale()
}
