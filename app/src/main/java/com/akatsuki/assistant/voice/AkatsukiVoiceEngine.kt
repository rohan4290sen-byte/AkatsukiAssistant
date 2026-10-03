package com.akatsuki.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class AkatsukiVoiceEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val tag = "AkatsukiVoiceEngine"
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _statusText = MutableStateFlow("Akatsuki Ready")
    val statusText: StateFlow<String> = _statusText

    // Voice mode: "hi" for Hindi (India), "en" for English (India)
    var currentLanguageCode: String = "hi"
        set(value) {
            field = value
            configureBestIndianVoice()
        }

    init {
        tts = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            configureBestIndianVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _statusText.value = "Akatsuki speaking..."
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _statusText.value = "Ready for command, Sir."
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _statusText.value = "Voice output error"
                }
            })
            Log.d(tag, "TTS initialized successfully with Indian vocal profile")
        } else {
            Log.e(tag, "Failed to initialize TTS engine")
        }
    }

    /**
     * Chooses the clearest, highest quality Indian neural voice available on the device
     * (e.g. Google Hindi India or Google English India with normal 1.0 pitch and speed)
     */
    private fun configureBestIndianVoice() {
        tts?.let { engine ->
            val targetLocale = if (currentLanguageCode == "hi") {
                Locale("hi", "IN")
            } else {
                Locale("en", "IN")
            }

            val result = engine.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English (India)
                engine.language = Locale("en", "IN")
            }

            // Normal, clear pitch & natural speed (ek dam saaf awaaz)
            engine.setPitch(1.0f)
            engine.setSpeechRate(1.0f)

            // Look for high quality / network-trained voice if available
            try {
                val voices = engine.voices
                if (voices != null) {
                    val matchingVoices = voices.filter { voice ->
                        voice.locale.language == targetLocale.language &&
                                voice.locale.country.equals("IN", ignoreCase = true)
                    }

                    // Prefer natural/neural voice without high latency
                    val selectedVoice = matchingVoices.find { it.name.contains("network", ignoreCase = true) }
                        ?: matchingVoices.find { !it.isNetworkConnectionRequired }
                        ?: matchingVoices.firstOrNull()

                    if (selectedVoice != null) {
                        engine.voice = selectedVoice
                        Log.d(tag, "Selected voice: ${selectedVoice.name}")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not filter specific voice: ${e.message}")
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _statusText.value = "Listening to you, Sir..."
                    }

                    override fun onBeginningOfSpeech() {
                        _statusText.value = "Recording..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _statusText.value = "Processing command..."
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _statusText.value = "Ready for command, Sir."
                        Log.w(tag, "Speech recognition error code: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            _statusText.value = "Command: $recognizedText"
                            onSpeechRecognized(recognizedText)
                        } else {
                            _statusText.value = "Ready for command, Sir."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening() {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Support mixed Hindi + English (Hinglish / Indian speech recognition)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguageCode == "hi") "hi-IN" else "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Akatsuki is listening...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error starting speech recognition: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun speak(text: String) {
        tts?.let { engine ->
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AkatsukiUtterance_${System.currentTimeMillis()}")
            }
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AkatsukiUtterance")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
