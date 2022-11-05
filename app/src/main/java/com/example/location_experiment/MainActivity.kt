package com.example.location_experiment

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.location_experiment.PlayServicesAvailableState.Initializing
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesAvailable
import com.example.location_experiment.PlayServicesAvailableState.PlayServicesUnavailable
import com.example.location_experiment.ui.InitializingScreen
import com.example.location_experiment.ui.LocationUpdatesScreen
import com.example.location_experiment.ui.ServiceUnavailableScreen
import com.example.location_experiment.ui.theme.ForegroundLocationTheme
import com.example.myapplication.R
import dagger.hilt.android.AndroidEntryPoint
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@AndroidEntryPoint
@RuntimePermissions
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermission(permission.ACCESS_BACKGROUND_LOCATION)) {
            viewModel.toggleLocationUpdates()
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
                    Column(
                        modifier = Modifier
                            .padding(it)
                    ) {
                        MainScreen(
                            onButtonClick = {
                                checkAndStartLocationUpdates()
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    private fun openAppSettings() {
        if (this.isFinishing) return
        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", this.packageName, null)
        appSettingsIntent.data = uri
        this.startActivityForResult(appSettingsIntent, 0) // Ignore the result
    }

    private fun checkAndStartLocationUpdates() {
        if (!hasPermission(permission.ACCESS_BACKGROUND_LOCATION) || !hasPermission(permission.ACCESS_FINE_LOCATION)) {
            this.openAppSettings()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                toggleLocationUpdatesWithPermissionCheck()
            } else {
                toggleFineUpdatesWithPermissionCheck()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @NeedsPermission(permission.ACCESS_BACKGROUND_LOCATION)
    fun toggleLocationUpdates() {
        viewModel.toggleLocationUpdates()
    }

    @NeedsPermission(permission.ACCESS_FINE_LOCATION)
    fun toggleFineUpdates() {
        viewModel.toggleLocationUpdates()
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
    onButtonClick: () -> Unit
) {
    val uiState by viewModel.playServicesAvailableState.collectAsState()
    val isLocationOn by viewModel.isReceivingLocationUpdates.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()
    val isAccessible by viewModel.isAccessible.collectAsState()

    when (uiState) {
        Initializing -> InitializingScreen()
        PlayServicesUnavailable -> ServiceUnavailableScreen()
        PlayServicesAvailable -> {
            LocationUpdatesScreen(
                accessible = isAccessible,
                onToggleAccessibleAreas = {
                    viewModel.toggleAccessibility()
                },
                deviceId = viewModel.deviceId,
                onButtonClick = onButtonClick,
                isLocationOn = isLocationOn,
                location = lastLocation,
            )
        }
    }
}
