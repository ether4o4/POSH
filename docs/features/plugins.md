# Plugins

**Last verified:** 2026-08-06

POSH can host **plugin apps**: separately-installed Android APKs that expose extra
agent tools. When such an app is on the device, POSH discovers the tools it
declares, lists them in a settings card, and — for the tools the user leaves
enabled — offers them to the model exactly like any other tool. Running a plugin
tool hands the request to the plugin app, which does the work in its own process
and returns a result.

This lets POSH inherit an existing, installable tool ecosystem instead of building
every capability in-app. The protocol is wire-compatible with FoneClaw's open
plugin apps, so those APKs work as POSH plugins unchanged.

Plugins are **Android-only**. On desktop, web, and iOS the plugin surfaces are
empty no-ops, so there is nothing to configure there.

## Concepts

### Plugin app

A plugin is an independent APK the user installs themselves (sideload or store).
It advertises a bindable service and, in its app metadata, points at a bundled
JSON manifest describing the tools it offers. POSH never runs a plugin's code
in-process — it binds the plugin's service and communicates across the process
boundary, so a misbehaving or absent plugin cannot crash or block POSH beyond a
per-call timeout.

### Discovery

POSH finds plugins by asking the system which installed apps expose the plugin
bind action, then reading each one's declared manifest. Discovery only reads
public app metadata, so it is cheap and safe to run on demand. A **Rescan**
control re-runs discovery after the user installs or removes a plugin. Under
modern Android package-visibility rules POSH must declare that it looks for the
plugin action and hold the plugin bind permission; both are declared in the app
manifest.

### Tool manifest

Each plugin declares a display name, version, and a list of tools. Every tool
carries a name, a human description, an input schema (the parameters the model
must supply), and a timeout. POSH converts that input schema into the same
parameter format its built-in and MCP tools use, so plugin tools appear to the
model identically to any other tool.

### Per-tool enable toggle

Every discovered tool has its own on/off switch, defaulting to **on**. Only
enabled tools are offered to the model. The toggle state is persisted per
tool and is namespaced so it never collides with MCP tool toggles. This mirrors
the per-tool control already used for MCP servers.

### Execution

When the model calls a plugin tool, POSH binds the owning plugin app (if not
already bound), sends the tool name and the arguments as JSON, and waits for the
plugin's reply up to the tool's timeout plus a small margin. The reply is
normalized into POSH's standard `{success, result}` / `{success, error}` shape.
If the plugin is missing, cannot be bound, or does not answer in time, the tool
returns a clean failure rather than hanging the conversation.

## Using plugins

The plugin host lives on the hub's **Plugins** page (also reachable from the
Tools area of settings on Android). It shows:

- A short explanation and a **Rescan for plugins** button.
- If nothing is installed, an empty-state note plus a link to a plugin catalog.
- For each installed plugin: its name, package, version, tool count, and a
  toggle row per tool.

Toggling a tool takes effect on the next agent turn — no restart needed. The
page is styled in POSH's black/red scheme like the rest of the app.

## Relationship to MCP

Plugins and [MCP servers](mcp.md) solve the same problem — extending the agent
with external tools — from opposite directions. MCP tools come from **network**
endpoints POSH connects to; plugin tools come from **local apps** POSH binds to.
Both are wrapped into the same tool contract, both have per-tool toggles, and
both are assembled into the model's tool list the same way. A device can use
either, both, or neither.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/androidMain/.../plugins/PluginProtocol.kt` (in `PluginModels.kt`) | Protocol constants + plugin/tool data models |
| `composeApp/src/androidMain/.../plugins/PluginManager.kt` | Discovery, manifest parsing, per-tool toggles, tool assembly |
| `composeApp/src/androidMain/.../plugins/PluginConnection.kt` | Binds a plugin service and calls its tool across the process boundary |
| `composeApp/src/androidMain/.../plugins/PluginTool.kt` | Wraps one plugin tool as a native Tool implementation |
| `composeApp/src/commonMain/.../ui/settings/PluginsCard.kt` | `expect` plugin-host card (Android-only real impl) |
| `composeApp/src/androidMain/.../ui/settings/PluginsCard.android.kt` | Plugin-host UI: plugin list, tool toggles, rescan |
| `composeApp/src/commonMain/.../ui/hub/HubPages.kt` | Hosts the Plugins page in the hub |
| `composeApp/src/commonMain/.../ui/hub/PoshHub.kt` | Plugins hub tile |
| `composeApp/src/androidMain/.../Platform.android.kt` | Adds enabled plugin tools to the agent's tool list |
| `composeApp/src/androidMain/.../sandbox/SandboxModule.kt` | Provides `PluginManager` via DI |
| `androidApp/src/main/AndroidManifest.xml` | Plugin bind permission + package-visibility queries |
