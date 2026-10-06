# POSH


## Latest polish test APK

[Download the newest verified test APK — rolling release page](https://github.com/ether4o4/POSH/releases/tag/polish-test-latest)

This separate prerelease channel contains **test APKs**, with source commit, package/version, native ABI and SHA-256 recorded on the release page. Open **Download newest verified test APK** on that page. Phone runtime testing is incomplete; test signing may differ from an installed/store version. Private repository downloads require GitHub access.

While this PR remains unmerged, a successful **Polish verification** build on `polish/mobile-2026-10-05` refreshes the channel (remove/reapply the `polish-verify` PR label to run verification). The initial download reuses the already verified polish build. After merge, successful **Build APK** builds on `main` also refresh it through `workflow_run`. Only trusted same-repository intended workflows/refs qualify; failed, older or diverging builds leave the working download intact. Versioned APK assets remain available, avoiding a replacement gap. Existing release channels keep their current behavior.

The stable link opens a release page; the highlighted APK filename changes after each accepted build. The channel tag anchors `main`; the release body identifies the actual APK source commit. These README additions and future `main` automation take effect on the default branch only after this PR is merged.


A personal AI assistant built around a **full-permission Linux shell** and **on-device local models** — run real shell commands and GGUF models from Hugging Face right on your phone, no root required. Runs on Android, iOS, Windows, macOS, Linux, and Web (the shell + local-GGUF features are Android-only).



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

It is a modified fork of **Kai 9000** (https://github.com/SimonSchubert/Kai), Copyright the Kai contributors, also under Apache-2.0. This repository contains modifications from the original, described above. Third-party licenses are listed in [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md).
