---
name: device-brief
description: Prepare a concise daily brief for the user — time, upcoming calendar events, recent notifications, and anything remembered about their day. Use for morning checks, evening recaps, or a quick status summary.
category: Daily
---

# Device Brief

## Scope

Use this skill when the user asks for a quick daily briefing, morning check, evening recap, "catch me up", or a compact status summary of their phone and day.

Do not use this skill for long research tasks, or for tasks that require sending messages, changing settings, or deleting data.

## Available Tools

Use the POSH tools that are actually enabled on this device. Depending on the user's settings and granted permissions, these may include:

- `get_local_time`: The current local date and time — always start here to frame the brief.
- Calendar tools: list upcoming events in a time window (when the calendar tool is enabled).
- Notification tools: read recent notifications the user may have missed (when notification access is granted).
- Memory tools: recall things the user asked POSH to remember (reminders, context about their day).
- The Linux sandbox shell: for anything scriptable the user asks to include.

## Procedure

1. Call `get_local_time` first so the brief is anchored to now.
2. If the calendar tool is available, list events for the relevant window (today by default; the day ahead for a morning brief, the day just passed for an evening recap).
3. If notification access is available and the user wants missed items, read recent notifications and surface only the actionable ones.
4. If memory is enabled, recall any stored reminders or day-context relevant to the window.
5. Only use tools that are actually available — if a source is not enabled or permitted, continue with what you have and state briefly what was not covered.

## Output Rules

- Keep it concise and practical — this is a glance, not a report.
- Group by: what needs action, what's scheduled, updates, and a suggested next step.
- Do not dump raw notification, calendar, or memory content unless the user asks for detail.
- Do not change any device settings from this skill.
- If a source could not be read (permission or feature off), name it in one short line.

_Bundled with POSH._
