package com.rescuemate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.rescuemate.R
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.LocationHelper
import com.rescuemate.utils.rememberLocationPermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveLocationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val locationPermissionsState = rememberLocationPermissionsState()
    var isSharing by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationAddress by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Request permissions and get location
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, retryTrigger) {
        if (locationPermissionsState.allPermissionsGranted) {
            isLoadingLocation = true
            try {
                val location = locationHelper.getCurrentLocation()
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    locationAddress = String.format("%.4f° N, %.4f° E", it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoadingLocation = false
            }
        }
    }

    // Request permissions on first load
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    com.rescuemate.ui.components.CosmicScaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CosmicTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    com.rescuemate.ui.components.CosmicHeader(
                        text = stringResource(R.string.live_location_title)
                    )
                    com.rescuemate.ui.components.CosmicSubHeader(
                        text = stringResource(R.string.real_time_tracking)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge
            if (isSharing) {
                val infiniteTransition = rememberInfiniteTransition(label = "sharing")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Surface(
                    color = CosmicPrimary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(scale)
                                .background(
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = stringResource(R.string.location_sharing_active),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            } else {
                Surface(
                    color = CosmicCard,
                    shape = MaterialTheme.shapes.small,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Text(
                        text = stringResource(R.string.location_sharing_inactive),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = CosmicTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = CosmicCard,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                when {
                    !locationPermissionsState.allPermissionsGranted -> {
                        // Permission not granted state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = CosmicTextSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Location Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = CosmicTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enable location access to view and share your real-time position",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            com.rescuemate.ui.components.CosmicButton(
                                text = "Grant Location Permission",
                                onClick = {
                                    locationPermissionsState.launchMultiplePermissionRequest()
                                },
                                isPrimary = true
                            )
                        }
                    }
                    isLoadingLocation -> {
                        // Loading state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = CosmicPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Getting your location...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                    // Check for missing API Key or Invalid Key (if map loads but is blank, usually key issue)
                    com.rescuemate.BuildConfig.GOOGLE_MAPS_API_KEY.isBlank() -> {
                         Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Google Maps API Key Missing",
                                style = MaterialTheme.typography.titleMedium,
                                color = CosmicTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please add GOOGLE_MAPS_API_KEY to your .env or local.properties file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    currentLocation != null -> {
                        // Map display
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(currentLocation!!, 15f)
                            },
                            properties = MapProperties(
                                mapType = MapType.NORMAL,
                                isMyLocationEnabled = true
                            ),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = true,
                                zoomControlsEnabled = true,
                                compassEnabled = true
                            )
                        ) {
                            Marker(
                                state = MarkerState(position = currentLocation!!),
                                title = "You are here",
                                snippet = locationAddress
                            )
                        }

                        // Coordinates Overlay
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CosmicCard.copy(alpha = 0.95f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = CosmicPrimary
                                )
                                Column {
                                    Text(
                                        text = "Your Location",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicTextSecondary,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        text = locationAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CosmicTextPrimary
                                    )
                                }
                            }
                        }

                        // Last Updated overlay (when sharing)
                        if (isSharing) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = CosmicCard.copy(alpha = 0.95f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = CosmicPrimary
                                    )
                                    Text(
                                        text = "Updated just now • High accuracy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CosmicTextPrimary
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Error state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CosmicTextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to get location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            com.rescuemate.ui.components.CosmicButton(
                                text = "Retry",
                                onClick = { retryTrigger++ },
                                isPrimary = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share Location Button
            com.rescuemate.ui.components.CosmicButton(
                text = if (isSharing) {
                    stringResource(R.string.stop_sharing_location)
                } else {
                    stringResource(R.string.share_my_location)
                },
                onClick = { isSharing = !isSharing },
                modifier = Modifier.fillMaxWidth(),
                isPrimary = !isSharing
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSharing) {
                    stringResource(R.string.location_shared_desc)
                } else {
                    stringResource(R.string.location_sharing_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = CosmicTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

