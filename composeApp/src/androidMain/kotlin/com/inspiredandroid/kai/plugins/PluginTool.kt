package com.inspiredandroid.kai.plugins

import com.inspiredandroid.kai.mcp.McpTool
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bridges one plugin-declared tool into POSH's [Tool] contract. Executing it
 * binds the owning plugin APK (via [PluginConnection]) and calls its single
 * AIDL method with the tool name and JSON args, then normalizes the reply
 * Bundle into the `{success, result|error}` shape the rest of POSH expects —
 * exactly the convention [McpTool] uses for MCP tools.
 */
class PluginTool(
    private val connection: PluginConnection,
    private val manifest: PluginToolManifest,
) : Tool {

    override val schema: ToolSchema = ToolSchema(
        name = manifest.name,
        description = manifest.description,
        parameters = McpTool.convertInputSchema(manifest.inputSchema),
    )

    // Give the bind + round-trip a little headroom over the plugin's own timeout.
    override val timeout: Duration = (manifest.timeoutMillis + 5_000L).milliseconds

    override suspend fun execute(args: Map<String, Any>): Any {
        val argsJson = buildJsonObject {
            for ((key, value) in args) put(key, anyToJsonElement(value))
        }.toString()

        val reply = connection.executeTool(
            toolName = manifest.name,
            argsJson = argsJson,
            timeoutMs = manifest.timeoutMillis + 5_000L,
        ) ?: return mapOf(
            "success" to false,
            "error" to "Plugin did not respond (not installed, not bound, or timed out).",
        )

        val status = reply.getString(PluginProtocol.KEY_STATUS).orEmpty()
        val text = reply.getString(PluginProtocol.KEY_TEXT).orEmpty()
        val errorCode = reply.getString(PluginProtocol.KEY_ERROR_CODE)
        val errorMessage = reply.getString(PluginProtocol.KEY_ERROR_MESSAGE)

        return if (status.equals("error", ignoreCase = true) || errorMessage != null) {
            mapOf(
                "success" to false,
                "error" to (errorMessage ?: text.ifBlank { "Plugin reported an error." }),
                "errorCode" to (errorCode ?: ""),
            )
        } else {
            mapOf(
                "success" to true,
                "status" to status.ifBlank { "ok" },
                "result" to text,
            )
        }
    }

    private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to anyToJsonElement(v) })
        is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}
