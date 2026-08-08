# Voice

**Last verified:** 2026-08-06

POSH supports hands-free interaction in two directions: it can **speak** assistant
replies aloud, and on Android it can **listen** and dictate the user's speech into
the chat input. Both favor working offline where the device supports it, and
neither bundles a native speech model or library — they drive the platform's own
speech facilities.

## Speech output (read-aloud)

Assistant messages can be played as speech, either automatically for each new
reply (top-bar toggle) or on demand per message. Markdown is stripped so only the
prose is spoken. This is described from the chat side in [chat.md](chat.md).

POSH speaks through the **device's default text-to-speech engine** instead of
requiring one specific vendor's engine. On de-Googled or FOSS phones a
Google-branded engine is frequently absent; requiring it left POSH silent on
those devices. Using the system default means read-aloud works with whatever
engine the user has — the stock one, or an offline/neural engine they side-load —
and picks up their configured voice and language automatically.

## Voice input (dictation)

On Android, when the chat input is empty a **mic button** appears in place of the
send button. Tapping it starts a speech-recognition session; the recognized text
is inserted into the input field, where the user can edit it and then send as
usual. If there is already text in the field, dictated speech is appended to it
rather than replacing it. Tapping the mic again while listening cancels the
session. The mic button pulses while POSH is actively listening.

Recognition **prefers the on-device recognizer**, so where the device supports
offline recognition it keeps working without a network connection. There is no
bundled model and no native library — POSH uses the platform speech recognizer.

### Permissions and availability

- Voice input needs the **microphone** permission. The first time the user taps
  the mic, POSH requests it; if granted, listening starts immediately. If the
  permission is denied, no dictation happens and the app is otherwise unaffected.
- The mic button only appears when the device actually offers speech recognition.
  On devices without it — and on desktop, web, and iOS — there is no mic
  affordance at all.
- Under modern Android package-visibility rules POSH declares that it looks for a
  recognition service so the recognizer stays visible to it.

## Platform support

| Capability | Android | Desktop | Web | iOS |
|---|---|---|---|---|
| Read-aloud (TTS) | Yes (device default engine) | Yes | Yes | Yes |
| Voice input (dictation) | Yes (offline-preferring) | No | No | No |

Voice input is Android-only; the shared code returns no controller on other
platforms, so the mic button never shows there.

## Key Files

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../ui/chat/VoiceInput.kt` | `expect` voice-input controller + factory |
| `composeApp/src/androidMain/.../ui/chat/VoiceInput.android.kt` | Android speech-recognizer implementation (offline-preferring) |
| `composeApp/src/desktopMain/.../ui/chat/VoiceInput.jvm.kt` | No-op actual (no dictation) |
| `composeApp/src/wasmJsMain/.../ui/chat/VoiceInput.wasmJs.kt` | No-op actual (no dictation) |
| `composeApp/src/iosMain/.../ui/chat/VoiceInput.ios.kt` | No-op actual (no dictation) |
| `composeApp/src/commonMain/.../ui/chat/composables/QuestionInput.kt` | Shows the mic button and routes recognized text into the input |
| `androidApp/src/main/kotlin/com/inspiredandroid/kai/MainActivity.kt` | Selects the device-default TTS engine |
| `androidApp/src/main/AndroidManifest.xml` | `RECORD_AUDIO` permission + recognition-service visibility query |
