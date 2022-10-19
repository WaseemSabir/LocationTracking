package com.example.location_experiment.ui

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.location_experiment.ui.theme.ForegroundLocationTheme


@Composable
fun LocationUpdatesScreen(
    showDegradedExperience: Boolean,
    needsPermissionRationale: Boolean,
    openSettingsClick: () -> Unit,
    onButtonClick: () -> Unit,
    isLocationOn: Boolean,
    location: Location?
) {
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showStopConfirmationDialog by remember { mutableStateOf(false) }

    if (showRationaleDialog) {
        CofirmationDialog(
            title = "Location Permission is required.",
            description = "To enable better tracking, we need to access your location.",
            onConfirm = {
                showRationaleDialog = false
                onButtonClick()
            },
            onDismiss = { showRationaleDialog = false }
        )
    }

    if (showStopConfirmationDialog) {
        CofirmationDialog(
            title = "Are you sure?",
            description = "Only stop this location once you have delivered all the deliveries!",
            onConfirm = {
                showStopConfirmationDialog = false
                onButtonClick()
            },
            onDismiss = {
                showStopConfirmationDialog = false
            }
        )
    }

    fun onClick() {
        if (needsPermissionRationale) {
            showRationaleDialog = true
        } else if (showDegradedExperience) {
            openSettingsClick()
        } else if (isLocationOn) {
            showStopConfirmationDialog = true
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

        showDegradedExperience -> "Location Permission is required! Please set the permission to"
        else -> "Not started!"
    }
    val secondaryMessage = when {
        showDegradedExperience -> "Allow all the time."
        else -> null
    }
    val label = if (isLocationOn) {
        "Stop Getting Location"
    } else if (showDegradedExperience) {
        "Open Settings"
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
            if (secondaryMessage != null) {
                Text(
                    text = secondaryMessage,
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.secondary
                )
            }
        }
        Button(onClick = { onClick() }) {
            Text(text = label)
        }
    }
}

@Composable
fun CofirmationDialog(
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


@Preview(showBackground = true)
@Composable
fun LocationUpdatesScreenPreview() {
    ForegroundLocationTheme {
        LocationUpdatesScreen(
            showDegradedExperience = false,
            needsPermissionRationale = false,
            openSettingsClick = {},
            onButtonClick = {},
            isLocationOn = true,
            location = null,
        )
    }
}
