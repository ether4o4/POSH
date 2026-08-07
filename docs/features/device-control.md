# Device Control

**Last verified:** 2026-08-06

POSH can operate the phone on the user's behalf — see what's on screen, tap, long-press, swipe, type into fields, press the system navigation keys, launch apps, and capture the screen. This capability is built into POSH through an accessibility service, not a separate plugin APK (though the [plugin host](plugins.md) can still bind external plugins on top).

Device control is **Android-only** and ships in the **FOSS flavor only** (Play Store policy restricts accessibility services and all-apps visibility). On other platforms and the Play build the feature is absent.

## How it works

The capability is powered by an accessibility service. Android only lets an accessibility service act after the user switches it on under **Settings → Accessibility**, so the feature has three independent gates, all of which must be true before the tools are offered to the model:

1. The accessibility service is declared in the build (FOSS only).
2. The **Device Control** master toggle is on (Settings → Agent → Device Control).
3. The user has switched POSH on in the system Accessibility settings.

Each tool also checks at call time that the service is actually bound, and if not returns a plain "device control is off — ask the user to enable it" hint instead of failing silently, so the model can guide the user rather than loop.

The service observes nothing on its own — it requests no event stream and only acts when a tool invokes it. It never runs in the background watching the screen.

## Tools

| Tool | What it does |
| --- | --- |
| `device_read_screen` | Lists the on-screen text and interactive elements, each with a center x,y tap point. The primary "see the screen" path — works for every model, including on-device ones. |
| `device_tap` / `device_long_press` | Press an element at a coordinate. |
| `device_swipe` | Swipe/scroll/drag between two coordinates. |
| `device_type` | Enter text into the focused field (tap the field first; supports append). |
| `device_press_key` | Back, home, recents, notifications, quick settings, lock. |
| `device_screenshot` | Capture the screen to a PNG under `/root` (Android 11+), viewable via `open_file`. Also returns a text summary of on-screen elements. |
| `device_open_app` | Launch an installed app by name or package id. |

The read → act → re-read loop is the intended workflow: the model reads the screen to get coordinates, performs one action, then reads again to confirm before the next step. Coordinates are never assumed — layouts shift, so each action is preceded by a fresh read.

### On-device models

`device_read_screen`, `device_tap`, `device_swipe`, `device_type`, `device_press_key`, and `device_open_app` are on the on-device tool allowlist, so a local GGUF/LiteRT model can drive the phone too. `device_screenshot` and `device_long_press` are remote-only — an image path is of little use to a text-only local model, and a smaller tool set keeps the local model's function-calling reliable.

## Skills

The Skills page ships pre-installed capability skills that teach the model to use these tools well, grouped into categories with per-skill on/off toggles (see [skills.md](skills.md)):

- **Device Control**: `drive-apps` (open an app and complete a task through it), `fill-forms` (enter data into fields and submit).
- **Vision**: `see-screen` (look at the screen and explain, or watch a live action).

## Permissions walkthrough

The Device Control settings card shows the master toggle plus a live permission status. When the accessibility permission isn't granted, its button deep-links straight into the Android Accessibility settings screen (highlighting POSH's row where the OEM supports it) so the user can switch it on in one tap and return. The status updates automatically on return.

`device_open_app` additionally relies on `QUERY_ALL_PACKAGES` (declared in the FOSS manifest) to enumerate and launch installed apps; without it the tool simply finds nothing to launch.

## Limitations

- **Android FOSS only.** No device control on the Play build, desktop, iOS, or web.
- **Coordinates, not semantics, for tapping.** The model taps pixel coordinates obtained from `device_read_screen`; a stale read can mistap, which is why the skills enforce read-before-tap.
- **Screenshot needs Android 11+** (`AccessibilityService.takeScreenshot`).
- **No PTY-style streaming** — actions are discrete calls; "watching live" is a read → act → read loop, not a video feed.
- The service acts with the user's own permissions; it cannot do anything the user couldn't do by tapping themselves, and never enters credentials or payment details unless the user provides them for that step.

## Key Files

| File | Purpose |
| --- | --- |
| `composeApp/src/androidMain/kotlin/com/inspiredandroid/kai/accessibility/PoshAccessibilityService.kt` | The accessibility service: gesture dispatch (tap/long-press/swipe), focused-field text entry, global-action keys, active-window node-tree read, API-30 screenshot. Exposes a volatile singleton the tools reach. |
| `composeApp/src/androidMain/kotlin/com/inspiredandroid/kai/tools/DeviceControlTools.kt` | The `device_*` tools wrapping the service, with graceful "not enabled" errors. |
| `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/tools/AccessibilityController.kt` + `*.android.kt` | Reports supported/enabled/running and deep-links into Settings → Accessibility. No-op actuals off-Android. |
| `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/ui/settings/DeviceControlCard.kt` + `*.android.kt` | The Device Control settings card (master toggle, live permission status, deep-link button, capability list). |
| `composeApp/src/androidMain/kotlin/com/inspiredandroid/kai/Platform.android.kt` | Registers the device tools (triple-gated) and their Settings definitions. |
| `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/data/RemoteDataRepository.kt` | `LOCAL_TOOL_ALLOWLIST` — the device tools exposed to on-device models. |
| `androidApp/src/foss/AndroidManifest.xml` | Declares the accessibility service and `QUERY_ALL_PACKAGES` (FOSS only). |
| `androidApp/src/main/res/xml/accessibility_service_config.xml` | Accessibility service config (window content, gestures, screenshot; no event stream). |
