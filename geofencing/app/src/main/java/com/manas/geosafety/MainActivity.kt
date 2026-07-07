package com.manas.geosafety

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.manas.geosafety.screens.BackgroundRationaleDialog
import com.manas.geosafety.screens.MapScreen
import com.manas.geosafety.ui.theme.GeoSafetyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeoSafetyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafetyPermissionGateway {
                        MapScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SafetyPermissionGateway(
    content: @Composable () -> Unit
) {
    val foregroundPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // FIX: Inline the evaluation so rememberPermissionState registers correctly in the main tracking tree
    val backgroundPermissionState: PermissionState? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    var showRationaleDialog by remember {
        mutableStateOf(false)
    }

    val isForegroundGranted = foregroundPermissionState.status.isGranted

    val isBackgroundGranted =
        backgroundPermissionState == null ||
                backgroundPermissionState.status.isGranted

    LaunchedEffect(isForegroundGranted, isBackgroundGranted) {
        when {
            !isForegroundGranted -> {
                foregroundPermissionState.launchPermissionRequest()
            }

            !isBackgroundGranted -> {
                showRationaleDialog = true
            }
        }
    }

    if (isForegroundGranted && isBackgroundGranted) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Initializing secure location boundaries...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }

    if (showRationaleDialog && backgroundPermissionState != null) {
        BackgroundRationaleDialog(
            onDismiss = {
                showRationaleDialog = false
            },
            onConfirm = {
                showRationaleDialog = false
                backgroundPermissionState.launchPermissionRequest()
            }
        )
    }
}