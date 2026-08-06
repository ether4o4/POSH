package com.inspiredandroid.kai.plugins

import kotlinx.serialization.json.JsonObject

/**
 * POSH's host side of the FoneClaw-compatible plugin protocol. A plugin is a
 * separate, independently-installed APK that exposes agent tools to POSH over a
 * bound service. POSH discovers such APKs, reads their declared tool manifest,
 * and — when a tool runs — binds the service and calls it over the plugin AIDL
 * contract. This lets POSH inherit an installable tool ecosystem instead of
 * inventing one; it is wire-compatible with FoneClaw's open plugin APKs.
 *
 * Protocol constants (must match the FoneClaw plugin contract exactly):
 * - bind action:        ai.android.claw.extension.BIND
 * - bind permission:    ai.android.claw.permission.BIND_EXTENSION
 * - AIDL descriptor:    ai.android.claw.extension.IFoneClawExtensionService
 * - manifest meta-data: ai.android.claw.extension.manifest -> raw JSON resource
 * - request bundle keys: toolName, argsJson
 * - reply bundle keys:   status, text, errorCode, errorMessage
 */
object PluginProtocol {
    const val BIND_ACTION = "ai.android.claw.extension.BIND"
    const val BIND_PERMISSION = "ai.android.claw.permission.BIND_EXTENSION"
    const val AIDL_DESCRIPTOR = "ai.android.claw.extension.IFoneClawExtensionService"
    const val META_API_VERSION = "ai.android.claw.extension.api_version"
    const val META_MANIFEST = "ai.android.claw.extension.manifest"

    // Request bundle keys.
    const val KEY_TOOL_NAME = "toolName"
    const val KEY_ARGS_JSON = "argsJson"

    // Reply bundle keys.
    const val KEY_STATUS = "status"
    const val KEY_TEXT = "text"
    const val KEY_ERROR_CODE = "errorCode"
    const val KEY_ERROR_MESSAGE = "errorMessage"
}

/** One installed plugin APK and the tools it declares. */
data class InstalledPlugin(
    val extensionId: String,
    val packageName: String,
    val displayName: String,
    val versionName: String,
    val apiVersion: Int,
    val tools: List<PluginToolManifest>,
)

/** A single tool as declared in a plugin's `foneclaw_extension` manifest JSON. */
data class PluginToolManifest(
    val name: String,
    val displayName: String,
    val description: String,
    val risk: String,
    val approvalMode: String,
    val inputSchema: JsonObject?,
    val timeoutMillis: Long,
)
