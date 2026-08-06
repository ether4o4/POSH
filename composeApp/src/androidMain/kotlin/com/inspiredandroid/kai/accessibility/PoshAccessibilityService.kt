package com.inspiredandroid.kai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * POSH's own AccessibilityService — the muscle behind the device-control tools.
 * Once the user enables it in Android's Accessibility settings, the system binds
 * this service and hands it the ability to inject gestures, type into the focused
 * field, read the on-screen node tree, and capture the screen. The device-control
 * tools reach the live instance through [instance]; when the service isn't enabled
 * that reference is null and every tool reports a clear "not enabled" error rather
 * than silently failing.
 *
 * Nothing here observes or records the user's activity: [onAccessibilityEvent] is a
 * no-op, and the config requests no event stream. The service only acts when a tool
 * explicitly calls one of the action methods below.
 */
class PoshAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    // Deliberately ignored — POSH does not watch the screen; it only acts on demand.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    // --- Gestures ---------------------------------------------------------------

    suspend fun tap(x: Int, y: Int): Boolean = gesture {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
    }

    suspend fun longPress(x: Int, y: Int): Boolean = gesture {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        addStroke(GestureDescription.StrokeDescription(path, 0, LONG_PRESS_DURATION_MS))
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean = gesture {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val dur = durationMs.coerceIn(20L, 10_000L)
        addStroke(GestureDescription.StrokeDescription(path, 0, dur))
    }

    private suspend fun gesture(build: GestureDescription.Builder.() -> Unit): Boolean {
        val gesture = GestureDescription.Builder().apply(build).build()
        return suspendCancellableCoroutine { cont ->
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null,
            )
            if (!dispatched && cont.isActive) cont.resume(false)
        }
    }

    // --- Global navigation ------------------------------------------------------

    fun pressKey(key: String): Boolean {
        val action = when (key.lowercase().trim()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents", "recent", "overview" -> GLOBAL_ACTION_RECENTS
            "notifications", "notification" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings", "quicksettings" -> GLOBAL_ACTION_QUICK_SETTINGS
            "lock", "lock_screen" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_LOCK_SCREEN else return false
            else -> return false
        }
        return performGlobalAction(action)
    }

    // --- Text entry -------------------------------------------------------------

    /** Set (or append to) the text of the currently-focused editable field. */
    fun typeText(text: String, append: Boolean): Boolean {
        val node = focusedEditable() ?: return false
        val newText = if (append) {
            val existing = node.text?.toString().orEmpty()
            existing + text
        } else {
            text
        }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        node.recycle()
        return ok
    }

    private fun focusedEditable(): AccessibilityNodeInfo? {
        findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { if (it.isEditable) return it }
        // Fall back to the first editable node in the active window.
        val root = rootInActiveWindow ?: return null
        return firstEditable(root)
    }

    private fun firstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = firstEditable(child)
            if (found != null) return found
        }
        return null
    }

    // --- Screen reading ---------------------------------------------------------

    data class ScreenElement(
        val text: String,
        val contentDescription: String,
        val className: String,
        val centerX: Int,
        val centerY: Int,
        val clickable: Boolean,
        val editable: Boolean,
    )

    /**
     * Flatten the active window into a compact list of elements that carry text,
     * a content description, or are interactable — with each element's tap point.
     * This is what lets even a text-only model "see" the screen and choose where
     * to tap without a screenshot.
     */
    fun readScreen(limit: Int = MAX_SCREEN_ELEMENTS): List<ScreenElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = ArrayList<ScreenElement>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        val bounds = Rect()
        while (stack.isNotEmpty() && out.size < limit) {
            val node = stack.removeLast()
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val interactable = node.isClickable || node.isEditable || node.isCheckable
            if ((text.isNotEmpty() || desc.isNotEmpty() || interactable) && node.isVisibleToUser) {
                node.getBoundsInScreen(bounds)
                out.add(
                    ScreenElement(
                        text = text,
                        contentDescription = desc,
                        className = node.className?.toString()?.substringAfterLast('.').orEmpty(),
                        centerX = bounds.centerX(),
                        centerY = bounds.centerY(),
                        clickable = node.isClickable,
                        editable = node.isEditable,
                    ),
                )
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.addLast(it) }
            }
        }
        return out
    }

    // --- Screenshot -------------------------------------------------------------

    /** Capture the screen to [destPath] as PNG. Requires Android 11 (API 30)+. */
    suspend fun takeScreenshotTo(destPath: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return suspendCancellableCoroutine { cont ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    executor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val ok = runCatching {
                                val hardwareBuffer = screenshot.hardwareBuffer
                                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                                hardwareBuffer.close()
                                if (bitmap == null) {
                                    false
                                } else {
                                    File(destPath).parentFile?.mkdirs()
                                    FileOutputStream(destPath).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
                                    bitmap.recycle()
                                    true
                                }
                            }.getOrDefault(false)
                            executor.shutdown()
                            if (cont.isActive) cont.resume(ok)
                        }

                        override fun onFailure(errorCode: Int) {
                            executor.shutdown()
                            if (cont.isActive) cont.resume(false)
                        }
                    },
                )
            } catch (e: Exception) {
                executor.shutdown()
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    companion object {
        @Volatile
        var instance: PoshAccessibilityService? = null
            private set

        /** The service's fully-qualified name, used to deep-link into its settings row. */
        const val SERVICE_FQN = "com.inspiredandroid.kai.accessibility.PoshAccessibilityService"

        private const val TAP_DURATION_MS = 50L
        private const val LONG_PRESS_DURATION_MS = 600L
        private const val MAX_SCREEN_ELEMENTS = 200
    }
}
