package com.example.parkspot.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.parkspot.R
import com.example.parkspot.location.LocationHelper
import com.example.parkspot.model.ParkingRecord
import com.example.parkspot.util.MapsHelper
import com.example.parkspot.widget.ParkingWidgetProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkSpotApp(
    viewModel: ParkingViewModel,
    initialIntent: Intent? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeParking by viewModel.activeParking.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val distanceToCar by viewModel.distanceToCar.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showWidgetGuideDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoUri by remember { mutableStateOf<String?>(null) }

    var inputFloor by remember { mutableStateOf("") }
    var inputSpot by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }

    // Camera capture launcher
    var photoUriForCapture by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onPhotoTaken(success, inputFloor, inputSpot, inputNotes)
    }

    // Permission launcher for location + camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (!locationGranted) {
            Toast.makeText(context, "Location permission helps locate your car accurately", Toast.LENGTH_LONG).show()
        }
    }

    // Request permissions on start
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
        )
    }

    // Handle initial intent actions (e.g. from Widget buttons)
    LaunchedEffect(initialIntent) {
        when (initialIntent?.action) {
            ParkingWidgetProvider.ACTION_TAKE_PICTURE -> {
                val uri = viewModel.createPhotoUri()
                photoUriForCapture = uri
                cameraLauncher.launch(uri)
            }
            ParkingWidgetProvider.ACTION_MARK_LOCATION -> {
                viewModel.markCurrentLocation()
            }
        }
    }

    // Show status messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_car),
                            contentDescription = "ParkSpot Logo",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ParkSpot",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showWidgetGuideDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Widget Guide",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Parking History",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14171C)
                )
            )
        },
        containerColor = Color(0xFF0F1216)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeParking != null && activeParking!!.isActive) {
                    ActiveParkingCard(
                        parking = activeParking!!,
                        distanceToCar = distanceToCar,
                        onNavigate = {
                            MapsHelper.navigateToParking(
                                context,
                                activeParking!!.latitude,
                                activeParking!!.longitude,
                                walkingMode = true
                            )
                        },
                        onShare = {
                            MapsHelper.shareParkingLocation(
                                context,
                                activeParking!!.latitude,
                                activeParking!!.longitude,
                                activeParking!!.address,
                                activeParking!!.notes
                            )
                        },
                        onOpenPhoto = { uri ->
                            fullScreenPhotoUri = uri
                        },
                        onEditNotes = {
                            inputFloor = activeParking!!.floorLevel ?: ""
                            inputSpot = activeParking!!.spotNumber ?: ""
                            inputNotes = activeParking!!.notes ?: ""
                            showNotesDialog = true
                        },
                        onSetTimer = {
                            showTimerDialog = true
                        },
                        onClear = {
                            viewModel.clearActiveParking()
                        }
                    )
                } else {
                    // Empty / Idle State matching screenshot
                    ServicePromoCard(
                        onTakePicture = {
                            val uri = viewModel.createPhotoUri()
                            photoUriForCapture = uri
                            cameraLauncher.launch(uri)
                        },
                        onMarkLocation = {
                            viewModel.markCurrentLocation(
                                floor = inputFloor.ifBlank { null },
                                spot = inputSpot.ifBlank { null },
                                notes = inputNotes.ifBlank { null }
                            )
                        }
                    )

                    // Indoor parking details helper
                    IndoorDetailsExpander(
                        floor = inputFloor,
                        spot = inputSpot,
                        notes = inputNotes,
                        onFloorChange = { inputFloor = it },
                        onSpotChange = { inputSpot = it },
                        onNotesChange = { inputNotes = it }
                    )

                    // Widget promo card with 1-tap pin request
                    WidgetPromoBanner(
                        onClickGuide = {
                            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                appWidgetManager.isRequestPinAppWidgetSupported
                            ) {
                                val provider = android.content.ComponentName(
                                    context,
                                    com.example.parkspot.widget.ParkingWidgetProvider::class.java
                                )
                                appWidgetManager.requestPinAppWidget(provider, null, null)
                            } else {
                                showWidgetGuideDialog = true
                            }
                        }
                    )
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2126)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Acquiring GPS location...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNotesDialog) {
        EditNotesDialog(
            floor = inputFloor,
            spot = inputSpot,
            notes = inputNotes,
            onDismiss = { showNotesDialog = false },
            onSave = { f, s, n ->
                viewModel.updateNotes(f, s, n)
                showNotesDialog = false
            }
        )
    }

    if (showTimerDialog) {
        ParkingTimerDialog(
            onDismiss = { showTimerDialog = false },
            onSetTimer = { minutes ->
                viewModel.setParkingTimer(minutes)
                showTimerDialog = false
            }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            history = history,
            onDismiss = { showHistoryDialog = false },
            onNavigate = { lat, lon ->
                MapsHelper.navigateToParking(context, lat, lon)
            },
            onDelete = { id ->
                viewModel.deleteHistory(id)
            }
        )
    }

    if (showWidgetGuideDialog) {
        WidgetGuideDialog(onDismiss = { showWidgetGuideDialog = false })
    }

    fullScreenPhotoUri?.let { uri ->
        FullScreenPhotoDialog(
            photoUri = uri,
            onDismiss = { fullScreenPhotoUri = null }
        )
    }
}

/**
 * Service card reproducing the UI provided in the user's screenshot
 */
@Composable
fun ServicePromoCard(
    onTakePicture: () -> Unit,
    onMarkLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D22)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2B2F38)))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // City skyline vector at the bottom
            Image(
                painter = painterResource(id = R.drawable.ic_city_skyline),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "PARKING LOCATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E939C),
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Parking Location Service",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Record your vehicle's parking location, making it easier to be found later. Mobile data and GPS must be turned on to mark your vehicle's location. For indoor parking, use the \"Take a picture\" function, as indoor location recording isn't currently available.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA0A5B0),
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Bottom row with actual icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Take a picture button
                    Button(
                        onClick = onTakePicture,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF282C35),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Take a picture",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Take a picture",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Mark location button
                    Button(
                        onClick = onMarkLocation,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF282C35),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mark_location),
                            contentDescription = "Mark location",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark location",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Active parking hero card with photo, GPS address, distance, Google Maps navigation, and actions
 */
@Composable
fun ActiveParkingCard(
    parking: ParkingRecord,
    distanceToCar: Float?,
    onNavigate: () -> Unit,
    onShare: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onEditNotes: () -> Unit,
    onSetTimer: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D22)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2D323C)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF34A853), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VEHICLE PARKED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34A853),
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = LocationHelper.formatDurationSince(parking.timestamp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF9CA3AF)
                    )
                )
            }

            // Photo section if available
            if (!parking.photoUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF242830))
                        .clickable { onOpenPhoto(parking.photoUri) }
                ) {
                    AsyncImage(
                        model = parking.photoUri,
                        contentDescription = "Parking Spot Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Zoom indicator pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap to enlarge",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Address and coordinates info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = parking.address.ifBlank { "Recorded Location" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_pin),
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = MapsHelper.formatCoordinates(parking.latitude, parking.longitude),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }

                if (parking.accuracyMeters > 0) {
                    Text(
                        text = "GPS accuracy: ±${parking.accuracyMeters.toInt()}m • Parked at ${LocationHelper.formatTime(parking.timestamp)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            // Live distance indicator
            if (distanceToCar != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_directions),
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Distance to vehicle: ${LocationHelper.formatDistance(distanceToCar)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF1F5F9)
                                )
                            )
                            val walkMin = (distanceToCar / 75f).toInt() // avg 75m/min walking speed
                            Text(
                                text = if (walkMin <= 1) "About 1 min walk" else "About $walkMin mins walk",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            // Indoor / notes badge if provided
            val hasIndoor = !parking.floorLevel.isNullOrBlank() || !parking.spotNumber.isNullOrBlank() || !parking.notes.isNullOrBlank()
            if (hasIndoor) {
                Surface(
                    color = Color(0xFF232730),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notes),
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Indoor Parking Details",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!parking.floorLevel.isNullOrBlank()) {
                            Text(
                                text = "Floor / Level: ${parking.floorLevel}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                        }
                        if (!parking.spotNumber.isNullOrBlank()) {
                            Text(
                                text = "Bay / Pillar #: ${parking.spotNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                        }
                        if (!parking.notes.isNullOrBlank()) {
                            Text(
                                text = "Note: ${parking.notes}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD1D5DB))
                            )
                        }
                    }
                }
            }

            // PRIMARY BUTTON: Navigate in Google Maps
            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_navigation),
                    contentDescription = "Navigate",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Navigate in Google Maps",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Action Grid: Share, Notes, Timer, Found Car
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5E7EB))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share", fontSize = 12.sp)
                }

                // Edit Notes
                OutlinedButton(
                    onClick = onEditNotes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5E7EB))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notes),
                        contentDescription = "Notes",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Notes", fontSize = 12.sp)
                }

                // Timer
                OutlinedButton(
                    onClick = onSetTimer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE5E7EB))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer),
                        contentDescription = "Timer",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Timer", fontSize = 12.sp)
                }
            }

            // CLEAR / FOUND CAR BUTTON
            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2128),
                    contentColor = Color(0xFF4ADE80)
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF22C55E).copy(alpha = 0.3f)))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = "Found Car",
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I Found My Car (Clear Spot)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * Collapsible indoor details inputs for floor, slot, and notes
 */
@Composable
fun IndoorDetailsExpander(
    floor: String,
    spot: String,
    notes: String,
    onFloorChange: (String) -> Unit,
    onSpotChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF242830)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notes),
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Optional Indoor / Garage Details",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }

                Text(
                    text = if (expanded) "Hide" else "Add",
                    color = Color(0xFF60A5FA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = floor,
                            onValueChange = onFloorChange,
                            label = { Text("Level / Floor (e.g. B2)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF333A46),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = spot,
                            onValueChange = onSpotChange,
                            label = { Text("Bay / Pillar #", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF333A46),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        label = { Text("Extra notes (e.g. Near yellow exit)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF333A46),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * Promo card for placing the home screen widget
 */
@Composable
fun WidgetPromoBanner(onClickGuide: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickGuide() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF262C38)))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add Home Screen Widget",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Place the 1-tap parking widget right on your home screen.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8)
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dialog to edit indoor notes
 */
@Composable
fun EditNotesDialog(
    floor: String,
    spot: String,
    notes: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var f by remember { mutableStateOf(floor) }
    var s by remember { mutableStateOf(spot) }
    var n by remember { mutableStateOf(notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Parking Details", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = f,
                    onValueChange = { f = it },
                    label = { Text("Level / Floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = s,
                    onValueChange = { s = it },
                    label = { Text("Bay / Pillar #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text("Additional Notes") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(f, s, n) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E2126)
    )
}

/**
 * Dialog to set a meter timer reminder
 */
@Composable
fun ParkingTimerDialog(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parking Meter Reminder", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select when your parking ticket expires to keep track:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                    OutlinedButton(
                        onClick = { onSetTimer(mins) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$mins minutes")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1E2126)
    )
}

/**
 * Dialog displaying past parking sessions
 */
@Composable
fun HistoryDialog(
    history: List<ParkingRecord>,
    onDismiss: () -> Unit,
    onNavigate: (Double, Double) -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D22))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parking History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved parking history yet.",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history, key = { it.id }) { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF22262E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    if (!record.photoUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = record.photoUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(Color(0xFF2E333D), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_car),
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.address.ifBlank { "Parked Location" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = LocationHelper.formatDate(record.timestamp),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8)
                                            )
                                        )
                                    }

                                    // Maps Navigation icon button
                                    IconButton(onClick = { onNavigate(record.latitude, record.longitude) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_navigation),
                                            contentDescription = "Navigate in Google Maps",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Delete icon button
                                    IconButton(onClick = { onDelete(record.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Widget placement instructions dialog
 */
@Composable
fun WidgetGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to Add the Widget", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Go to your phone's Home Screen.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "2. Long press on any empty space.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "3. Tap 'Widgets' and scroll to 'ParkSpot'.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "4. Drag the 'Parking Location' card to your home screen.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enjoy instant 1-tap parking location saving and Google Maps navigation from your home screen!",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        },
        containerColor = Color(0xFF1E2126)
    )
}

/**
 * Fullscreen photo viewer
 */
@Composable
fun FullScreenPhotoDialog(
    photoUri: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Full Screen Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
