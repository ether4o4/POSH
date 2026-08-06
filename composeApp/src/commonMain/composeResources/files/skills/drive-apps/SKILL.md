---
name: drive-apps
description: Operate apps on the phone for the user — open an app and tap, scroll, and navigate through it to complete a task (open a chat, change a setting, play something, etc.). Use when the user asks POSH to actually do something on their phone.
category: Device Control
---

# Drive Apps

Use this skill when the user asks POSH to *do* something on their phone that lives in an app — "open WhatsApp and message Sam", "turn on Bluetooth", "play my liked songs on Spotify", "add this to my cart". You control the phone through the device-control tools.

Requires **Device Control** to be enabled (Settings → Agent → Device Control) with POSH switched on under Android Accessibility settings. If a device tool returns `accessibility_disabled`, stop and tell the user to enable it — do not keep retrying.

## Available Tools

- `device_read_screen`: list the on-screen elements and their center `x,y`. **Your eyes** — call it before tapping.
- `device_open_app`: launch an app by name or package.
- `device_tap` / `device_long_press`: press an element at `x,y`.
- `device_swipe`: scroll or drag (swipe up to scroll down, etc.).
- `device_type`: enter text into the focused field (tap the field first).
- `device_press_key`: `back`, `home`, `recents`, `notifications`, `quick_settings`, `lock`.

## Procedure

1. **Look before you leap.** Call `device_read_screen` to see the current state. Never tap a coordinate you didn't get from a fresh read — layouts shift.
2. Open the target app with `device_open_app` if you're not already in it.
3. Work one step at a time: read the screen → decide the single next action → do it → read again to confirm it worked before the next step.
4. To enter text: `device_tap` the field first so it's focused, then `device_type`.
5. To scroll: `device_swipe` within the scrollable area; read again to see newly-revealed content.
6. Use `device_press_key back` to back out of wrong screens; `home` to reset to the launcher.

## Rules

- Re-read the screen after every action that changes it. Do not fire a sequence of blind taps.
- Confirm before anything destructive or costly — sending a message, making a purchase, deleting data. Describe what you're about to do and get a yes.
- If you're stuck (an element isn't where expected after two reads), stop and report what you see rather than tapping randomly.
- Never enter passwords, payment details, or 2FA codes on the user's behalf unless they explicitly provide them for that exact step.
- Keep the user informed: briefly narrate what you did ("opened Settings → tapped Bluetooth → turned it on").

_Adapted for POSH from the FoneClaw open skill set (MIT)._
