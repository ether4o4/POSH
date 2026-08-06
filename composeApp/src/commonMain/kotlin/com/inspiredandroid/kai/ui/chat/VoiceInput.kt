package com.inspiredandroid.kai.ui.chat

import androidx.compose.runtime.Composable

/**
 * Hands-free voice input for the chat box. On Android this drives the platform
 * on-device speech recognizer (offline-preferring); every other platform
 * returns null so no mic affordance is shown.
 */
interface VoiceInputController {
    /** True while actively listening — read inside composition to drive the mic UI. */
    val isListening: Boolean

    /** True when speech recognition is available on this device. */
    val isAvailable: Boolean

    /** Begin (or, if already listening, cancel) a recognition session. */
    fun toggle()
}

/**
 * Returns a [VoiceInputController] wired to deliver recognized text via [onText],
 * or null when voice input is unsupported on this platform. Android provides a
 * real implementation; desktop/web/iOS return null.
 */
@Composable
expect fun rememberVoiceInput(onText: (String) -> Unit): VoiceInputController?
