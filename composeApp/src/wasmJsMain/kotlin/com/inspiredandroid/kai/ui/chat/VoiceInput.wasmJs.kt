package com.inspiredandroid.kai.ui.chat

import androidx.compose.runtime.Composable

// On-device speech recognition is Android-only here. No mic on web.
@Composable
actual fun rememberVoiceInput(onText: (String) -> Unit): VoiceInputController? = null
