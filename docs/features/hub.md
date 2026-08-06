# Command-Deck Hub

**Last verified:** 2026-08-06

POSH launches into a "command deck" home screen instead of a chat-first view: a monospace, black/red HUD with system gauges and a tile grid that routes to every surface of the app. Each tile opens a dedicated full-screen page — not a settings tab.

## Tiles

| Tile | Destination |
|------|-------------|
| **CHAT** | The chat screen. |
| **TERMINAL** | A standalone live shell into the Linux sandbox (full-screen terminal, nothing else). Shows a setup hint if the sandbox isn't running. |
| **MODELS** | On-device model management: search a Hugging Face repo for GGUF files, download a quant, see all downloaded models, and run one as a local OpenAI-compatible server (Android-only card). |
| **SKILLS** | Create, save, and activate skills. A create form (name, description, instructions) writes a skill straight into the sandbox where it hot-reloads and becomes immediately invokable; below it, the installed-skills list with install-from-GitHub/browse and uninstall. |
| **PLUGINS** | Host FoneClaw-compatible plugin apps: list installed plugin APKs and the tools they declare, toggle each tool on/off, and rescan after installing one. Android-only; see [plugins.md](plugins.md). |
| **PROJECTS** | Start and resume projects. Creating a named project makes a folder under `/root/projects` in the sandbox; the project list is stored persistently in app settings, so it survives sandbox resets. Deleting a project (with confirmation) removes it from the list and deletes its folder. |
| **FILES** | The sandbox area in Settings (file browser, packages). |
| **MEMORY** | Everything the AI has remembered, as a list with per-entry delete. |
| **CONFIG** | The AI's persona and behavior: the soul/system prompt editor and the rest of the agent configuration (memory toggle, heartbeat, scheduling). |
| **SETTINGS** | The settings screen with its General, Agent, Services, Tools, and Sandbox tabs. Opens on the General tab. |

## Gauges

The ATTRIBUTES row shows RAM / disk / CPU / model-state cells. These are static placeholder values in the current build; live system stats are a planned follow-up.

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/.../ui/hub/PoshHub.kt` | The hub screen: header, gauges, tile grid |
| `composeApp/src/commonMain/.../ui/hub/HubPages.kt` | The tile destination pages (terminal, skills, plugins, config, memory, models, projects) |
| `composeApp/src/commonMain/.../App.kt` | Navigation routes wiring tiles to pages |
| `composeApp/src/commonMain/.../data/AppSettings.kt` | Persistent project-name list |
