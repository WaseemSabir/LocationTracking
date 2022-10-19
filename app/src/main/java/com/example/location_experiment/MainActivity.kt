package com.example.location_experiment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.example.location_experiment.PlayServicesAvailableState.Initializing
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesAvailable
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesUnavailable
import com.example.location_experiment.ui.InitializingScreen
import com.example.location_experiment.ui.LocationPermissionState
import com.example.location_experiment.ui.LocationUpdatesScreen
import com.example.location_experiment.ui.ServiceUnavailableScreen
import com.example.location_experiment.ui.theme.ForegroundLocationTheme
import com.example.myapplication.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationPermissionState = LocationPermissionState(this) {
            if (it.hasPermission()) {
                viewModel.toggleLocationUpdates()
            }
        }

        setContent {
            ForegroundLocationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(id = R.string.app_name))
                            }
                        )
                    }
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        locationPermissionState = locationPermissionState
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, ForegroundLocationService::class.java)
        bindService(serviceIntent, viewModel, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(viewModel)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    locationPermissionState: LocationPermissionState
) {
    val uiState by viewModel.playServicesAvailableState.collectAsState()
    val isLocationOn by viewModel.isReceivingLocationUpdates.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()

    when (uiState) {
        Initializing -> InitializingScreen()
        PlayServicesUnavailable -> ServiceUnavailableScreen()
        PlayServicesAvailable -> {
            LocationUpdatesScreen(
                showDegradedExperience = locationPermissionState.showDegradedExperience,
                needsPermissionRationale = locationPermissionState.shouldShowRationale(),
                openSettingsClick = locationPermissionState::openSettings,
                onButtonClick = locationPermissionState::requestPermissions,
                isLocationOn = isLocationOn,
                location = lastLocation,
            )
        }
    }
}
