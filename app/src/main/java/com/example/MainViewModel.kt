package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Representing a Song file metadata
 */
data class CarSong(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val isFromUsb: Boolean,
    val path: String = ""
)

/**
 * Representing Bluetooth paired devices
 */
data class BluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val signalStrengthDb: Int
)

/**
 * Log entries for AI Console interactions
 */
data class ConsoleLog(
    val sender: String, // "DRIVER" or "SYSTEM"
    val text: String,
    val timestamp: String = "09:17 AM"
)

class MainViewModel : ViewModel() {
    private val TAG = "MainViewModel"
    private val synth = SoundGenerator()

    // --- State Managers (StateFlows) ---

    // Media & Active Player State
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _beatStep = MutableStateFlow(0)
    val beatStep: StateFlow<Int> = _beatStep.asStateFlow()

    // System HUD Volume
    private val _volume = MutableStateFlow(80)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // Bluetooth States
    private val _isBluetoothOn = MutableStateFlow(true)
    val isBluetoothOn: StateFlow<Boolean> = _isBluetoothOn.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDevice>> = _bluetoothDevices.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isScanningBluetooth = MutableStateFlow(false)
    val isScanningBluetooth: StateFlow<Boolean> = _isScanningBluetooth.asStateFlow()

    // Virtual USB Drive states
    private val _isUsbPluggedIn = MutableStateFlow(false)
    val isUsbPluggedIn: StateFlow<Boolean> = _isUsbPluggedIn.asStateFlow()

    private val _usbScanning = MutableStateFlow(false)
    val usbScanning: StateFlow<Boolean> = _usbScanning.asStateFlow()

    // All available media tracks (reloads depending on USB connection)
    private val _playlist = MutableStateFlow<List<CarSong>>(emptyList())
    val playlist: StateFlow<List<CarSong>> = _playlist.asStateFlow()

    // Voice assistant terminals interaction logs
    private val _voiceLogs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val voiceLogs: StateFlow<List<ConsoleLog>> = _voiceLogs.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _speechTranscription = MutableStateFlow("")
    val speechTranscription: StateFlow<String> = _speechTranscription.asStateFlow()

    // Active Navigation HUD state
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _navigationDestination = MutableStateFlow<String?>(null)
    val navigationDestination: StateFlow<String?> = _navigationDestination.asStateFlow()

    private val _navInstructions = MutableStateFlow("Drive safely. AI Radar listening...")
    val navInstructions: StateFlow<String> = _navInstructions.asStateFlow()

    private val _navDistanceMiles = MutableStateFlow(0.0)
    val navDistanceMiles: StateFlow<Double> = _navDistanceMiles.asStateFlow()

    private val _navMinutesEta = MutableStateFlow(0)
    val navMinutesEta: StateFlow<Int> = _navMinutesEta.asStateFlow()

    // Real Native Speech recognizer helper
    private var speechRecognizer: SpeechRecognizer? = null

    // Prebuilt Core Deck
    private val internalSongs = listOf(
        CarSong("Starry Drive", "Neon Kitty", 184, isFromUsb = false),
        CarSong("Cyber Cat Bounce", "Whisker Waves", 145, isFromUsb = false),
        CarSong("Grid Runner", "Laser Paws", 210, isFromUsb = false),
        CarSong("Midnight Purr", "Lofi Meow", 120, isFromUsb = false)
    )

    private val usbSongsTemplate = listOf(
        CarSong("Bass Whiskers [USB]", "Glitch Feline", 195, isFromUsb = true, path = "/usb/music/whiskers.wav"),
        CarSong("Pixel Highway [USB]", "Chiptune Kitty", 168, isFromUsb = true, path = "/usb/music/highway.wav"),
        CarSong("Feline Beats [USB]", "Dr. Purr", 205, isFromUsb = true, path = "/usb/music/feline.wav")
    )

    init {
        // Initialize playlist with core songs
        _playlist.value = internalSongs

        // Prepopulate paired audio links
        _bluetoothDevices.value = listOf(
            BluetoothDevice("Chevrolet HandsFree Link", "00:1E:AA:33:55:12", isConnected = false, signalStrengthDb = -62),
            BluetoothDevice("Tesla Model 3 Stereo", "1A:2B:3C:4D:5E:6F", isConnected = false, signalStrengthDb = -45),
            BluetoothDevice("Sony XAV Car Console", "4F:F5:23:44:E1:90", isConnected = false, signalStrengthDb = -72),
            BluetoothDevice("Harmon Kardon BT Pack", "E1:10:9A:C3:FF:1B", isConnected = false, signalStrengthDb = -84)
        )

        _voiceLogs.value = listOf(
            ConsoleLog("SYSTEM", "DriveBeat console ready. Bluetooth, virtual USB, and Voice trigger activated."),
            ConsoleLog("SYSTEM", "Tip: Ask \"Navigate to nearest recharging spot\" or \"play Grid Runner\"!")
        )

        // Wire Beat callback to visual anim steps
        synth.onBeatCallback = { step ->
            _beatStep.value = step
        }
        synth.setVolume(_volume.value / 100f)
    }

    // --- Audio Control Functions ---

    fun togglePlayback() {
        val currentPlayState = !_isPlaying.value
        _isPlaying.value = currentPlayState
        
        if (currentPlayState) {
            val song = _playlist.value.getOrNull(_currentSongIndex.value)
            if (song != null) {
                synth.start(song.title)
            }
        } else {
            synth.stop()
        }
    }

    fun playTrack(index: Int) {
        if (index in 0 until _playlist.value.size) {
            _currentSongIndex.value = index
            _isPlaying.value = true
            val song = _playlist.value[index]
            synth.start(song.title)
            addLog("SYSTEM", "Playing: ${song.title} by ${song.artist}")
        }
    }

    fun nextTrack() {
        val size = _playlist.value.size
        if (size > 0) {
            val nextIdx = (_currentSongIndex.value + 1) % size
            playTrack(nextIdx)
        }
    }

    fun prevTrack() {
        val size = _playlist.value.size
        if (size > 0) {
            var prevIdx = _currentSongIndex.value - 1
            if (prevIdx < 0) prevIdx = size - 1
            playTrack(prevIdx)
        }
    }

    fun adjustVolume(newVolumePercent: Int) {
        val bounded = newVolumePercent.coerceIn(0, 100)
        _volume.value = bounded
        synth.setVolume(bounded / 100f)
    }

    // --- USB Drive Simulation & Storage Scan ---

    fun toggleUsbDrivePlug() {
        if (_isUsbPluggedIn.value) {
            // Unplug action
            _isUsbPluggedIn.value = false
            _playlist.value = internalSongs
            _currentSongIndex.value = 0
            
            // Stop playback if playing a USB song currently
            if (_isPlaying.value) {
                synth.stop()
                _isPlaying.value = false
            }
            
            addLog("SYSTEM", "USB Drive unmounted. Scan cleared.")
        } else {
            // Plug in action
            _isUsbPluggedIn.value = true
            _usbScanning.value = true
            
            viewModelScope.launch {
                kotlinx.coroutines.delay(1200) // Simulating scanning hardware drive
                _usbScanning.value = false
                _playlist.value = internalSongs + usbSongsTemplate
                addLog("SYSTEM", "USB Drive parsed! Scan found 3 local songs.")
            }
        }
    }

    /**
     * Scan real device Downloads / Music directory for local audio
     */
    fun scanLocalDeviceStorage(context: Context) {
        _usbScanning.value = true
        addLog("SYSTEM", "Scanning device directories for audio tracks...")
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            
            // Search standard audio directories
            val foundSongs = mutableListOf<CarSong>()
            try {
                val downloadsDir = context.getExternalFilesDir(null)
                if (downloadsDir != null && downloadsDir.exists()) {
                    downloadsDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".mp3", true) || file.name.endsWith(".wav", true)) {
                            foundSongs.add(
                                CarSong(
                                    title = file.nameWithoutExtension,
                                    artist = "Local File",
                                    durationSeconds = 180,
                                    isFromUsb = true,
                                    path = file.absolutePath
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Storage scan limitation", e)
            }
            
            _usbScanning.value = false
            if (foundSongs.isNotEmpty()) {
                _playlist.value = _playlist.value + foundSongs
                addLog("SYSTEM", "Scan finished. Found ${foundSongs.size} actual music files.")
            } else {
                addLog("SYSTEM", "Scan finished. No local files found. Virtual USB active.")
            }
        }
    }

    /**
     * Add a custom imported USB song manually
     */
    fun addManualUsbSong(title: String, artist: String) {
        val dummy = CarSong(
            title = "$title [USB]",
            artist = artist.ifEmpty { "Retro Collector" },
            durationSeconds = 180,
            isFromUsb = true
        )
        _playlist.value = _playlist.value + dummy
        addLog("SYSTEM", "Added custom USB track: $title")
    }

    // --- Bluetooth Pairing Controls ---

    fun toggleBluetoothState() {
        val prev = _isBluetoothOn.value
        _isBluetoothOn.value = !prev
        if (prev) {
            // Disconnect current paired link
            _connectedDeviceName.value = null
            addLog("SYSTEM", "Bluetooth module deactivated.")
        } else {
            addLog("SYSTEM", "Bluetooth module activated.")
        }
    }

    fun startBluetoothScan() {
        if (!_isBluetoothOn.value || _isScanningBluetooth.value) return
        _isScanningBluetooth.value = true
        addLog("SYSTEM", "Scanning for vehicle bluetooth hosts...")
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _isScanningBluetooth.value = false
            addLog("SYSTEM", "Pair targets updated. Signal feeds stable.")
        }
    }

    fun connectBluetoothDevice(device: BluetoothDevice) {
        if (!_isBluetoothOn.value) return
        _connectedDeviceName.value = device.name
        
        // Update device connection statuses
        val updated = _bluetoothDevices.value.map {
            it.copy(isConnected = it.name == device.name)
        }
        _bluetoothDevices.value = updated
        addLog("SYSTEM", "Successfully coupled Bluetooth Audio to: ${device.name}")
    }

    fun disconnectBluetooth() {
        _connectedDeviceName.value = null
        val updated = _bluetoothDevices.value.map { it.copy(isConnected = false) }
        _bluetoothDevices.value = updated
        addLog("SYSTEM", "Bluetooth audio connection terminated.")
    }

    // --- Voice control terminal log & Gemini command parsing ---

    private fun addLog(sender: String, message: String) {
        val currentTime = "09:17 AM" // Mock static time or standard local format
        val newLogs = _voiceLogs.value + ConsoleLog(sender, message, currentTime)
        // Keep logs capped to prevent bloating
        _voiceLogs.value = if (newLogs.size > 20) newLogs.drop(1) else newLogs
    }

    /**
     * Processes texted spoken console instructions
     */
    fun handleVoiceInput(sentence: String) {
        if (sentence.isBlank()) return
        addLog("DRIVER", sentence)
        
        viewModelScope.launch {
            addLog("SYSTEM", "Parsing voice control intent via AI...")
            val action = VoiceCommandService.parseVoiceCommand(sentence)
            
            // Speak reply
            addLog("SYSTEM", action.reply)
            
            // Execute the intent
            when (action.intent.uppercase()) {
                "PLAY" -> {
                    val matchingSong = action.songTitle?.let { title ->
                        _playlist.value.indexOfFirst { it.title.contains(title, ignoreCase = true) }
                    } ?: -1
                    if (matchingSong != -1) {
                        playTrack(matchingSong)
                    } else if (_playlist.value.isNotEmpty()) {
                        playTrack(0)
                    }
                }
                "PAUSE" -> {
                    if (_isPlaying.value) {
                        togglePlayback()
                    }
                }
                "NAVIGATE" -> {
                    val destination = action.navigationDestination ?: "Center Horizon"
                    startNavigation(destination)
                }
                "VOLUME" -> {
                    action.volumeLevel?.let { vol ->
                        adjustVolume(vol)
                    }
                }
                "BLUETOOTH" -> {
                    // Turn on Bluetooth if off, and connect Chevy/First device
                    _isBluetoothOn.value = true
                    val target = _bluetoothDevices.value.firstOrNull()
                    if (target != null) {
                        connectBluetoothDevice(target)
                    }
                }
            }
        }
    }

    // --- Simulated GPS Hands-free Navigation HUD ---

    private fun startNavigation(destination: String) {
        _isNavigating.value = true
        _navigationDestination.value = destination
        _navDistanceMiles.value = (3.5 + Math.random() * 25.0).coerceIn(1.2, 50.0)
        _navMinutesEta.value = (8 + (Math.random() * 45).toInt()).coerceIn(5, 60)
        
        _navInstructions.value = "Head east on Hyperway Nebula. Continue for 2.4 miles."
    }

    fun endNavigation() {
        _isNavigating.value = false
        _navigationDestination.value = null
        addLog("SYSTEM", "Autonomous guidance terminated.")
    }

    // --- Native Real Speech Recording Controller ---

    fun startNativeVoiceRecording(context: Context) {
        if (_isVoiceListening.value) return
        
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isVoiceListening.value = true
                    _speechTranscription.value = "Listening to driver..."
                }

                override fun onBeginningOfSpeech() {
                    _speechTranscription.value = "Converting meow/sound..."
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    _isVoiceListening.value = false
                }

                override fun onError(error: Int) {
                    _isVoiceListening.value = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions needed"
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Mic error code: $error"
                    }
                    _speechTranscription.value = errorMsg
                    Log.e(TAG, "SpeechRecognizer error: $errorMsg")
                }

                override fun onResults(results: Bundle?) {
                    _isVoiceListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcribedText = matches?.firstOrNull() ?: ""
                    if (transcribedText.isNotEmpty()) {
                        _speechTranscription.value = transcribedText
                        handleVoiceInput(transcribedText)
                    } else {
                        _speechTranscription.value = "Couldn't capture sound."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isVoiceListening.value = false
            _speechTranscription.value = "Failed to launch voice: ${e.localizedMessage}"
            Log.e(TAG, "SpeechRecognizer startup error", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        synth.stop()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning speechRecognizer", e)
        }
    }
}
