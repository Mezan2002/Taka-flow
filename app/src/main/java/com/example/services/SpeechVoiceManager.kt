package com.example.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechVoiceManager(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.language = Locale.US
                }
            }
        }
    }

    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!isRecognitionAvailable()) {
            onError("Speech recognition service is not available on this device.")
            return
        }
        stopListening()
        runCatching {
            val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                private var lastHeard = ""

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (lastHeard.isNotBlank()) {
                        onFinal(lastHeard)
                    } else {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Please try speaking again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech service network issue. You can also type below."
                            else -> "Voice capture stopped (code $error)."
                        }
                        onError(msg)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        onFinal(text)
                    } else if (lastHeard.isNotBlank()) {
                        onFinal(lastHeard)
                    } else {
                        onError("No speech detected.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        lastHeard = text
                        onPartial(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.startListening(intent)
        }.onFailure { e ->
            Log.e("SpeechVoiceManager", "Failed to start listening", e)
            onError(e.localizedMessage ?: "Failed to start speech recognizer.")
        }
    }

    fun stopListening() {
        runCatching {
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
    }

    fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "takaflow_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
    }

    fun release() {
        stopListening()
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }
}
