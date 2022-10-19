package com.example.location_experiment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.location_experiment.ui.theme.ForegroundLocationTheme

@Composable
fun InitializingScreen() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Initializing",
            style = MaterialTheme.typography.h6
        )
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun InitializingScreenPreview() {
    ForegroundLocationTheme() {
        InitializingScreen()
    }
}
