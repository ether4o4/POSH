package com.inspiredandroid.kai.ui.settings

import androidx.compose.runtime.Composable

// GGUF runtime is Android-only (needs the Linux sandbox). No-op on web.
@Composable
actual fun PlatformGgufModelsCard() {}
