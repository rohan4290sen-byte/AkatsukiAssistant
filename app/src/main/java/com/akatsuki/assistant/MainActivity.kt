package com.akatsuki.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.akatsuki.assistant.actions.PhoneActionDispatcher
import com.akatsuki.assistant.brain.GeminiBrain
import com.akatsuki.assistant.service.AkatsukiOverlayService
import com.akatsuki.assistant.ui.AkatsukiHomeScreen
import com.akatsuki.assistant.ui.theme.AkatsukiAssistantTheme
import com.akatsuki.assistant.voice.AkatsukiVoiceEngine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var voiceEngine: AkatsukiVoiceEngine
    private lateinit var brain: GeminiBrain
    private lateinit var actionDispatcher: PhoneActionDispatcher

    private var lastUserQuery by mutableStateOf("")
    private var lastAssistantResponse by mutableStateOf("")
    private var isOverlayActive by mutableStateOf(false)
    private var currentLanguage by mutableStateOf("hi")
    private var apiKey by mutableStateOf("")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            Toast.makeText(this, "Microphone permission is required for Akatsuki voice commands", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved preferences
        val prefs = getSharedPreferences("akatsuki_prefs", Context.MODE_PRIVATE)
        apiKey = prefs.getString("gemini_api_key", "") ?: ""
        currentLanguage = prefs.getString("voice_language", "hi") ?: "hi"

        // Initialize Core Systems
        brain = GeminiBrain(apiKey)
        actionDispatcher = PhoneActionDispatcher(this)
        voiceEngine = AkatsukiVoiceEngine(this) { recognizedText ->
            handleVoiceCommand(recognizedText)
        }
        voiceEngine.currentLanguageCode = currentLanguage

        // Request Permissions
        checkAndRequestPermissions()

        // Handle auto-listen from overlay trigger
        if (intent.getBooleanExtra("AUTO_LISTEN", false)) {
            voiceEngine.startListening()
        }

        setContent {
            AkatsukiAssistantTheme {
                val isListening by voiceEngine.isListening.collectAsState()
                val isSpeaking by voiceEngine.isSpeaking.collectAsState()
                val statusText by voiceEngine.statusText.collectAsState()

                AkatsukiHomeScreen(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    statusText = statusText,
                    lastUserQuery = lastUserQuery,
                    lastAssistantResponse = lastAssistantResponse,
                    currentLanguage = currentLanguage,
                    isOverlayActive = isOverlayActive,
                    apiKey = apiKey,
                    onMicClick = {
                        if (isListening) {
                            voiceEngine.stopListening()
                        } else {
                            voiceEngine.startListening()
                        }
                    },
                    onLanguageChange = { lang ->
                        currentLanguage = lang
                        voiceEngine.currentLanguageCode = lang
                        prefs.edit().putString("voice_language", lang).apply()
                        voiceEngine.speak(if (lang == "hi") "Awaaz Hindi set ho gayi hai." else "Voice language set to English.")
                    },
                    onToggleOverlay = { enable ->
                        toggleOverlayService(enable)
                    },
                    onSaveApiKey = { newKey ->
                        apiKey = newKey
                        brain.updateApiKey(newKey)
                        prefs.edit().putString("gemini_api_key", newKey).apply()
                        Toast.makeText(this, "Gemini API key saved!", Toast.LENGTH_SHORT).show()
                    },
                    onQuickAction = { command ->
                        handleVoiceCommand(command)
                    }
                )
            }
        }
    }

    private fun handleVoiceCommand(commandText: String) {
        lastUserQuery = commandText
        lifecycleScope.launch {
            // Process with Gemini Brain
            val command = brain.processUserCommand(commandText)
            lastAssistantResponse = command.speech

            // Execute Phone Action
            actionDispatcher.execute(command) { spokenFeedback ->
                voiceEngine.speak(spokenFeedback)
            }
        }
    }

    private fun toggleOverlayService(enable: Boolean) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "Enable 'Display over other apps' for Akatsuki", Toast.LENGTH_LONG).show()
                return
            }
            val serviceIntent = Intent(this, AkatsukiOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isOverlayActive = true
        } else {
            val serviceIntent = Intent(this, AkatsukiOverlayService::class.java)
            stopService(serviceIntent)
            isOverlayActive = false
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.destroy()
    }
}
