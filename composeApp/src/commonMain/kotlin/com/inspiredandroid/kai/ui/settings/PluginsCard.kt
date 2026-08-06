package com.inspiredandroid.kai.ui.settings

import androidx.compose.runtime.Composable

/**
 * FoneClaw-compatible plugin host card: lists installed plugin APKs and their
 * tools, with a per-tool enable toggle, plus a rescan control.
 *
 * Android-only: the real implementation lives in androidMain and drives
 * [com.inspiredandroid.kai.plugins.PluginManager]. Every other platform gets an
 * empty actual so shared UI still compiles.
 */
@Composable
expect fun PlatformPluginsCard()
