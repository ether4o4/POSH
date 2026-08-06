package com.inspiredandroid.kai.tools

import android.content.Context
import android.graphics.BitmapFactory
import com.inspiredandroid.kai.accessibility.PoshAccessibilityService
import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import com.inspiredandroid.kai.sandbox.LinuxSandboxManager
import com.inspiredandroid.kai.sandbox.resolveSandboxFile
import org.koin.java.KoinJavaComponent.inject

/**
 * The device-control tool family — POSH driving the phone through its own
 * AccessibilityService: reading what's on screen, tapping, swiping, typing,
 * pressing the system keys, launching apps, and capturing the screen. Every tool
 * fails gracefully with an actionable message when the service isn't enabled, so
 * the model can tell the user to switch it on rather than looping on a dead call.
 *
 * These are the built-in equivalent of FoneClaw's device actions; the plugin host
 * remains available for third-party FoneClaw plugin APKs on top of these.
 */

private const val SERVICE_DISABLED_HINT =
    "POSH device control is off. Ask the user to enable it: Settings → Device Control → turn on, then switch POSH on under Android Accessibility settings."

private fun activeService(): PoshAccessibilityService? = PoshAccessibilityService.instance

private fun serviceDisabled(): Map<String, Any> = mapOf(
    "success" to false,
    "error" to "accessibility_disabled",
    "hint" to SERVICE_DISABLED_HINT,
)

private fun Map<String, Any>.intArg(key: String): Int? =
    (this[key] as? Number)?.toInt() ?: (this[key] as? String)?.trim()?.toIntOrNull()

private fun Map<String, Any>.longArg(key: String): Long? =
    (this[key] as? Number)?.toLong() ?: (this[key] as? String)?.trim()?.toLongOrNull()

private fun Map<String, Any>.boolArg(key: String): Boolean? = when (val v = this[key]) {
    is Boolean -> v
    is String -> v.trim().lowercase().toBooleanStrictOrNull()
    else -> null
}

/** Read the visible on-screen elements with their tap coordinates. */
object DeviceReadScreenTool : Tool {
    override val schema = ToolSchema(
        name = "device_read_screen",
        description = "See what is currently on the phone screen. Returns the visible text and interactive elements (buttons, fields, etc.) each with a center x,y you can pass to device_tap. Call this before tapping so you know where things are.",
        parameters = emptyMap(),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val elements = service.readScreen().map { e ->
            buildMap<String, Any> {
                if (e.text.isNotEmpty()) put("text", e.text)
                if (e.contentDescription.isNotEmpty()) put("desc", e.contentDescription)
                if (e.className.isNotEmpty()) put("class", e.className)
                put("x", e.centerX)
                put("y", e.centerY)
                if (e.clickable) put("clickable", true)
                if (e.editable) put("editable", true)
            }
        }
        return mapOf("success" to true, "count" to elements.size, "elements" to elements)
    }

    val toolInfo = ToolInfo(
        id = "device_read_screen",
        name = "Read Screen",
        description = "Let the assistant see on-screen text and elements",
    )
}

/** Tap a screen coordinate. */
object DeviceTapTool : Tool {
    override val schema = ToolSchema(
        name = "device_tap",
        description = "Tap the screen at a pixel coordinate. Get coordinates from device_read_screen first.",
        parameters = mapOf(
            "x" to ParameterSchema("integer", "X pixel from the left edge", true),
            "y" to ParameterSchema("integer", "Y pixel from the top edge", true),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val x = args.intArg("x") ?: return mapOf("success" to false, "error" to "x is required")
        val y = args.intArg("y") ?: return mapOf("success" to false, "error" to "y is required")
        val ok = service.tap(x, y)
        return mapOf("success" to ok, "x" to x, "y" to y)
    }

    val toolInfo = ToolInfo(
        id = "device_tap",
        name = "Tap Screen",
        description = "Tap the screen at a coordinate",
    )
}

/** Long-press a screen coordinate. */
object DeviceLongPressTool : Tool {
    override val schema = ToolSchema(
        name = "device_long_press",
        description = "Long-press (press and hold) the screen at a pixel coordinate — e.g. to open a context menu.",
        parameters = mapOf(
            "x" to ParameterSchema("integer", "X pixel from the left edge", true),
            "y" to ParameterSchema("integer", "Y pixel from the top edge", true),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val x = args.intArg("x") ?: return mapOf("success" to false, "error" to "x is required")
        val y = args.intArg("y") ?: return mapOf("success" to false, "error" to "y is required")
        val ok = service.longPress(x, y)
        return mapOf("success" to ok, "x" to x, "y" to y)
    }

    val toolInfo = ToolInfo(
        id = "device_long_press",
        name = "Long Press",
        description = "Press and hold the screen at a coordinate",
    )
}

/** Swipe / scroll between two coordinates. */
object DeviceSwipeTool : Tool {
    override val schema = ToolSchema(
        name = "device_swipe",
        description = "Swipe from one coordinate to another — use to scroll (e.g. swipe up = scroll down) or drag. Duration controls speed.",
        parameters = mapOf(
            "x1" to ParameterSchema("integer", "Start X pixel", true),
            "y1" to ParameterSchema("integer", "Start Y pixel", true),
            "x2" to ParameterSchema("integer", "End X pixel", true),
            "y2" to ParameterSchema("integer", "End Y pixel", true),
            "duration_ms" to ParameterSchema("integer", "Swipe duration in milliseconds (default 300)", false),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val x1 = args.intArg("x1") ?: return mapOf("success" to false, "error" to "x1 is required")
        val y1 = args.intArg("y1") ?: return mapOf("success" to false, "error" to "y1 is required")
        val x2 = args.intArg("x2") ?: return mapOf("success" to false, "error" to "x2 is required")
        val y2 = args.intArg("y2") ?: return mapOf("success" to false, "error" to "y2 is required")
        val duration = args.longArg("duration_ms") ?: 300L
        val ok = service.swipe(x1, y1, x2, y2, duration)
        return mapOf("success" to ok)
    }

    val toolInfo = ToolInfo(
        id = "device_swipe",
        name = "Swipe / Scroll",
        description = "Swipe or scroll between two coordinates",
    )
}

/** Type text into the focused field. */
object DeviceTypeTool : Tool {
    override val schema = ToolSchema(
        name = "device_type",
        description = "Type text into the currently-focused text field. Tap the field first (device_tap) so it has focus. Set append=true to add to existing text instead of replacing it.",
        parameters = mapOf(
            "text" to ParameterSchema("string", "The text to enter", true),
            "append" to ParameterSchema("boolean", "Append to existing text instead of replacing (default false)", false),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val text = args["text"] as? String
            ?: return mapOf("success" to false, "error" to "text is required")
        val append = args.boolArg("append") ?: false
        val ok = service.typeText(text, append)
        return if (ok) {
            mapOf("success" to true)
        } else {
            mapOf(
                "success" to false,
                "error" to "no_focused_field",
                "hint" to "No editable field is focused. Tap the text field first with device_tap, then type.",
            )
        }
    }

    val toolInfo = ToolInfo(
        id = "device_type",
        name = "Type Text",
        description = "Type into the focused text field",
    )
}

/** Press a system key: back, home, recents, notifications, etc. */
object DeviceKeyTool : Tool {
    override val schema = ToolSchema(
        name = "device_press_key",
        description = "Press a system navigation key. Valid keys: back, home, recents, notifications, quick_settings, lock.",
        parameters = mapOf(
            "key" to ParameterSchema("string", "One of: back, home, recents, notifications, quick_settings, lock", true),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val key = (args["key"] as? String)?.trim()
            ?: return mapOf("success" to false, "error" to "key is required")
        val ok = service.pressKey(key)
        return if (ok) {
            mapOf("success" to true, "key" to key)
        } else {
            mapOf("success" to false, "error" to "unknown_or_unsupported_key", "key" to key)
        }
    }

    val toolInfo = ToolInfo(
        id = "device_press_key",
        name = "Press System Key",
        description = "Back, home, recents, notifications, etc.",
    )
}

/** Capture the screen to a PNG the user can open. */
object DeviceScreenshotTool : Tool {
    private val sandboxManager: LinuxSandboxManager by inject(LinuxSandboxManager::class.java)

    override val schema = ToolSchema(
        name = "device_screenshot",
        description = "Capture the current screen to a PNG saved under /root (viewable via open_file). Also returns the screen size and a summary of on-screen elements. Requires Android 11+.",
        parameters = emptyMap(),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val service = activeService() ?: return serviceDisabled()
        val relPath = "screenshots/screen-${System.currentTimeMillis()}.png"
        val dest = resolveSandboxFile(sandboxManager.homePath, relPath)
            ?: return mapOf("success" to false, "error" to "could not resolve screenshot path")
        dest.parentFile?.mkdirs()
        val ok = service.takeScreenshotTo(dest.absolutePath)
        if (!ok) {
            return mapOf(
                "success" to false,
                "error" to "screenshot_failed",
                "hint" to "Screenshot needs Android 11+ and the accessibility service enabled.",
            )
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(dest.absolutePath, bounds) }
        val summary = service.readScreen(limit = 40).mapNotNull {
            it.text.ifEmpty { it.contentDescription }.ifEmpty { null }
        }
        return mapOf(
            "success" to true,
            "path" to relPath,
            "width" to bounds.outWidth,
            "height" to bounds.outHeight,
            "on_screen_text" to summary,
            "note" to "Saved to /root/$relPath — open with open_file to show the user.",
        )
    }

    val toolInfo = ToolInfo(
        id = "device_screenshot",
        name = "Screenshot",
        description = "Capture the screen to a PNG",
    )
}

/** Launch an installed app by name or package. */
object DeviceOpenAppTool : Tool {
    private val context: Context by inject(Context::class.java)

    override val schema = ToolSchema(
        name = "device_open_app",
        description = "Launch an installed app by name (e.g. \"Settings\", \"Chrome\") or exact package id. Matches the app's visible label loosely.",
        parameters = mapOf(
            "query" to ParameterSchema("string", "App name or package id to open", true),
        ),
    )

    override suspend fun execute(args: Map<String, Any>): Any {
        val query = (args["query"] as? String)?.trim()?.lowercase()
            ?: return mapOf("success" to false, "error" to "query is required")
        val pm = context.packageManager
        val apps = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
        // Prefer an exact package match, then an exact label, then a label/package contains.
        val exactPkg = apps.firstOrNull { it.packageName.equals(query, ignoreCase = true) }
        val match = exactPkg ?: apps
            .mapNotNull { info ->
                val label = pm.getApplicationLabel(info).toString()
                val launchable = pm.getLaunchIntentForPackage(info.packageName) != null
                if (!launchable) return@mapNotNull null
                val score = when {
                    label.equals(query, ignoreCase = true) -> 0
                    label.lowercase().startsWith(query) -> 1
                    label.lowercase().contains(query) -> 2
                    info.packageName.lowercase().contains(query) -> 3
                    else -> return@mapNotNull null
                }
                info to score
            }
            .minByOrNull { it.second }?.first
        if (match == null) {
            return mapOf("success" to false, "error" to "app_not_found", "query" to query)
        }
        val launch = pm.getLaunchIntentForPackage(match.packageName)
            ?: return mapOf("success" to false, "error" to "not_launchable", "package" to match.packageName)
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launch)
            mapOf("success" to true, "package" to match.packageName, "label" to pm.getApplicationLabel(match).toString())
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "failed to launch"))
        }
    }

    val toolInfo = ToolInfo(
        id = "device_open_app",
        name = "Open App",
        description = "Launch an installed app by name",
    )
}

/** All device-control tools, in the order they should be offered to the model. */
val deviceControlTools: List<Tool> = listOf(
    DeviceReadScreenTool,
    DeviceTapTool,
    DeviceLongPressTool,
    DeviceSwipeTool,
    DeviceTypeTool,
    DeviceKeyTool,
    DeviceScreenshotTool,
    DeviceOpenAppTool,
)

val deviceControlToolInfos: List<ToolInfo> = listOf(
    DeviceReadScreenTool.toolInfo,
    DeviceTapTool.toolInfo,
    DeviceLongPressTool.toolInfo,
    DeviceSwipeTool.toolInfo,
    DeviceTypeTool.toolInfo,
    DeviceKeyTool.toolInfo,
    DeviceScreenshotTool.toolInfo,
    DeviceOpenAppTool.toolInfo,
)
