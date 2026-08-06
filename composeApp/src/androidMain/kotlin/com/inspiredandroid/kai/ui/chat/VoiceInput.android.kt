package com.inspiredandroid.kai.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android voice input backed by the platform [SpeechRecognizer]. Prefers the
 * on-device recognizer (`EXTRA_PREFER_OFFLINE`) so it keeps working without a
 * network connection where the device supports offline recognition; no bundled
 * model, no native library. Recognized text is delivered via [onText].
 */
@Composable
actual fun rememberVoiceInput(onText: (String) -> Unit): VoiceInputController? {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    if (!available) return null

    val currentOnText by rememberUpdatedState(onText)
    var listening by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(recognizer) {
        onDispose {
            listening = false
            recognizer.destroy()
        }
    }

    // Holds a start request made before the permission prompt resolved.
    var startWhenGranted by remember { mutableStateOf(false) }

    fun beginListening() {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                listening = false
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) currentOnText(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        listening = true
        recognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted && startWhenGranted) {
            startWhenGranted = false
            beginListening()
        }
    }

    return remember {
        object : VoiceInputController {
            override val isListening: Boolean get() = listening
            override val isAvailable: Boolean = true

            override fun toggle() {
                if (listening) {
                    recognizer.cancel()
                    listening = false
                    return
                }
                if (hasPermission) {
                    beginListening()
                } else {
                    startWhenGranted = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}
