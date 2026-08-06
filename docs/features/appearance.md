# Appearance

**Last verified:** 2026-08-06

POSH uses one strict **red, white, and black** visual system on every platform. Screens and startup frames are black, primary text is white, and interaction emphasis is red. Wallpaper-derived Material You colors are disabled so the palette cannot drift.

The four-way theme picker (**System**, **Light**, **Dark**, **OLED**) remains for settings compatibility. Every option resolves to the same black background; OLED therefore has no visual color difference. Cards and hub tiles preserve the existing hub layout as transparent or near-black panels with red borders.

## Text boxes

Text input fields use black containers and white entered text. Focused borders, cursors, floating labels, selection handles, and selection fills are red. Unfocused and disabled text use white or neutral gray with reduced opacity.

## Controls and dynamic UI

Material controls inherit the central scheme: active switches, checkboxes, sliders, progress indicators, selected segments, buttons, links, icons, and focus states use red. Inactive content uses white, neutral gray, black, or near-black. Dynamic UI components must use `MaterialTheme.colorScheme` instead of introducing literal off-palette colors.

## Terminal

Terminal ANSI blue and cyan entries are remapped to red-family or neutral entries. Extended 256-color values whose blue channel would dominate are sanitized before rendering; escape parsing and terminal behavior remain unchanged.

## Component guidance

Use shared theme colors and shared text-field helpers. New UI must not add blue, cyan, teal, or wallpaper-derived colors. Raised surfaces should use black, near-black, dark gray, or dark red so they remain distinct from the page.

## Key Files

| File | Purpose |
|------|---------|
| `composeApp/.../ui/Theme.kt` | Central black background, white content, red accents, and shared text-field styling |
| `composeApp/.../ui/settings/AnsiParser.kt` | ANSI terminal parsing with POSH-safe blue/cyan remapping |
| `composeApp/.../ui/components/PoshLogo.kt` | Existing POSH P-chevron logo |
| `composeApp/.../App.kt` | Applies the selected theme mode |
| `androidApp/.../res/values/styles.xml` | Android pre-Compose black startup frame |
| `androidApp/.../res/values-night/styles.xml` | Android night startup frame |
| `composeApp/src/wasmJsMain/resources/styles.css` | Web startup frame and red loader accent |
| `iosApp/iosApp/LaunchScreen.storyboard` | iOS black launch background |
