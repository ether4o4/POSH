# Appearance

**Last verified:** 2026-07-31

POSH uses a **red / white / black** brand scheme. The light theme is a white canvas with black content and red accents; the dark theme is a near-black canvas with white content and red accents. The app icon and in-app logo are a white shell prompt (">_") on a red rounded square — the animated purple two-circle mark inherited from the upstream app (and the matching purple launcher icon) were removed, including from the chat empty state.

A four-way theme picker — **System**, **Light**, **Dark**, and **OLED** — is exposed in Settings on every platform. The default is System, which follows the operating system's dark/light preference. The other three force a specific theme regardless of system state. OLED flattens the background and the lowest surface tier to pure black for users who want to save power on OLED panels.

## Text boxes

All text input fields — the chat input, API-key and base-URL fields, search fields, the sandbox file editor, model-download fields, and dynamic-UI form inputs — render as **black boxes with cyan text**, in both light and dark themes. The cursor, floating label, placeholder (dimmed), and text selection are cyan; the border is cyan when focused and dark gray otherwise. This styling is centralized in one shared color definition, so new text fields pick it up by using the shared text-field components or color helper.

## Behavior

- **System**: the OS dark/light preference decides between the light and dark schemes.
- **Light**: white background, black text, red primary accent.
- **Dark**: near-black background (`#0A0A0A`) with slightly lighter surfaces, white text, red primary accent. The background is deliberately not pure black so it stays distinct from OLED mode.
- **OLED**: dark + pure-black override. Background, surface, and the lowest surface tier render pure black; elevated container tiers are unchanged so cards and menus stay visible against black. Cards switch to a transparent-with-outline style in this mode.
- **Reactivity**: changing the theme picker recomposes the theme immediately without an app restart.

The picker exists on every platform because system theme detection is unreliable on some desktop window systems (notably Linux/Wayland), so users there need an explicit override.

## Component guidance

When adding new surfaces in dark mode, **do not** bind fills to `surface` if the element should stand out from the page background with OLED selected — in OLED `surface` becomes black and the element will be invisible against the background. Use `surfaceContainer` (or higher) for anything that represents a raised card, pill, or control.

New text inputs should use the shared text-field components (or the shared text-field color helper) rather than default Material colors, so they get the black/cyan treatment. Avoid setting an explicit color on a text field's label — an explicit dark label is invisible against the black field container in the light theme.

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/.../ui/Theme.kt` | Light/dark color schemes, red accent constants, black/cyan text-field colors, pure-black OLED override |
| `composeApp/.../ui/components/PoshLogo.kt` | The ">_" logo mark used in the chat empty state and interactive-mode welcome |
| `composeApp/.../data/AppSettings.kt` | Theme mode enum and persistent setting, with one-time migration from the legacy OLED boolean |
| `composeApp/.../App.kt` | Shared app content — observes the theme setting and picks the light, dark, or black-flattened dark scheme |
| `composeApp/.../ui/settings/SettingsScreen.kt` | Theme mode dropdown in the General tab |
| `androidApp/.../MainActivity.kt` | Android entry — the resolved dark/light state drives the system-bar style |
| `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` | Launcher icon glyph (white ">_" prompt) |
| `androidApp/src/main/res/values/ic_launcher_background.xml` | Launcher icon background (red) |
| `androidApp/.../res/values-night/styles.xml` | Pre-Compose window background matching the dark frame |
| `composeApp/.../desktopMain/.../main.kt` | Desktop entry — window title, HiDPI hints, initial window size |
