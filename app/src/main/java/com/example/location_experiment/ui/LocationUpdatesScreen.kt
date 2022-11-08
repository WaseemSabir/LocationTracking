package com.example.location_experiment.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.location_experiment.setClipboard
import com.example.location_experiment.ui.theme.ForegroundLocationTheme
import com.example.myapplication.R


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocationUpdatesScreen(
    deviceId: Int,
    showAllowInBackgroundDialog: Boolean,
    alreadyShownClick: () -> Unit,
    goToSettingsForBackgroundAllowClick: () -> Unit,
    accessible: Boolean,
    onButtonClick: () -> Unit,
    onToggleAccessibleAreas: () -> Unit,
    isLocationOn: Boolean,
    location: Location?
) {
    var showStopConfirmationDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationString =
        if (location == null) "No Location" else "${location.latitude},${location.longitude}"

    if (showStopConfirmationDialog) {
        ShowTextDialog(
            title = "Are you sure?",
            description = "Only stop this location once you have returned!",
            onConfirm = {
                showStopConfirmationDialog = false
                onButtonClick()
            },
            onDismiss = {
                showStopConfirmationDialog = false
            }
        )
    }

    if (showPermissionRationale) {
        ShowTextDialog(
            title = "Allow all the time",
            description = "Location permission is required to access your location",
            onConfirm = {
                showPermissionRationale = false
                onButtonClick()
            },
            onDismiss = {
                showPermissionRationale = false
            }
        )
    }

    if (showAllowInBackgroundDialog) {
        showAllowAppInBackgroundApp(
            onConfirm = goToSettingsForBackgroundAllowClick,
            onDismiss = alreadyShownClick
        )
    }

    fun onClick() {
        if (isLocationOn) {
            showStopConfirmationDialog = true
        } else if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionRationale = true
        } else {
            onButtonClick()
        }
    }

    val message = when {
        isLocationOn -> if (location != null) {
            "Location: ${location.latitude}, ${location.longitude}"
        } else {
            "Waiting for location..."
        }

        else -> "Not started!"
    }
    val label = if (isLocationOn) {
        "Stop Getting Location"
    } else {
        "Start Getting Location"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "Device: $deviceId",
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { onClick() }) {
                Text(text = label)
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedVisibility(visible = (location != null)) {
                OutlinedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Copied Location: $locationString",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        setClipboard(context, "Copied Location: ", locationString)
                    },
                    content = {
                        Text(text = "Copy Location")
                    }
                )
            }

//            Spacer(modifier = Modifier.height(12.dp))
//            Button(
//                onClick = { onToggleAccessibleAreas() },
//                colors = ButtonDefaults.buttonColors(
//                    backgroundColor = if (accessible.not()) Color.Red else Color.Green,
//                    contentColor = Color.Black
//                ),
//                content = {
//                    if (accessible) {
//                        Text(text = "Area is Accessible")
//                    } else {
//                        Text(text = "Area is Not Accessible")
//                    }
//                }
//            )
        }
    }
}

@Composable
fun ShowTextDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = description)
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun showAllowAppInBackgroundApp(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* don't do anything */ },
        title = {
            Text(text = "Allow App in Background")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "To run the app properly, it is important that you allow the app to run in the background.")
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Find the app name and click the toggle button to allow.")
                Image(
                    painterResource(R.drawable.allowbar),
                    "allow bar"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Go To Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Already Allowed", color = Color.Gray)
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun LocationUpdatesScreenPreview() {
    ForegroundLocationTheme {
        LocationUpdatesScreen(
            deviceId = 1,
            accessible = false,
            showAllowInBackgroundDialog = false,
            alreadyShownClick = {},
            goToSettingsForBackgroundAllowClick = {},
            onButtonClick = {},
            onToggleAccessibleAreas = {},
            isLocationOn = true,
            location = null,
        )
    }
}
