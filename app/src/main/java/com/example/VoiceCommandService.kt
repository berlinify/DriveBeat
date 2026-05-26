package com.example

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Parsed action representing a car console voice command
 */
data class CarConsoleAction(
    val intent: String, // PLAY, PAUSE, NAVIGATE, VOLUME, BLUETOOTH, CHAT
    val songTitle: String? = null,
    val navigationDestination: String? = null,
    val volumeLevel: Int? = null,
    val reply: String
)

/**
 * Intelligent voice assistant for hands-free navigation & console steering.
 * Connects directly to Gemini API via Beta REST API (Option B), or falls back to robust
 * regex offline parsing if Gemini is unconfigured/no key is present.
 */
object VoiceCommandService {
    private const val TAG = "VoiceCommandService"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val SYSTEM_INSTRUCTIONS = """
        You are "DriveBeat Voice Assistant", an AI console built into standard vehicle infotainment setups.
        Your goal is to parse spoke command texts or user driver sentences into strict action intentions.
        You MUST respond ONLY with a RAW JSON block matching this structure:
        
        {
          "intent": "PLAY" | "PAUSE" | "NAVIGATE" | "VOLUME" | "BLUETOOTH" | "CHAT",
          "song_title": "string or null",
          "navigation_destination": "string or null",
          "volume_level": 0-100 or null,
          "reply": "Spoken clear driving safety reply. Keep it very short (under 15 words)!"
        }
        
        Examples:
        - "take me to McDonald's" -> {"intent":"NAVIGATE","navigation_destination":"McDonald's","reply":"Navigating to McDonald's."}
        - "play Starry Drive" -> {"intent":"PLAY","song_title":"Starry Drive","reply":"Synthesizing Starry Drive."}
        - "hush, turn down the sound" -> {"intent":"VOLUME","volume_level":30,"reply":"Volume set to thirty percent."}
        - "connect car system" -> {"intent":"BLUETOOTH","reply":"Initiating Bluetooth pair link."}
        - "tell me a joke" -> {"intent":"CHAT","reply":"What do cat car stereos play? Meow-sic! Hahaha."}
        
        No markdown blocks like ```json ... ``` tags are allowed in your output. Just pure, clean JSON. Use double-quotes for JSON properties.
    """.trimIndent()

    /**
     * Process voice command text and parse into structured CarConsoleAction
     */
    suspend fun parseVoiceCommand(text: String): CarConsoleAction = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Detect if API Key is placeholder or missing
        val isKeyConfigured = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.contains("PLACEHOLDER")

        if (!isKeyConfigured) {
            Log.w(TAG, "Gemini API key is unconfigured. Running smart local regex parser.")
            return@withContext parseOffline(text, isKeyMissing = true)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            
            // Build direct REST RequestBody mirroring Gemini REST API Structure
            val requestJson = JSONObject().apply {
                // System instructions
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", SYSTEM_INSTRUCTIONS)
                    }))
                })
                
                // Content contents list
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", text)
                    }))
                }))

                // Force JSON output
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorDetails = response.body?.string() ?: ""
                Log.e(TAG, "Gemini REST failure: ${response.code} code. Details: $errorDetails")
                return@withContext parseOffline(text, isError = true)
            }

            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Gemini REST response successfully retrieved: $responseBody")

            // Extract content candidate text safely
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val parts = firstCandidate.getJSONObject("content").getJSONArray("parts")
            val responseText = parts.getJSONObject(0).getString("text").trim()

            // Parse responseText JSON
            val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
            val parsedActionJson = JSONObject(cleanJson)

            val intent = parsedActionJson.optString("intent", "CHAT").uppercase()
            val songTitle = if (parsedActionJson.has("song_title") && !parsedActionJson.isNull("song_title")) parsedActionJson.getString("song_title") else null
            val navDest = if (parsedActionJson.has("navigation_destination") && !parsedActionJson.isNull("navigation_destination")) parsedActionJson.getString("navigation_destination") else null
            val volVal = if (parsedActionJson.has("volume_level") && !parsedActionJson.isNull("volume_level")) {
                parsedActionJson.getInt("volume_level")
            } else null
            val reply = parsedActionJson.optString("reply", "Parsed action successfully.")

            CarConsoleAction(
                intent = intent,
                songTitle = songTitle,
                navigationDestination = navDest,
                volumeLevel = volVal,
                reply = reply
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini connection error, fallback to offline parser", e)
            parseOffline(text, isError = true)
        }
    }

    /**
     * Fallback parser using intelligent offline semantic regex
     */
    fun parseOffline(text: String, isKeyMissing: Boolean = false, isError: Boolean = false): CarConsoleAction {
        val lower = text.lowercase().trim()
        val notice = when {
            isKeyMissing -> " [Offline HUD]"
            isError -> " [Offline Sync fallback]"
            else -> ""
        }

        return when {
            // Volume
            lower.contains("volume") || lower.contains("sound") -> {
                val numMatcher = Pattern.compile("\\d+").matcher(lower)
                val targetVol = if (numMatcher.find()) numMatcher.group().toInt().coerceIn(0, 100) else null
                CarConsoleAction(
                    intent = "VOLUME",
                    volumeLevel = targetVol,
                    reply = "Adjusting sound volume${if (targetVol != null) " to $targetVol%" else ""}.$notice"
                )
            }
            // Bluetooth
            lower.contains("bluetooth") || lower.contains("connect") || lower.contains("pair") -> {
                CarConsoleAction(
                    intent = "BLUETOOTH",
                    reply = "Pairing with nearest available car bluetooth handsfree link.$notice"
                )
            }
            // Navigate
            lower.contains("navigate to") || lower.contains("drive to") || lower.contains("find route") || lower.contains("map") || lower.contains("gps") || lower.contains("route") -> {
                val index = when {
                    lower.contains("navigate to") -> lower.indexOf("navigate to") + 11
                    lower.contains("drive to") -> lower.indexOf("drive to") + 8
                    lower.contains("find route to") -> lower.indexOf("find route to") + 13
                    else -> lower.indexOf("map") + 3
                }
                val destination = if (index in 0 until lower.length) text.substring(index).trim() else "Main City Core"
                CarConsoleAction(
                    intent = "NAVIGATE",
                    navigationDestination = destination,
                    reply = "Starting navigation guidance to $destination.$notice"
                )
            }
            // Play
            lower.contains("play") || lower.contains("start") || lower.contains("music") -> {
                val title = text.substringAfter("play", "").trim()
                CarConsoleAction(
                    intent = "PLAY",
                    songTitle = if (title.isNotEmpty()) title else null,
                    reply = "Sure, searching and playing requested track.$notice"
                )
            }
            // Pause
            lower.contains("pause") || lower.contains("stop") || lower.contains("mute") || lower.contains("hal") -> {
                CarConsoleAction(
                    intent = "PAUSE",
                    reply = "Audio synthesizer suspended for driving safety.$notice"
                )
            }
            // Chat / Other
            else -> {
                CarConsoleAction(
                    intent = "CHAT",
                    reply = "Hello driver! Let me know if you want to play a song, navigate, or link your Bluetooth audio console.$notice"
                )
            }
        }
    }
}
