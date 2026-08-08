package com.inspiredandroid.kai.plugins

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Discovers installed compatible plugin APKs, reads their declared
 * tool manifests, and exposes their tools to POSH's agent — the same way
 * [com.inspiredandroid.kai.mcp.McpServerManager] exposes MCP tools. Each plugin
 * runs in its own APK/process; POSH binds it on demand via [PluginConnection].
 *
 * Discovery is cheap and safe: it only reads public manifest metadata. A tool
 * is offered to the model only when the user has left it enabled (default on),
 * mirroring the MCP per-tool toggle.
 */
class PluginManager(
    private val context: Context,
    private val appSettings: AppSettings,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val connections = mutableMapOf<String, PluginConnection>()

    @Volatile
    private var cache: List<InstalledPlugin> = emptyList()

    /** Re-scan installed packages for plugin services. Returns the discovered plugins. */
    fun rescan(): List<InstalledPlugin> {
        val pm = context.packageManager
        val intent = Intent(PluginProtocol.BIND_ACTION)
        val services = try {
            pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }
        val discovered = services.mapNotNull { resolve ->
            val pkg = resolve.serviceInfo?.packageName ?: return@mapNotNull null
            runCatching { readPlugin(pkg) }.getOrNull()
        }.distinctBy { it.packageName }
        cache = discovered
        return discovered
    }

    /** The most recently scanned plugins (scans once if never scanned). */
    fun installedPlugins(): List<InstalledPlugin> {
        if (cache.isEmpty()) return rescan()
        return cache
    }

    private fun readPlugin(pkg: String): InstalledPlugin? {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
        val meta = appInfo.metaData ?: return null
        val manifestResId = meta.getInt(PluginProtocol.META_MANIFEST, 0)
        if (manifestResId == 0) return null
        val apiVersion = meta.getInt(PluginProtocol.META_API_VERSION, 1)

        val res = pm.getResourcesForApplication(appInfo)
        val jsonText = res.openRawResource(manifestResId).bufferedReader().use { it.readText() }
        val root = json.parseToJsonElement(jsonText).jsonObject

        val extensionId = root["extensionId"]?.jsonPrimitive?.content ?: pkg
        val displayName = root["displayName"]?.jsonPrimitive?.content ?: extensionId
        val versionName = root["versionName"]?.jsonPrimitive?.content ?: ""
        val tools = root["tools"]?.jsonArray?.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            PluginToolManifest(
                name = name,
                displayName = obj["displayName"]?.jsonPrimitive?.content ?: name,
                description = obj["description"]?.jsonPrimitive?.content ?: "",
                risk = obj["risk"]?.jsonPrimitive?.content ?: "LOW",
                approvalMode = obj["approvalMode"]?.jsonPrimitive?.content ?: "AUTO",
                inputSchema = obj["inputSchema"] as? JsonObject,
                timeoutMillis = obj["timeoutMillis"]?.jsonPrimitive?.content?.toLongOrNull() ?: 15_000L,
            )
        }.orEmpty()

        return InstalledPlugin(
            extensionId = extensionId,
            packageName = pkg,
            displayName = displayName,
            versionName = versionName,
            apiVersion = apiVersion,
            tools = tools,
        )
    }

    private fun connectionFor(pkg: String): PluginConnection =
        connections.getOrPut(pkg) { PluginConnection(context, pkg) }

    /** Tools the model may call: every declared tool the user has left enabled. */
    fun getEnabledPluginTools(): List<Tool> = buildList {
        for (plugin in installedPlugins()) {
            for (manifest in plugin.tools) {
                if (appSettings.isToolEnabled(toggleId(plugin.packageName, manifest.name))) {
                    add(PluginTool(connectionFor(plugin.packageName), manifest))
                }
            }
        }
    }

    /** Tool infos for the settings/plugins UI, one row per declared tool. */
    fun getPluginToolInfos(): List<ToolInfo> = buildList {
        for (plugin in installedPlugins()) {
            for (manifest in plugin.tools) {
                val id = toggleId(plugin.packageName, manifest.name)
                add(
                    ToolInfo(
                        id = id,
                        name = manifest.displayName,
                        description = manifest.description,
                        isEnabled = appSettings.isToolEnabled(id),
                    ),
                )
            }
        }
    }

    fun setToolEnabled(packageName: String, toolName: String, enabled: Boolean) {
        appSettings.setToolEnabled(toggleId(packageName, toolName), enabled)
    }

    fun isToolEnabled(packageName: String, toolName: String): Boolean =
        appSettings.isToolEnabled(toggleId(packageName, toolName))

    fun release() {
        connections.values.forEach { it.unbind() }
        connections.clear()
    }

    companion object {
        /** Persistent per-tool enable-toggle key. Namespaced so it never collides with MCP. */
        fun toggleId(packageName: String, toolName: String): String = "plugintool:$packageName:$toolName"
    }
}
