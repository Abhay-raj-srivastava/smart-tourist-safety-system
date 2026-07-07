package com.manas.geosafety.screens

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.manas.geosafety.geofence.GeofenceManager
import com.manas.geosafety.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

val SearchIconVector: ImageVector
    get() = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.Black), strokeLineWidth = 2f) {
            moveTo(15.5f, 14f)
            horizontalLineTo(14.71f)
            lineTo(14.43f, 13.73f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
            curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
            curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
            curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
            lineTo(14f, 14.71f)
            verticalLineTo(15.5f)
            lineTo(19f, 20.49f)
            lineTo(20.49f, 19f)
            lineTo(15.5f, 14f)
            close()
            moveTo(9.5f, 14f)
            curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
            curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
            curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
            close()
        }
    }.build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(geofenceRadiusMeters: Double = 500.0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationHelper = remember { LocationHelper(context) }
    val geofenceManager = remember { GeofenceManager(context) }

    var searchQuery by remember { mutableStateOf("") }
    var targetGeofenceLocation by remember { mutableStateOf<LatLng?>(null) }
    var targetZoneName by remember { mutableStateOf("") }

    var currentTrackingLocation by remember { mutableStateOf(LatLng(26.4741, 80.2471)) }

    // Tracks whether the camera has snapped to your initial real physical location yet
    var isInitialLocationSet by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentTrackingLocation, 15f)
    }

    DisposableEffect(Unit) {
        locationHelper.startLocationUpdates { location ->
            val newLatLng = LatLng(location.latitude, location.longitude)
            currentTrackingLocation = newLatLng

            // FIX: If this is the very first coordinate update, force the camera to snap smoothly to your phone's real spot
            if (!isInitialLocationSet) {
                isInitialLocationSet = true
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(newLatLng, 16f)
                    )
                }
            }
        }
        onDispose { locationHelper.stopLocationUpdates() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // FIX: Explicitly pass MapProperties with location markers enabled to force the map engine to draw your blue dot tracker puck
            properties = MapProperties(
                isMyLocationEnabled = true
            )
        ) {
            targetGeofenceLocation?.let { targetLatLng ->
                Circle(
                    center = targetLatLng,
                    radius = geofenceRadiusMeters,
                    fillColor = Color(0x33FF0000),
                    strokeColor = Color.Red,
                    strokeWidth = 4f
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Enter custom danger zone address...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            scope.launch {
                                val resolvedLatLng = withContext(Dispatchers.IO) {
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocationName(searchQuery, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            LatLng(addresses[0].latitude, addresses[0].longitude)
                                        } else null
                                    } catch (e: IOException) {
                                        null
                                    }
                                }

                                if (resolvedLatLng != null) {
                                    if (targetZoneName.isNotBlank()) {
                                        geofenceManager.removeGeofence(targetZoneName)
                                    }

                                    targetGeofenceLocation = resolvedLatLng
                                    targetZoneName = searchQuery

                                    geofenceManager.addGeofence(
                                        id = searchQuery,
                                        latitude = resolvedLatLng.latitude,
                                        longitude = resolvedLatLng.longitude,
                                        radiusInMeters = geofenceRadiusMeters.toFloat()
                                    )

                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(resolvedLatLng, 15f)
                                    )
                                    Toast.makeText(context, "Registered: $searchQuery Safety Perimeter", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Location not found. Try again!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = SearchIconVector,
                        contentDescription = "Search and set custom geofence",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}