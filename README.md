# POSH

A personal AI assistant built around a **full-permission Linux shell** and **on-device local models** — run real shell commands and GGUF models from Hugging Face right on your phone, no root required. Runs on Android, iOS, Windows, macOS, Linux, and Web (the shell + local-GGUF features are Android-only).

## What POSH adds over its open-source base

POSH builds on an open-source Kotlin Multiplatform assistant base (assistant, memory, sandbox, and skills — see the attribution in [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md)). On top of that base, POSH adds:

- **On-device GGUF models** — build `llama.cpp`'s server inside the Linux sandbox, pull a `.gguf` from a Hugging Face repo id or URL, and serve it locally as an OpenAI-compatible endpoint. No Ollama, no terminal typing required.
- **Hardened shell** — longer command timeouts (up to 30 min), automatic `PIP_BREAK_SYSTEM_PACKAGES` so `pip install` works in Alpine, and tool guidance that runs full multi-step scripts in one shot (Alpine `apk`, not Termux `pkg`).
- **Conversation branching** — fork a chat from any message into a new branch.
- **Skill dependency auto-install** — skills declare the packages they need and POSH installs them in the sandbox before the skill runs.
- **Skill hot-reload** — edit a skill file and it reloads live.
- **FTS5 memory search** — memories are stored in SQLite with a full-text index for fast, proper search.

## Build

```
./gradlew :androidApp:assembleFossDebug
```

The APK lands under `androidApp/build/outputs/apk/foss/debug/`. CI builds it for every push and publishes a sideloadable preview to this repo's Releases.

## License & attribution

POSH is licensed under the **Apache License 2.0** — see [`LICENSE.txt`](LICENSE.txt).

POSH is a modified fork of an upstream Apache-2.0 project; this repository contains modifications from the original, described above. The upstream attribution required by Apache-2.0, along with all third-party binary licenses, is listed in [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md).
