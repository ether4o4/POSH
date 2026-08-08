package com.inspiredandroid.kai.tools

/**
 * Controller for POSH's accessibility-based device control. Like the notification
 * listener, `BIND_ACCESSIBILITY_SERVICE` is not a runtime permission — the user must
 * switch POSH on under **Settings → Accessibility**, so this controller just reports
 * the state and offers a deep-link into that screen. Android-only (FOSS flavor); the
 * other platforms return "unsupported".
 */
expect class AccessibilityController() {
    /** True when the build actually declares the accessibility service (Android FOSS only). */
    fun isSupported(): Boolean

    /** True when the user has switched POSH on in system Accessibility settings. */
    fun isEnabled(): Boolean

    /** True when the service is bound and live (ready to act right now). */
    fun isRunning(): Boolean

    /** Open the system Accessibility settings screen (highlighting POSH's row where supported). */
    fun openAccessibilitySettings()
}
