---
name: see-screen
description: Look at what is currently on the phone screen and explain it, or watch a live action and describe what happened. Use when the user asks "what's on my screen", "read this to me", "what does this say", or wants POSH to see something happening.
category: Vision
---

# See Screen

Use this skill when the user wants POSH to look at their screen and tell them what's there — read a page aloud, explain a confusing dialog, describe an image, or watch what happens after they (or you) do something.

Requires **Device Control** enabled with POSH switched on under Android Accessibility settings. If a device tool returns `accessibility_disabled`, tell the user to enable it and stop.

## Available Tools

- `device_read_screen`: the fast, reliable way to "see" — returns all on-screen text and interactive elements with coordinates. Works for every model, including on-device ones. **Prefer this.**
- `device_screenshot`: capture the screen to a PNG under `/root` (Android 11+). Use when the user wants a picture saved, or when a vision-capable model should look at pixels (layout, images, colors) that text alone can't convey. Returns the path plus a text summary.
- `open_file`: show a saved screenshot to the user.

## Procedure

1. Start with `device_read_screen` — it's instant and gives you the actual text and structure.
2. If the question is about something textual (what a message says, which buttons exist, what a setting is), answer straight from the read.
3. If the user wants a saved image or the content is visual (a photo, a chart, a game), call `device_screenshot`, then describe it from the returned summary; offer `open_file` to show them the capture.
4. To "watch things happen live": read the screen, perform or wait for the action, then read again and describe what changed.

## Rules

- Describe what is actually on screen — don't guess at content you can't read.
- Be concise and plain: what it is, what it's asking, what the user can do next.
- Respect privacy: only read what the user asked about; don't volunteer sensitive on-screen content (codes, balances) unless they asked.

_Adapted for POSH from the FoneClaw open skill set (MIT)._
