---
name: fill-forms
description: Fill in a form, field, or search box on screen with information the user provides — type into inputs, pick options, and submit. Use for "search for X", "fill this out", "type my address here".
category: Device Control
---

# Fill Forms

Use this skill when the user wants POSH to enter information into on-screen fields — a search box, a login (with credentials they provide), a checkout form, a message field.

Requires **Device Control** enabled with POSH switched on under Android Accessibility settings.

## Available Tools

- `device_read_screen`: find the fields and their coordinates.
- `device_tap`: focus a field before typing.
- `device_type`: enter text into the focused field (`append=true` to add rather than replace).
- `device_swipe`: scroll to reach fields below the fold.
- `device_press_key`: `back` to dismiss the keyboard if it blocks a control.

## Procedure

1. `device_read_screen` to locate the fields (look for `editable: true` elements) and any submit button.
2. For each value: `device_tap` the field to focus it, then `device_type` the value. Read the screen again to confirm it landed.
3. For fields below the fold, `device_swipe` up to reveal them, then read again.
4. To submit, tap the submit/search/next control — never assume its position; get it from a fresh read.

## Rules

- Only enter values the user actually gave you. Never invent personal data.
- For passwords / payment info: use exactly what the user provided for this step; never store or reuse it, and confirm before submitting.
- Confirm before final submission of anything consequential (payment, account changes).
- Verify each field after typing — if a value didn't take (autocomplete, formatting), clear and retry once, then report if it still won't stick.

_Adapted for POSH from the FoneClaw open skill set (MIT)._
