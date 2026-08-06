package com.inspiredandroid.kai.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.inspiredandroid.kai.accessibility.PoshAccessibilityService
import org.koin.java.KoinJavaComponent.inject

/** True when the merged manifest actually declares the accessibility service. */
internal fun Context.declaresAccessibilityService(): Boolean = try {
    packageManager.getServiceInfo(
        ComponentName(this, PoshAccessibilityService.SERVICE_FQN),
        0,
    )
    true
} catch (_: Throwable) {
    false
}

actual class AccessibilityController actual constructor() {
    private val context: Context by inject(Context::class.java)
    private val supported: Boolean by lazy { context.declaresAccessibilityService() }

    actual fun isSupported(): Boolean = supported

    actual fun isEnabled(): Boolean {
        if (!supported) return false
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(context, PoshAccessibilityService.SERVICE_FQN).flattenToString()
        // The setting is a ':'-separated list of enabled service components.
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    actual fun isRunning(): Boolean = PoshAccessibilityService.instance != null

    actual fun openAccessibilitySettings() {
        if (!supported) return
        val component = ComponentName(context, PoshAccessibilityService.SERVICE_FQN).flattenToString()
        // ACTION_ACCESSIBILITY_SETTINGS always resolves. The plain-string fragment-args
        // extras are the widely-used, non-constant way to ask Settings to highlight a
        // specific service's row; unsupported builds simply show the full list.
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(":settings:fragment_args_key", component)
            putExtra(
                ":settings:show_fragment_args",
                Bundle().apply { putString(":settings:fragment_args_key", component) },
            )
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
