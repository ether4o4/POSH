package com.inspiredandroid.kai.ui.settings

import androidx.compose.runtime.Composable

/**
 * Device-control settings card: the master on/off toggle for POSH's
 * accessibility-based phone control, plus a live permission walkthrough that
 * deep-links into the Android Accessibility settings screen.
 *
 * Android-only (FOSS flavor); every other platform gets an empty actual so the
 * shared settings screen still compiles.
 */
@Composable
expect fun PlatformDeviceControlCard()
