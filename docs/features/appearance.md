# Appearance

**Last verified:** 2026-08-04

POSH is **deep-teal everywhere**: a dark "abyssal teal" ink background (a near-black teal) with white text, red accents, and red outlines. Teal is the opposite of red on the color wheel, so the red accents read hotter and pop harder than they did on a flat black screen, and the tint quietly rhymes with the cyan text-field accent — while staying dark enough that white body text stays crisp. Every theme-picker mode resolves to this same palette — "Light" is not a white theme — and wallpaper-derived Material You dynamic colors are disabled on Android so the brand scheme always wins. Because the background is a single brand color (like pure black before it), cards render as transparent panels with red outline borders rather than gray fills. The app icon and in-app logo are a white "P" letterform whose bowl carries a red ">" chevron with a black shadow edge, on a red field.

The four-way theme picker (**System**, **Light**, **Dark**, **OLED**) still exists in Settings. **OLED** flattens the background to pure black for AMOLED power saving; the other three modes all resolve to the deep-teal brand scheme.

## Text boxes

All text input fields — the chat input, API-key and base-URL fields, search fields, the sandbox file editor, model-download fields, and dynamic-UI form inputs — render as **black boxes with cyan text**, in both light and dark themes. The cursor, floating label, placeholder (dimmed), and text selection are cyan; the border is cyan when focused and dark gray otherwise. This styling is centralized in one shared color definition, so new text fields pick it up by using the shared text-field components or color helper.

## Behavior

- The three non-OLED picker modes resolve to the teal/red/white palette: background and surfaces the deep-teal brand ink, text white, primary accent red, outlines red (bright red focused, dark red for card borders). OLED flattens background and surfaces to pure black.
- Cards, service rows, and hub tiles render transparent with a red/white hairline border against the brand background (teal, or pure black under OLED).
- **Reactivity**: changing the theme picker recomposes immediately without an app restart (though visually all modes currently match).

## Component guidance

When adding new surfaces in dark mode, **do not** bind fills to `surface` if the element should stand out from the page background with OLED selected — in OLED `surface` becomes black and the element will be invisible against the background. Use `surfaceContainer` (or higher) for anything that represents a raised card, pill, or control.

New text inputs should use the shared text-field components (or the shared text-field color helper) rather than default Material colors, so they get the black/cyan treatment. Avoid setting an explicit color on a text field's label — an explicit dark label is invisible against the black field container in the light theme.

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/.../ui/Theme.kt` | Light/dark color schemes, `brandBackground` deep-teal constant, red accent constants, black/cyan text-field colors, pure-black OLED override |
| `composeApp/.../ui/components/PoshLogo.kt` | The P-chevron logo mark used in the chat empty state and interactive-mode welcome |
| `composeApp/.../data/AppSettings.kt` | Theme mode enum and persistent setting, with one-time migration from the legacy OLED boolean |
| `composeApp/.../App.kt` | Shared app content — observes the theme setting and picks the light, dark, or black-flattened dark scheme |
| `composeApp/.../ui/settings/SettingsScreen.kt` | Theme mode dropdown in the General tab |
| `androidApp/.../MainActivity.kt` | Android entry — the resolved dark/light state drives the system-bar style |
| `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` | Launcher icon glyph (white P with red chevron) |
| `androidApp/src/main/res/values/ic_launcher_background.xml` | Launcher icon background (red) |
| `androidApp/.../res/values-night/styles.xml` | Pre-Compose window background matching the dark frame |
| `composeApp/.../desktopMain/.../main.kt` | Desktop entry — window title, HiDPI hints, initial window size |
