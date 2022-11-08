package com.example.location_experiment

import android.content.ServiceConnection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.location_experiment.PlayServicesAvailableState.Initializing
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesAvailable
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesUnavailable
import com.example.location_experiment.data.LocationPreferences
import com.example.location_experiment.data.LocationRepository
import com.example.location_experiment.data.PlayServicesAvailabilityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    playServicesAvailabilityChecker: PlayServicesAvailabilityChecker,
    locationRepository: LocationRepository,
    private val locationPreferences: LocationPreferences,
    private val serviceConnection: ForegroundLocationServiceConnection
) : ViewModel(), ServiceConnection by serviceConnection {

    val playServicesAvailableState = flow {
        emit(
            if (playServicesAvailabilityChecker.isGooglePlayServicesAvailable()) {
                PlayServicesAvailable
            } else {
                PlayServicesUnavailable
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Initializing)

    val allowInBackgroundAlreadyShown = MutableStateFlow(false)
    val isReceivingLocationUpdates = locationRepository.isReceivingLocationUpdates
    val lastLocation = locationRepository.lastLocation
    val deviceId = locationRepository.deviceId
    val isAccessible = locationRepository.isAccessible

    fun setShowAllowInBackgroundAlreadyShownTrue() {
        allowInBackgroundAlreadyShown.value = true
    }

    fun toggleLocationUpdates() {
        if (isReceivingLocationUpdates.value) {
            stopLocationUpdates()
        } else {
            startLocationUpdates()
        }
    }

    fun toggleAccessibility() {
        serviceConnection.service?.toggleAccessibility()
    }

    private fun startLocationUpdates() {
        serviceConnection.service?.startLocationUpdates()
        viewModelScope.launch {
            locationPreferences.setLocationTurnedOn(true)
        }
    }

    private fun stopLocationUpdates() {
        serviceConnection.service?.stopLocationUpdates()
        viewModelScope.launch {
            locationPreferences.setLocationTurnedOn(false)
        }
    }
}

enum class PlayServicesAvailableState {
    Initializing, PlayServicesUnavailable, PlayServicesAvailable
}
