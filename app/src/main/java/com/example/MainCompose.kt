package com.example

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarConsoleDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe StateFlow properties
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val currentIdx by viewModel.currentSongIndex.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val beatStep by viewModel.beatStep.collectAsState()

    val isBluetoothOn by viewModel.isBluetoothOn.collectAsState()
    val btDevices by viewModel.bluetoothDevices.collectAsState()
    val connectedBtDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isScanningBt by viewModel.isScanningBluetooth.collectAsState()

    val isUsbPluggedIn by viewModel.isUsbPluggedIn.collectAsState()
    val usbScanning by viewModel.usbScanning.collectAsState()

    val voiceLogs by viewModel.voiceLogs.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val speechTranscription by viewModel.speechTranscription.collectAsState()

    val isNavigating by viewModel.isNavigating.collectAsState()
    val navDest by viewModel.navigationDestination.collectAsState()
    val navInstructions by viewModel.navInstructions.collectAsState()
    val navDistance by viewModel.navDistanceMiles.collectAsState()
    val navEtaMin by viewModel.navMinutesEta.collectAsState()

    // Helper Dialog states
    var showAddSongDialog by remember { mutableStateOf(false) }
    var inputSongTitle by remember { mutableStateOf("") }
    var inputSongArtist by remember { mutableStateOf("") }
    
    // Quick Chat command simulation options
    var typedCommand by remember { mutableStateOf("") }

    // Audio recording permission launcher
    var hasRecordAudioPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasRecordAudioPermission = isGranted
            if (isGranted) {
                viewModel.startNativeVoiceRecording(context)
            } else {
                Toast.makeText(context, "Microphone permission is required for real voice detection.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Current Song metadata computed safely
    val currentSong = playlist.getOrNull(currentIdx)

    // Layout containers: Column scrollable dashboard for vertical layouts, side by side for tablets/car wide screens
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberDark,
        topBar = {
            Column(
                modifier = Modifier
                    .background(CyberDark)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                // Status Bar Overlay (Matching Editorial Aesthetic details)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "10:42 PM",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.graphicsLayer(alpha = 0.8f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer(alpha = 0.8f)
                    ) {
                        // Bluetooth icon custom retro rune representation
                        Text(
                            text = "ᛒ",
                            color = if (isBluetoothOn) NeonCyan else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        // USB system indicator
                        Text(
                            text = "USB",
                            color = if (isUsbPluggedIn) GlowGreen else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        // Cellular signal
                        Text(
                            text = "📶",
                            fontSize = 11.sp
                        )
                        // Battery indicator
                        Text(
                            text = "🔋",
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Editorial Header Navigation Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left navigation button rounded container
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyberCardAccent.copy(alpha = 0.4f))
                            .clickable { /* Minimize vibe */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Console",
                            tint = RetroPink,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Centered Text Details matching editorial style
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isUsbPluggedIn) "PLAYING FROM USB DRIVE" else "CONSOLE MONITORING",
                            color = RetroPink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("app_brand_text")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isUsbPluggedIn) "Local Favorites" else "DriveBeat Offline",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right navigation option menu button container
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyberCardAccent.copy(alpha = 0.4f))
                            .clickable { /* Settings options */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "System Options",
                            tint = RetroPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    // Vintage retro synth grid lines
                    Brush.verticalGradient(
                        colors = listOf(CyberDark, CyberDark, GridPurple)
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ------------------ TOP ROW STATUS STATS ------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bluetooth Status Banner Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bluetooth icon custom colored vector representation as a placeholder or real icon usage
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (connectedBtDeviceName != null) NeonCyan.copy(alpha = 0.2f) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "BT",
                                color = if (connectedBtDeviceName != null) NeonCyan else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Bluetooth Audio", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = connectedBtDeviceName ?: "Disconnected",
                                color = if (connectedBtDeviceName != null) NeonCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // USB Status Banner Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isUsbPluggedIn) GlowGreen.copy(alpha = 0.2f) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "USB",
                                color = if (isUsbPluggedIn) GlowGreen else Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Storage Mount", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = if (usbScanning) "Scanning..." else if (isUsbPluggedIn) "USB Read Active" else "USB Ejected",
                                color = if (isUsbPluggedIn) GlowGreen else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ------------------ AUTONOMOUS GUIDANCE GPS NAVIGATION HUD ------------------
            AnimatedVisibility(
                visible = isNavigating,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUTONOMOUS NAVIGATION HUD",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.endNavigation() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = Color.White, fontSize = 13.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Circular cyber radar compass canvas mock animation
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .drawBehind {
                                        // Draw compass circles
                                        drawCircle(color = NeonCyan.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                                        drawCircle(color = NeonCyan.copy(alpha = 0.1f))
                                        // Custom directional radar vector arrow line rotating
                                        val angleRad = (beatStep * 15) * Math.PI / 180.0
                                        val arrowX = center.x + (size.width / 2.3f) * Math.cos(angleRad).toFloat()
                                        val arrowY = center.y + (size.height / 2.3f) * Math.sin(angleRad).toFloat()
                                        drawLine(
                                            color = NeonCyan,
                                            start = center,
                                            end = Offset(arrowX, arrowY),
                                            strokeWidth = 4f
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "GPS",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = navDest?.uppercase() ?: "NAV TARGET DEFAULT",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = navInstructions,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "📍 ${"%.1f".format(navDistance)} mi",
                                        color = BrightYellow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "⏱️ $navEtaMin mins ETA",
                                        color = BrightYellow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------ CORE INTEGRATED NOW PLAYING & COMPOSE RETRO CAT ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RetroPink.copy(alpha = 0.4f))
            ) {
                // Single-screen car dashboard splits cleanly: Left is pixel meow, Right is details & dials
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VIBING CAT NOW PLAYING DETECTOR",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Retrocade canvas framing container of dancing feline (Editorial style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CyberCard)
                            .border(4.dp, CyberCardAccent, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Floating Music Notes (Editorial features)
                        Text(
                            text = "🎵",
                            color = RetroPink,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 12.dp)
                                .graphicsLayer(alpha = 0.4f)
                        )
                        Text(
                            text = "〰",
                            color = RetroPink,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 12.dp, start = 10.dp)
                                .graphicsLayer(alpha = 0.3f)
                        )

                        PixelCat(
                            isPlaying = isPlaying,
                            beatStep = beatStep,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .testTag("dancing_pixel_cat")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Track details row (Editorial Style)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentSong?.title ?: "No Song Selected",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("song_title_display")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            color = RetroPink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        
                        // USB File Path Indicator
                        if (currentSong != null && currentSong.isFromUsb) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📁 Path: " + (currentSong.path.ifEmpty { "Virtual USB Drive Block" }),
                                color = GlowGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated neon frequency music visualizer bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (i in 0 until 12) {
                            // High energy multipliers when playing, subtle idle tremors when paused
                            val activeVal = if (isPlaying) {
                                val phase = (beatStep + i) % 5
                                when (phase) {
                                    0 -> 0.8f
                                    1 -> 0.4f
                                    2 -> 0.9f
                                    3 -> 0.2f
                                    else -> 0.6f
                                }
                            } else {
                                0.12f
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .fillMaxHeight(activeVal)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(NeonCyan, RetroPink)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // MUSIC PLAYER CORE NAVIGATION DIALS / DIALS BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        IconButton(
                            onClick = { viewModel.prevTrack() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CyberCardAccent)
                                .testTag("prev_button")
                        ) {
                            Text("⏮", color = Color.White, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Large Center Play / Pause toggle as a modern editorial squircle
                        IconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(RetroPink)
                                .testTag("play_pause_button")
                        ) {
                            Text(
                                text = if (isPlaying) "⏸" else "▶",
                                color = Color(0xFF381E72),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.offset(x = if (isPlaying) 0.dp else 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Next Button
                        IconButton(
                            onClick = { viewModel.nextTrack() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(CyberCardAccent)
                                .testTag("next_button")
                        ) {
                            Text("⏭", color = Color.White, fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vol adjust dial slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔊", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Slider(
                            value = volume.toFloat(),
                            onValueChange = { viewModel.adjustVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("volume_slider")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$volume%",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }

            // ------------------ INTUITIVE VOICE CONTROLS & AI GUIDANCE HUD ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title info indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HANDS-FREE AI VOICE RADAR",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        // Active voice listening pulse
                        if (isVoiceListening) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(GlowGreen)
                            )
                        } else {
                            Text(
                                text = "ACTIVE SENSE",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrolling radar terminal text log
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF07040B))
                            .border(1.dp, Color.White.copy(alpha = 0.1f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            reverseLayout = true // Keeping the newest commands at the bottom
                        ) {
                            itemsIndexed(voiceLogs.reversed()) { _, log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (log.sender == "DRIVER") Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (log.sender == "DRIVER") NeonCyan.copy(alpha = 0.15f) else CyberCardAccent)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "[${log.sender}] ${log.text}",
                                            color = if (log.sender == "DRIVER") NeonCyan else TextPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real Recording Activation button and Manual Typing simulation side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating micro button
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceListening) GlowGreen else neonVoiceGlow()
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(56.dp)
                                .testTag("voice_command_button"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isVoiceListening) "🗣️ LISTENING" else "🎙️ TAP TO SPEAK",
                                    color = CyberDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Simulated test actions drop or quick chat text box
                        OutlinedTextField(
                            value = typedCommand,
                            onValueChange = { typedCommand = it },
                            placeholder = { Text("Or type command...", color = Color.Gray, fontSize = 11.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF07040B),
                                unfocusedContainerColor = Color(0xFF07040B),
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("sim_command_input")
                        )
                        
                        IconButton(
                            onClick = {
                                if (typedCommand.isNotBlank()) {
                                    viewModel.handleVoiceInput(typedCommand)
                                    typedCommand = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonCyan)
                                .testTag("sim_command_send")
                        ) {
                            Text("▶", color = CyberDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    if (speechTranscription.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🎤 Voice Status: $speechTranscription",
                            color = BrightYellow,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // One-touch Quick Command Shortcut Chips (Increments accessibility significantly)
                    Text(
                        text = "QUICK SHIELD CONTROL CHIPS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickChips = listOf(
                            "Navigate to nearest EV charging station" to "📍 EV Station",
                            "Play Starry Drive" to "🎵 Play Starry",
                            "Mute console audio" to "🔇 Mute",
                            "Turn up the volume" to "🔊 Vol Up",
                            "Sync Bluetooth handsfree" to "🔗 Sync BT",
                            "What time is it?" to "⌚ Ask Clock"
                        )
                        
                        quickChips.forEach { (cmd, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CyberCardAccent)
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .clickable { viewModel.handleVoiceInput(cmd) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ------------------ LOCAL USB DRIVE SCANNER MODULE ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GlowGreen.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💾", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "USB STORAGE DRIVE MODULE",
                                color = GlowGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        // Storage mounting toggler
                        Switch(
                            checked = isUsbPluggedIn,
                            onCheckedChange = { viewModel.toggleUsbDrivePlug() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GlowGreen,
                                checkedTrackColor = GlowGreen.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("usb_mount_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The system reads offline songs from an active USB drive. Plug in the meow-disk to access track storage.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isUsbPluggedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Native Storage Scan
                            Button(
                                onClick = { viewModel.scanLocalDeviceStorage(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCardAccent),
                                border = BorderStroke(1.dp, GlowGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scan Phone Files", color = GlowGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }

                            // Write Custom Manual Track
                            Button(
                                onClick = { showAddSongDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("usb_import_song_button")
                            ) {
                                Text("Import Song File", color = CyberDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ PLUG IN USB STORAGE TO IMPORT OR SCAN LOCAL AUDIO FILES",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ------------------ CAR MEDIA PLAYLIST MANAGER ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CAR PLAYLIST STACK (${playlist.size} Tracks)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        playlist.forEachIndexed { idx, song ->
                            val isSelected = idx == currentIdx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) RetroPink.copy(alpha = 0.12f) else CyberCardAccent)
                                    .border(
                                        1.dp,
                                        if (isSelected) RetroPink else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.playTrack(idx) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isSelected && isPlaying) "⚡" else (idx + 1).toString(),
                                        color = if (isSelected) RetroPink else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = song.title,
                                            color = if (isSelected) RetroPink else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Origin tag (USB vs Built-in)
                                    if (song.isFromUsb) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(GlowGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("USB", color = GlowGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Text(
                                        text = "${song.durationSeconds / 60}:${String.format("%02d", song.durationSeconds % 60)}",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------ NEON BLUETOOTH PAIRING DECK ------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📻", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BLUETOOTH HARDWARE MANAGEMENT",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        Switch(
                            checked = isBluetoothOn,
                            onCheckedChange = { viewModel.toggleBluetoothState() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("bluetooth_state_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isBluetoothOn) {
                        Button(
                            onClick = { viewModel.startBluetoothScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("bt_scan_button")
                        ) {
                            Text(
                                text = if (isScanningBt) "SCANNING HOSTS..." else "SCAN FOR CAR AUDIO LINKS",
                                color = CyberDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Connectable Bluetooth list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            btDevices.forEach { device ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (device.isConnected) NeonCyan.copy(alpha = 0.1f) else CyberCardAccent)
                                        .clickable { viewModel.connectBluetoothDevice(device) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = device.name,
                                            color = if (device.isConnected) NeonCyan else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = device.address + " | Signal: ${device.signalStrengthDb} dBm",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (device.isConnected) NeonCyan else Color.DarkGray)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (device.isConnected) "COUPLED" else "CONNECT",
                                            color = CyberDark,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BLUETOOTH MODULE SHUTDOWN\nTurn on Bluetooth switch to search car audio.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ------------------ EDITORIAL BOTTOM SHORTCUTS ------------------
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shortcut 1: USB Library
                Column(
                    modifier = Modifier.clickable { /* Simulated library action */ },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎶", fontSize = 20.sp)
                    Text(
                        text = "USB LIBRARY",
                        color = RetroPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Shortcut 2: Pair Device
                Column(
                    modifier = Modifier
                        .clickable { viewModel.toggleBluetoothState() }
                        .graphicsLayer(alpha = if (isBluetoothOn) 1.0f else 0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("ᛒ", fontSize = 20.sp, color = if (isBluetoothOn) NeonCyan else Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        text = "PAIR DEVICE",
                        color = if (isBluetoothOn) NeonCyan else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Shortcut 3: Car Sync Settings
                Column(
                    modifier = Modifier
                        .clickable { /* Settings action */ }
                        .graphicsLayer(alpha = 0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⚙️", fontSize = 20.sp, color = Color.White)
                    Text(
                        text = "CAR SYNC",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Simulated Home Indicator overlay bar (Matching bottom of Editorial specification)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }
    }

    // ------------------ USB MANUALLY SONG DISK DIALOG ------------------
    if (showAddSongDialog) {
        AlertDialog(
            onDismissRequest = { showAddSongDialog = false },
            title = {
                Text(
                    "WRITE CUSTOM USB AUDIO FILE",
                    color = GlowGreen,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Input metadata coordinates to simulate adding local tracks on the USB meow-drive.",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = inputSongTitle,
                        onValueChange = { inputSongTitle = it },
                        label = { Text("Song Title*", color = GlowGreen) },
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowGreen,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_song_title_input")
                    )

                    OutlinedTextField(
                        value = inputSongArtist,
                        onValueChange = { inputSongArtist = it },
                        label = { Text("Artist Name") },
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowGreen,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_song_artist_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputSongTitle.isNotBlank()) {
                            viewModel.addManualUsbSong(inputSongTitle, inputSongArtist)
                            inputSongTitle = ""
                            inputSongArtist = ""
                            showAddSongDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("Mount Track", color = CyberDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = CyberCard
        )
    }
}

// Glowing colors helper for Voice command system
@Composable
private fun neonVoiceGlow(): Color {
    val infiniteTransition = rememberInfiniteTransition()
    val glowColorAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    return NeonCyan
}
