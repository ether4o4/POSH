package com.inspiredandroid.kai.sandbox

import android.content.Context
import com.inspiredandroid.kai.SandboxController
import com.inspiredandroid.kai.SandboxSessions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Typed orchestration over the `morsllm` shell runtime. Installs the script into
 * the sandbox the first time it's needed, then drives provision/pull/serve/stop
 * through the SYSTEM shell session so long-running operations don't block any
 * chat's own shell. JSON-emitting subcommands are run with stderr suppressed so
 * the returned string is a parseable JSON object.
 */
class GgufServerManager(
    private val context: Context,
    private val sandbox: SandboxController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val installLock = Mutex()

    @Volatile
    private var scriptInstalled = false

    val openAiBaseUrl: String = "http://127.0.0.1:8080/v1"

    /**
     * State of the current long-running engine/model operation. Owned by the
     * manager (an app-scoped singleton) rather than the screen, so a build or
     * download keeps running — and stays observable — after the user navigates
     * away and comes back. [Running] carries a label for the busy UI; [Done] and
     * [Failed] are terminal until the UI acknowledges them.
     */
    sealed interface EngineOp {
        data object Idle : EngineOp
        /** [progress] is 0f..1f when a determinate value is known (model download),
         *  or null for an indeterminate spinner. */
        data class Running(val label: String, val progress: Float? = null) : EngineOp
        data class Done(val message: String) : EngineOp
        data class Failed(val result: GenericResult) : EngineOp
    }

    private val _op = MutableStateFlow<EngineOp>(EngineOp.Idle)
    val op: StateFlow<EngineOp> = _op.asStateFlow()

    /**
     * Launch a long op on the manager's own scope. No-op if one is already in
     * flight (prevents a second build/download from a double tap or a re-entered
     * screen). The op runs to completion regardless of UI lifecycle.
     */
    private fun launchOp(label: String, block: suspend () -> EngineOp) {
        if (_op.value is EngineOp.Running) return
        _op.value = EngineOp.Running(label)
        scope.launch {
            _op.value = try {
                block()
            } catch (e: CancellationException) {
                _op.value = EngineOp.Idle
                throw e
            } catch (e: Exception) {
                EngineOp.Failed(GenericResult(ok = false, error = e.message ?: "operation_failed"))
            }
        }
    }

    fun startProvision() = launchOp("Setting up engine… usually under a minute; up to 30 min if it has to compile from source") {
        val r = provision()
        if (r.ok) EngineOp.Done("Engine ready") else EngineOp.Failed(r)
    }

    fun startPull(repoOrUrl: String, quant: String? = null) = launchOp("Downloading model…") {
        val r = pull(repoOrUrl, quant)
        if (r.ok) EngineOp.Done("Downloaded ${r.file ?: "model"}") else EngineOp.Failed(r)
    }

    fun startServe(modelFilename: String) = launchOp("Starting $modelFilename…") {
        val r = serve(modelFilename)
        if (r.ok) EngineOp.Done("Running. Tap \"Add as service\" below.") else EngineOp.Failed(r)
    }

    fun startStop() = launchOp("Stopping…") {
        val r = stop()
        if (r.ok) EngineOp.Done("Stopped") else EngineOp.Failed(r)
    }

    fun startDelete(modelFilename: String) = launchOp("Deleting $modelFilename…") {
        val r = deleteModel(modelFilename)
        if (r.ok) EngineOp.Done("Deleted $modelFilename") else EngineOp.Failed(r)
    }

    /** Clear a terminal [EngineOp.Done]/[EngineOp.Failed] back to idle once the UI
     *  has shown it, so it doesn't re-appear on the next visit to the screen. */
    fun acknowledgeOp() {
        if (_op.value is EngineOp.Done || _op.value is EngineOp.Failed) {
            _op.value = EngineOp.Idle
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Serializable
    data class Status(
        val ok: Boolean = true,
        val provisioned: Boolean = false,
        val running: Boolean = false,
        val pid: String = "",
        val port: String = "",
        val model: String = "",
        @SerialName("base_url") val baseUrl: String = "",
    )

    @Serializable
    data class QuantFile(
        val name: String,
        val size: Long = 0,
        val quant: String? = null,
    )

    @Serializable
    data class ListQuantsResult(
        val ok: Boolean = false,
        val repo: String? = null,
        val files: List<QuantFile> = emptyList(),
        val error: String? = null,
    )

    @Serializable
    data class ModelFile(
        val name: String,
        val path: String,
        val size: Long = 0,
    )

    @Serializable
    data class ListModelsResult(
        val ok: Boolean = false,
        val models: List<ModelFile> = emptyList(),
        val error: String? = null,
    )

    @Serializable
    data class GenericResult(
        val ok: Boolean = false,
        val error: String? = null,
        val detail: String? = null,
        @SerialName("log_path") val logPath: String? = null,
        val hint: String? = null,
        @SerialName("base_url") val baseUrl: String? = null,
        val pid: Long? = null,
        val port: Int? = null,
        val model: String? = null,
        val file: String? = null,
        val path: String? = null,
        val size: Long? = null,
        @SerialName("already_built") val alreadyBuilt: Boolean? = null,
    )

    private suspend fun ensureScriptInstalled(): Boolean {
        if (scriptInstalled) return true
        installLock.withLock {
            if (scriptInstalled) return true
            // Sandbox isn't always fully responsive the instant its status flag
            // flips to ready — writeTextFile + executeCommand can intermittently
            // come back garbled or refuse on the first try after a cold mount.
            // Retry up to 3 times with a brief backoff so the first script call
            // after app launch doesn't race the install and report
            // "not built yet" until the user manually re-taps Set up engine.
            for (attempt in 1..3) {
                if (installScriptAsset("sandbox/morsllm.sh", SCRIPT_PATH)) {
                    // Best-effort install of the manual-recovery helper. Failure
                    // here doesn't block provisioning — it's only an escape
                    // hatch the user can invoke from the terminal.
                    installScriptAsset("sandbox/morsllm-setup.sh", SETUP_SCRIPT_PATH)
                    scriptInstalled = true
                    return true
                }
                kotlinx.coroutines.delay(1500L * attempt)
            }
            return false
        }
    }

    private suspend fun installScriptAsset(asset: String, path: String): Boolean {
        val raw = readAssetText(asset) ?: return false
        // Strip CR so a CRLF-mangled asset can't turn the shebang / `set`
        // lines into "command not found" or syntax errors under bash.
        val script = raw.replace("\r\n", "\n").replace("\r", "\n")
        // Clear anything stale at the target first (file, dir, or symlink).
        sandbox.executeCommand(
            command = "rm -rf $path 2>/dev/null; mkdir -p \$(dirname $path)",
            sessionId = SandboxSessions.SYSTEM,
        )
        val written = sandbox.writeTextFile(path, script)
        if (!written) return false
        sandbox.executeCommand(
            command = "chmod 755 $path",
            sessionId = SandboxSessions.SYSTEM,
        )
        val verify = sandbox.executeCommand(
            command = "test -x $path && echo INSTALL_OK",
            sessionId = SandboxSessions.SYSTEM,
        )
        return verify.contains("INSTALL_OK")
    }

    private fun readAssetText(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private suspend fun runQuick(subcommand: String): String {
        if (!ensureScriptInstalled()) return SCRIPT_INSTALL_FAILED_JSON
        return sandbox.executeCommand(
            command = "$SCRIPT_PATH $subcommand 2>/dev/null",
            sessionId = SandboxSessions.SYSTEM,
        ).trim()
    }

    private suspend fun runStreaming(
        subcommand: String,
        onStderrLine: (String) -> Unit = {},
    ): String {
        if (!ensureScriptInstalled()) return SCRIPT_INSTALL_FAILED_JSON
        val stdoutBuf = StringBuilder()
        // stderr arrives in arbitrary chunks; buffer and dispatch whole lines so
        // callers can parse line-oriented progress (PROGRESS <pct> <cur> <total>).
        val stderrBuf = StringBuilder()
        val handle = sandbox.executeCommandStreaming(
            command = "$SCRIPT_PATH $subcommand",
            onStdout = { synchronized(stdoutBuf) { stdoutBuf.append(it) } },
            onStderr = { chunk ->
                synchronized(stderrBuf) {
                    stderrBuf.append(chunk)
                    var nl = stderrBuf.indexOf("\n")
                    while (nl >= 0) {
                        val line = stderrBuf.substring(0, nl)
                        stderrBuf.delete(0, nl + 1)
                        onStderrLine(line)
                        nl = stderrBuf.indexOf("\n")
                    }
                }
            },
            sessionId = SandboxSessions.SYSTEM,
        )
        handle.awaitExit()
        val captured = synchronized(stdoutBuf) { stdoutBuf.toString() }
        return captured.lines()
            .lastOrNull { it.trim().startsWith("{") }
            ?.trim()
            ?: ""
    }

    suspend fun status(): Status {
        val st = decodeOr(runQuick("status"), Status())
        // Reconcile the keep-alive foreground service with reality: the in-sandbox
        // idle watchdog can stop the server without the app knowing, so any status
        // read that reports "not running" also clears the service (and its
        // notification). A running server (re)asserts it — cheap and idempotent.
        setServingForeground(st.running)
        return st
    }

    /** Hold or release the foreground service that keeps a serving model from
     *  being OOM-killed. Idempotent; no-op if the state is unchanged. */
    @Volatile
    private var servingForeground = false
    private fun setServingForeground(on: Boolean) {
        if (on == servingForeground) return
        servingForeground = on
        if (on) GgufServerService.start(context) else GgufServerService.stop(context)
    }

    suspend fun listModels(): ListModelsResult = decodeOr(runQuick("list-models"), ListModelsResult(ok = false, error = "decode_failed"))

    suspend fun listQuants(repo: String): ListQuantsResult = decodeOr(
        runQuick("list-quants ${shellQuote(repo)}"),
        ListQuantsResult(ok = false, error = "decode_failed"),
    )

    suspend fun provision(): GenericResult {
        val raw = runStreaming("provision") { line -> applyProgressLine(line, "Setting up engine") }
        val r = decodeOr(raw, GenericResult(ok = false, error = "provision_unparseable"))
        if (r.ok) return r
        // The streaming call can come back unparseable if it was interrupted (app
        // backgrounded, shell reset) even though the binary actually installed.
        // Re-check the real on-disk status before reporting failure — this kills the
        // spurious provision_unparseable a user otherwise hits on retry.
        if (status().provisioned) return GenericResult(ok = true)
        return r
    }

    /** Parse a `PROGRESS <pct> <cur> <total>` line into a live [EngineOp.Running]. */
    private fun applyProgressLine(line: String, labelPrefix: String) {
        val t = line.trim()
        if (!t.startsWith("PROGRESS ")) return
        val parts = t.split(" ")
        val pct = parts.getOrNull(1)?.toIntOrNull() ?: return
        _op.value = if (pct in 0..100) {
            EngineOp.Running("$labelPrefix… $pct%", pct / 100f)
        } else {
            val mb = parts.getOrNull(2)?.toLongOrNull()?.let { it / (1024 * 1024) }
            EngineOp.Running(if (mb != null) "$labelPrefix… $mb MB" else "$labelPrefix…", null)
        }
    }

    suspend fun pull(repoOrUrl: String, quant: String? = null): GenericResult {
        val args = if (quant.isNullOrBlank()) {
            shellQuote(repoOrUrl)
        } else {
            "${shellQuote(repoOrUrl)} ${shellQuote(quant)}"
        }
        // morsllm emits `PROGRESS <pct> <downloaded> <total>` to stderr each second
        // during the download; applyProgressLine turns it into a live progress bar.
        val raw = runStreaming("pull $args") { line -> applyProgressLine(line, "Downloading model") }
        return decodeOr(raw, GenericResult(ok = false, error = "pull_unparseable"))
    }

    @Serializable
    data class HealthResult(
        val ok: Boolean = false,
        val healthy: Boolean = false,
        val port: Int = DEFAULT_PORT,
    )

    /** Is the served model actually answering /health right now? Distinguishes
     *  "process alive but still loading" from "ready". */
    suspend fun health(): Boolean = decodeOr(runQuick("health"), HealthResult()).healthy

    suspend fun serve(modelFilename: String, port: Int = 8080): GenericResult {
        val portArg = if (port == DEFAULT_PORT) "" else " --port $port"
        // Clear the previous serve's mirrored result up front (the script also
        // truncates it) so anything recovered from the file afterwards is
        // unambiguously from THIS run.
        sandbox.executeCommand("rm -f $SERVE_RESULT_PATH", SandboxSessions.SYSTEM)
        val r = decodeOr(
            runStreaming("serve ${shellQuote(modelFilename)}$portArg"),
            GenericResult(ok = false, error = "serve_unparseable"),
        )
        if (r.ok) {
            setServingForeground(true)
            return r
        }
        if (r.error == "serve_unparseable") {
            // The streaming shell connection was severed (app backgrounded during
            // a multi-minute model load, shell reset) — the script and the
            // detached llama-server may both still be alive and fine. Recover the
            // real outcome: the script mirrors its final JSON to a result file,
            // and process liveness + /health are directly observable.
            val recovered = recoverServeOutcome(modelFilename, port, useResultFile = true, deadlineMs = 240_000L)
            if (recovered != null) {
                if (recovered.ok) setServingForeground(true)
                return recovered
            }
            return r.copy(
                detail = r.detail ?: "The serve command's output was lost (the sandbox shell connection was interrupted) and the server did not come up afterwards. The log below usually shows why.",
                logPath = r.logPath ?: SERVER_LOG_PATH,
            )
        }
        if (r.error == "health_timeout") {
            // The script gave up after 5 minutes but deliberately left the server
            // loading. Give it a bounded extra window; if it comes healthy, this
            // serve SUCCEEDED. If not, stop it so the UI and reality agree —
            // previously this path showed a Failed dialog while a zombie server
            // kept loading, and the natural retry tap killed it moments before
            // it would have finished.
            val recovered = recoverServeOutcome(modelFilename, port, useResultFile = false, deadlineMs = 180_000L)
            if (recovered != null) {
                if (recovered.ok) setServingForeground(true)
                return recovered
            }
            runCatching { stop() }
            return r
        }
        return r
    }

    /**
     * After a serve whose stream result was lost ([useResultFile]) or that timed
     * out waiting for health: watch the result file, the process, and the /health
     * endpoint until [deadlineMs]. Returns a definitive result — success once the
     * server answers /health, the script's own recovered verdict, or a diagnosed
     * server death — or null when nothing conclusive emerged before the deadline.
     */
    private suspend fun recoverServeOutcome(
        modelFilename: String,
        port: Int,
        useResultFile: Boolean,
        deadlineMs: Long,
    ): GenericResult? {
        val deadline = System.currentTimeMillis() + deadlineMs
        var notRunningStreak = 0
        while (System.currentTimeMillis() < deadline) {
            if (useResultFile) {
                val raw = sandbox.executeCommand("cat $SERVE_RESULT_PATH 2>/dev/null", SandboxSessions.SYSTEM).trim()
                if (raw.isNotBlank()) {
                    val fromFile = decodeOr(raw, GenericResult(ok = false, error = null))
                    // The script finished and wrote its verdict; trust it. (ok
                    // results carry the model name — require the match so a
                    // half-written or foreign line can't claim success.)
                    if (fromFile.ok && (fromFile.model == null || fromFile.model == modelFilename)) return fromFile
                    if (!fromFile.ok && fromFile.error != null) return fromFile
                }
            }
            val st = runCatching { status() }.getOrNull()
            if (st != null) {
                if (st.running && (st.model.isEmpty() || st.model == modelFilename)) {
                    notRunningStreak = 0
                    if (runCatching { health() }.getOrDefault(false)) {
                        return GenericResult(
                            ok = true,
                            model = st.model.ifEmpty { modelFilename },
                            port = st.port.toIntOrNull() ?: port,
                            baseUrl = st.baseUrl.ifEmpty { "http://127.0.0.1:$port/v1" },
                        )
                    }
                    // Alive but not healthy yet: still loading — keep waiting.
                } else {
                    // Not running: either the script hasn't launched it yet or the
                    // server died. A persistent streak with no result file means dead.
                    notRunningStreak++
                    if (notRunningStreak >= 3) {
                        return if (useResultFile) {
                            GenericResult(
                                ok = false,
                                error = "server_died",
                                detail = "The server process exited while loading. The log below usually says why (corrupt model file, out of memory).",
                                logPath = SERVER_LOG_PATH,
                            )
                        } else {
                            null
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(5_000L)
        }
        return null
    }

    suspend fun stop(): GenericResult {
        val r = decodeOr(runQuick("stop"), GenericResult(ok = false, error = "stop_unparseable"))
        setServingForeground(false)
        return r
    }

    suspend fun deleteModel(modelFilename: String): GenericResult =
        decodeOr(runQuick("delete ${shellQuote(modelFilename)}"), GenericResult(ok = false, error = "delete_unparseable"))

    /** Read the tail of a log file from inside the sandbox; capped so we don't
     * push huge text into a Compose dialog. Returns empty string if missing. */
    suspend fun readLogTail(path: String, maxBytes: Int = 8000): String = sandbox.executeCommand(
        command = "tail -c $maxBytes ${shellQuote(path)} 2>/dev/null",
        sessionId = SandboxSessions.SYSTEM,
    )

    private inline fun <reified T> decodeOr(raw: String, fallback: T): T {
        // The shell layer can append lifecycle noise to otherwise-valid JSON
        // ("Shell session ended", "Exit code: 1", a mid-string truncation
        // marker), and quick commands don't pre-filter to the JSON line the way
        // the streaming path does. Try progressively narrower slices before
        // giving up, so real results aren't misreported as *_unparseable.
        for (candidate in jsonCandidates(raw)) {
            runCatching { return json.decodeFromString<T>(candidate) }
        }
        return fallback
    }

    private fun jsonCandidates(raw: String): List<String> {
        val t = raw.trim()
        if (t.isEmpty()) return emptyList()
        val out = mutableListOf(t)
        t.lines().lastOrNull { it.trim().startsWith("{") }?.trim()?.let { if (it != t) out.add(it) }
        val first = t.indexOf('{')
        val last = t.lastIndexOf('}')
        if (first in 0 until last) {
            val slice = t.substring(first, last + 1)
            if (slice != t) out.add(slice)
        }
        return out
    }

    private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    init {
        // Pre-install the script whenever the sandbox reaches Ready so the user
        // can also invoke `morsllm` directly from the in-app Terminal without
        // going through this manager. Pure file write + chmod — building
        // llama-server is gated behind explicit provision(). Observed as a
        // stream, not a one-shot: a sandbox reset+reinstall wipes the rootfs
        // (and the installed script) without restarting the app process, so
        // the installed flag must drop when readiness drops or every engine op
        // after a reset fails until the app is killed.
        scope.launch {
            sandbox.status.collect { status ->
                if (status.ready) {
                    ensureScriptInstalled()
                    maybeAutoRecoverEngine()
                } else {
                    scriptInstalled = false
                }
            }
        }
    }

    @Volatile
    private var autoRecoverAttempted = false

    /**
     * A sandbox reset/reinstall wipes the engine binary (it lives on the rootfs at
     * /opt) while downloaded models survive under /root. When we come back Ready with
     * models present but no engine, quietly rebuild it — a ~14 MB download — so the
     * user doesn't have to notice "not built yet" and tap Set up engine again. One
     * attempt per app process; if it fails, the manual button is still there.
     */
    private suspend fun maybeAutoRecoverEngine() {
        if (autoRecoverAttempted) return
        if (_op.value !is EngineOp.Idle) return
        val st = runCatching { status() }.getOrNull() ?: return
        if (st.provisioned) return
        val hasModels = runCatching { listModels().models.isNotEmpty() }.getOrDefault(false)
        if (!hasModels) return
        autoRecoverAttempted = true
        startProvision()
    }

    companion object {
        const val DEFAULT_PORT = 8080
        private const val SCRIPT_PATH = "/usr/local/bin/morsllm"
        private const val SETUP_SCRIPT_PATH = "/usr/local/bin/morsllm-setup"
        private const val SCRIPT_INSTALL_FAILED_JSON = """{"ok":false,"error":"script_install_failed"}"""

        // Mirror of morsllm.sh's default layout (ROOT=/root/.posh/llm). Used only
        // for post-failure recovery reads and error-dialog log loading.
        private const val SERVE_RESULT_PATH = "/root/.posh/llm/run/last-serve.json"
        private const val SERVER_LOG_PATH = "/root/.posh/llm/logs/server.log"
    }
}
