package com.inspiredandroid.kai.ui.settings

import androidx.compose.runtime.Composable

// Device control is Android-only (accessibility service). No-op on desktop.
@Composable
actual fun PlatformDeviceControlCard() {}
