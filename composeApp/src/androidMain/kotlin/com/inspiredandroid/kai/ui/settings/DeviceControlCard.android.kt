package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.tools.AccessibilityController
import com.inspiredandroid.kai.ui.handCursor
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Android implementation of the Device Control card. Reads its state directly from
 * Koin (mirroring the GGUF card) so it needs no ViewModel wiring, and polls the
 * accessibility permission state so the row updates the moment the user returns from
 * the Android settings screen.
 */
@Composable
actual fun PlatformDeviceControlCard() {
    val appSettings = koinInject<AppSettings>()
    val controller = koinInject<AccessibilityController>()

    if (!controller.isSupported()) return

    var enabled by remember { mutableStateOf(appSettings.isDeviceControlEnabled()) }

    // Poll the system permission state so the status flips right after the user
    // toggles POSH on under Settings → Accessibility and comes back.
    var permTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            permTick++
        }
    }
    val serviceOn = remember(permTick) { controller.isEnabled() }
    val serviceRunning = remember(permTick, serviceOn) { controller.isRunning() }

    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Device Control",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Let the assistant see the screen and tap, type, swipe, and screenshot for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    appSettings.setDeviceControlEnabled(it)
                },
                modifier = Modifier.handCursor(),
            )
        }

        if (enabled) {
            Spacer(Modifier.height(12.dp))
            val statusText = when {
                serviceRunning -> "Accessibility permission: granted and active"
                serviceOn -> "Accessibility permission: granted (starting…)"
                else -> "Accessibility permission: not granted"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (serviceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!serviceOn) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "POSH needs Android's accessibility permission to control the phone. Tap below — it opens Settings → Accessibility. Find \"POSH Device Control\" and switch it on, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { controller.openAccessibilitySettings() },
                    modifier = Modifier.handCursor(),
                ) { Text("Open Accessibility settings") }
            } else {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { controller.openAccessibilitySettings() },
                    modifier = Modifier.handCursor(),
                ) { Text("Manage in Accessibility settings") }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Capabilities unlocked",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = CAPABILITY_LIST,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Turn individual capability skills on or off on the Skills page (Device Control and Vision categories).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val CAPABILITY_LIST = """• read_screen — see on-screen text + tap targets
• tap / long_press — press elements
• swipe — scroll and drag
• type — enter text into fields
• press_key — back, home, recents, notifications
• screenshot — capture the screen (Android 11+)
• open_app — launch any installed app"""
