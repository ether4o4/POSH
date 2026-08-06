package com.inspiredandroid.kai.tools

actual class AccessibilityController actual constructor() {
    actual fun isSupported(): Boolean = false
    actual fun isEnabled(): Boolean = false
    actual fun isRunning(): Boolean = false
    actual fun openAccessibilitySettings() {}
}
