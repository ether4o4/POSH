# Local Models (GGUF)

**Last verified:** 2026-08-08

POSH can download and run any GGUF-format model fully on-device, served from inside the Linux sandbox as an OpenAI-compatible endpoint on `127.0.0.1:8080`. This is POSH's headline local-model engine and is Android-only (it needs the sandbox).

It is **separate from the LiteRT engine** described in [on-device-inference.md](on-device-inference.md): that engine runs Gemma-family models in-process and appears in the service picker as **"Local Model"**; this engine runs GGUF models behind a loopback HTTP server and appears in the picker as **"OpenAI-Compatible API"**. The two share no models, storage, or downloads. The GGUF models card states this distinction so users don't look for their GGUF models under "Local Model".

## Lifecycle

1. **Set up engine** — downloads a prebuilt inference server binary (usually under a minute); compiles from source inside the sandbox only when no prebuilt is reachable (10–30 min). The binary lives on the sandbox's internal rootfs (which allows execution); models, logs, and run state live under the sandbox home so they survive engine resets. After a sandbox reset wipes the binary while models survive, the engine quietly rebuilds itself once per app launch instead of demanding a manual re-setup.
2. **Download** — pull a `.gguf` from a Hugging Face repo id, repo URL, or direct file URL, with quick-install buttons for curated small models. Shows a live progress bar (percentage or MB) and verifies the downloaded size so truncated/corrupt files are rejected rather than served.
3. **Run** — starts the server with the model's own chat template (so answers and tool calls are well-formed), a context window of 4096 tokens, and a thread count tuned for phone big.LITTLE CPUs. A RAM preflight fails fast with a plain-language message when the model clearly won't fit in free memory.
4. **Add as service** — registers the running server as an OpenAI-compatible service instance pointing at the loopback endpoint, after which its models are pickable in chat like any other service.
5. **Stop / Delete** — stop the running server, or delete a downloaded model (with confirmation; deleting the currently-serving model stops it first).

## Local routing guarantees

A service instance pointing at the sandbox server's loopback endpoint is treated as **on-device inference**, with the same guarantees as the LiteRT engine:

- **Trimmed local system prompt** — the compact local prompt variant, not the full remote-grade prompt that overflows small models.
- **Small-model tool allowlist** — only the simple-schema tool set local models can reliably call (the same allowlist the LiteRT engine uses; see [tools.md](tools.md)).
- **Honest context budget** — the app budgets/compacts history against the server's real 4096-token window, not the 100k default assumed for unknown cloud models.
- **No silent fallback in either direction** — a failure while chatting with the local server surfaces as an error instead of quietly re-sending the conversation to a cloud provider, and a cloud failure never falls back into the local server.

## Failure reporting and recovery

Every engine operation reports a typed error with a plain-language detail, a suggested fix, and — for server failures — the tail of the server log loaded right into the error dialog.

Because a model load can take minutes and the server runs detached inside the sandbox, the app's connection to the operation can be severed (app backgrounded, shell reset) without the operation itself failing. The engine recovers the true outcome instead of guessing:

- Engine setup re-checks the on-disk state before reporting failure.
- Run/serve mirrors its final verdict to a result file, which the app reads back when the live result was lost; the app also watches process liveness and the server's health endpoint (distinguishing "still loading" from "ready" and from "died"), and only reports failure when the server demonstrably isn't coming up.
- When the server is still loading after the 5-minute in-sandbox wait, the app extends the wait briefly; if the model still isn't ready it stops the server, so the UI and reality never disagree about whether something is running.
- Shell lifecycle noise appended to an operation's output (session resets, exit-code markers) is stripped before parsing, so a succeeded operation is never misreported as an unparseable error.

## Dependency story

A fresh sandbox ships with busybox tools only. The engine's helper commands install the two small runtime dependencies they need (curl, jq) on demand and fall back to busybox equivalents where possible, so the fast prebuilt setup path — which skips the sandbox's package-install step entirely — still yields a working download + serve pipeline.

## Key Files

| File | Role |
| --- | --- |
| `composeApp/src/androidMain/assets/sandbox/morsllm.sh` | The in-sandbox engine runtime: provision, pull, serve, stop, status, health, delete |
| `composeApp/src/androidMain/kotlin/com/inspiredandroid/kai/sandbox/GgufServerManager.kt` | Typed orchestration over the runtime; operation state for the UI; recovery logic |
| `composeApp/src/androidMain/kotlin/com/inspiredandroid/kai/ui/settings/GgufModelsCard.android.kt` | The Local Models (GGUF) card: setup, download, run/stop/delete, add-as-service |
| `composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/data/RemoteDataRepository.kt` | Local routing guarantees for the loopback service instance |
| `docs/features/on-device-inference.md` | The separate LiteRT engine this one is often confused with |
