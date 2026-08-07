package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.plugins.InstalledPlugin
import com.inspiredandroid.kai.plugins.PluginManager
import com.inspiredandroid.kai.ui.handCursor
import org.koin.compose.koinInject

/**
 * Android plugin host UI: lists installed compatible plugin APKs and every tool
 * they declare, with a per-tool enable toggle and a rescan button.
 * Styled to POSH's black/red scheme via [SettingsCard] and theme colors.
 */
@Composable
actual fun PlatformPluginsCard() {
    val manager = koinInject<PluginManager>()

    var plugins by remember { mutableStateOf(manager.installedPlugins()) }
    // Bump to force recomposition of toggle rows after a change.
    var toggleTick by remember { mutableStateOf(0) }

    SettingsCard {
        Text(
            text = "Plugins",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Install a compatible plugin APK to give POSH new tools (file manager, media, and more). Discovered tools are enabled by default and appear to the agent automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { plugins = manager.rescan() },
            modifier = Modifier.handCursor(),
        ) { Text("Rescan for plugins") }

        if (plugins.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No plugin APKs found. Sideload one, then tap Rescan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            val uriHandler = LocalUriHandler.current
            OutlinedButton(
                onClick = { uriHandler.openUri("https://github.com/ether4o4/POSH/blob/main/docs/features/plugins.md") },
                modifier = Modifier.handCursor(),
            ) { Text("Open plugin catalog") }
        }

        plugins.forEach { plugin ->
            key(toggleTick, plugin.packageName) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                PluginBlock(
                    plugin = plugin,
                    isEnabled = { tool -> manager.isToolEnabled(plugin.packageName, tool) },
                    onToggle = { tool, enabled ->
                        manager.setToolEnabled(plugin.packageName, tool, enabled)
                        toggleTick++
                    },
                )
            }
        }
    }
}

@Composable
private fun PluginBlock(
    plugin: InstalledPlugin,
    isEnabled: (String) -> Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = plugin.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${plugin.packageName}  ·  v${plugin.versionName}  ·  ${plugin.tools.size} tools",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        plugin.tools.forEach { tool ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tool.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (tool.description.isNotBlank()) {
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = isEnabled(tool.name),
                    onCheckedChange = { onToggle(tool.name, it) },
                )
            }
        }
    }
}
