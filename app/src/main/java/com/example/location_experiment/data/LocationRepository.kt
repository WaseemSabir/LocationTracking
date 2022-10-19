package com.example.location_experiment.data

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {
    private val callback = Callback()

    private val _isReceivingUpdates = MutableStateFlow(false)
    val isReceivingLocationUpdates = _isReceivingUpdates.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    @SuppressLint("MissingPermission") // Only called when holding location permission.
    fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 30_000 // 30 seconds
        }

        val handlerThread = HandlerThread("LocationCallbackHandler")
        handlerThread.start()
        fusedLocationProviderClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )
        _isReceivingUpdates.value = true
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(callback)
        _isReceivingUpdates.value = false
        _lastLocation.value = null
    }

    private fun pushUpdatesToFireStore(data: LocationResult) {
        var dt: LocalDateTime? = null
        var dtStr: String? = null
        val deviceID = 6

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dt = LocalDateTime.now()
            dtStr = LocalDateTime.now().toString()
        }

        val db = Firebase.firestore
        val mappedData = mapOf(
            "datetime" to dt,
            "datetime_str" to dtStr,
            "location" to data.lastLocation,
            "device" to deviceID,
        )
        db.collection("deliveryLocationTracking")
            .add(mappedData)
            .addOnSuccessListener { documentReference ->
                Log.d(
                    "FIREBASE_",
                    "SUCCESS: DocumentSnapshot added with ID: ${documentReference.id}"
                )
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_", "Error: adding document", e)
            }
    }

    private inner class Callback : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            _lastLocation.value = result.lastLocation
            pushUpdatesToFireStore(data = result)
        }
    }
}
