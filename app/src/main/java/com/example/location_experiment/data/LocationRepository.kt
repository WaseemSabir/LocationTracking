package com.example.location_experiment.data

import android.annotation.SuppressLint
import android.location.Location
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocationRepository @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {
    private val callback = Callback()

    private val _isReceivingUpdates = MutableStateFlow(false)
    val isReceivingLocationUpdates = _isReceivingUpdates.asStateFlow()

    private val accessibility = MutableStateFlow(true)
    val isAccessible = accessibility.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    val deviceId = 1

    @SuppressLint("MissingPermission") // Only called when holding location permission.
    fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 30_000
            fastestInterval = 15_000
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
        val c: Date = Calendar.getInstance().time
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = df.format(c)

        val db = Firebase.firestore
        db.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }

        val mappedData = mapOf(
            "date_str" to formattedDate,
            "area_accessible" to accessibility.value,
            "location" to data.lastLocation,
            "device" to deviceId,
            "serverTimestamp" to FieldValue.serverTimestamp()
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

    fun toggleAccessible() {
        accessibility.value = accessibility.value.not()
    }

    private inner class Callback : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            _lastLocation.value = result.lastLocation
            pushUpdatesToFireStore(data = result)
        }
    }
}
