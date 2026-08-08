package com.inspiredandroid.kai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.SandboxController
import com.inspiredandroid.kai.data.DataRepository
import com.inspiredandroid.kai.data.Service
import com.inspiredandroid.kai.sandbox.GgufServerManager
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.outlineTextFieldColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Android implementation: download and run GGUF models through
 * [GgufServerManager] (llama.cpp inside the Linux sandbox), then one tap to
 * register the loopback server as an OpenAI-Compatible service.
 */
@Composable
actual fun PlatformGgufModelsCard() {
    val manager = koinInject<GgufServerManager>()
    val dataRepository = koinInject<DataRepository>()
    val sandboxController = koinInject<SandboxController>()
    val sandboxStatus by sandboxController.status.collectAsState()
    val scope = rememberCoroutineScope()

    // The engine build / model download now lives in the app-scoped manager, so it
    // keeps running when you leave this screen. The card just observes its state.
    val op by manager.op.collectAsState()
    val busy = op is GgufServerManager.EngineOp.Running
    val busyLabel = (op as? GgufServerManager.EngineOp.Running)?.label.orEmpty()
    val busyProgress = (op as? GgufServerManager.EngineOp.Running)?.progress
    val errorResult = (op as? GgufServerManager.EngineOp.Failed)?.result

    var status by remember { mutableStateOf<GgufServerManager.Status?>(null) }
    var models by remember { mutableStateOf<List<GgufServerManager.ModelFile>>(emptyList()) }
    var repoInput by remember { mutableStateOf("") }
    var quantInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var modelToDelete by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        // Defensive: don't overwrite `status` or `models` with empty/failed
        // results — if the script-install race causes runQuick to return the
        // SCRIPT_INSTALL_FAILED_JSON sentinel, the resulting Status has
        // provisioned=false and ListModelsResult has empty models. Overwriting
        // a previously-correct state with that wipes the UI back to "not built
        // yet" + no downloaded models even though both are actually fine on
        // disk. Only commit a new status if the call returned a non-default
        // shape; only commit a new model list if the call reported ok.
        val newStatus = manager.status()
        if (newStatus.provisioned || newStatus.running || status == null) {
            status = newStatus
        }
        val newModels = manager.listModels()
        if (newModels.ok || (status?.provisioned != true)) {
            models = newModels.models
        }
    }

    LaunchedEffect(sandboxStatus.ready) {
        if (sandboxStatus.ready) refresh()
    }

    // Observe the manager op: clear stale toasts when a new op starts, and on
    // success surface the message, re-read status/models, then acknowledge so it
    // doesn't replay on the next visit. Failures stay until the dialog is dismissed.
    LaunchedEffect(op) {
        when (val current = op) {
            is GgufServerManager.EngineOp.Running -> message = null

            is GgufServerManager.EngineOp.Done -> {
                message = current.message
                refresh()
                manager.acknowledgeOp()
            }

            is GgufServerManager.EngineOp.Failed -> refresh()

            else -> {}
        }
    }

    SettingsCard {
        Text(
            text = "Local Models (GGUF)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Runs any GGUF model in the Linux sandbox. Separate from the service named \"Local Model\" (the built-in Gemma engine) — models here appear in the picker as \"OpenAI-Compatible API\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        if (!sandboxStatus.ready) {
            Text(
                text = "Set up the Alpine Linux sandbox first — the model engine runs inside it. Install it from Settings → Linux Sandbox.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SettingsCard
        }

        val st = status
        Text(
            text = "Engine: " + if (st?.provisioned == true) "ready" else "not built yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (st?.running == true) {
            Text(
                text = "Serving ${st.model} at ${st.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Auto-stops after 30 min without use to save battery and RAM — tap Run to start it again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (busy) {
            if (busyProgress != null) {
                // Determinate download progress: real bar + percentage.
                Text(
                    text = busyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { busyProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = busyLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (st == null || !st.provisioned) {
            Text(
                text = "Downloads a prebuilt engine (usually under a minute). Compiles from source only if no prebuilt is reachable — that path can take 10–30 min.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { manager.startProvision() },
                modifier = Modifier.handCursor(),
            ) { Text("Set up engine") }
        } else {
            // Quick-install buttons: curated GGUF models that are known to work
            // with the current llama.cpp build. Removes the "what do I type"
            // friction for new users — one tap and the right repo id is filled in.
            Text(
                text = "Quick install — small models known to work with this engine. The 1B runs on any phone; the 2-3B models want 2-3 GB free RAM:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(
                    onClick = { repoInput = "mradermacher/Llama-3.2-1B-Instruct-abliterated-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Quick 1B\n(uncensored • ~0.8GB)", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = { repoInput = "bartowski/Hermes-3-Llama-3.2-3B-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Tool-caller 3B\n(Hermes • ~2GB)", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = { repoInput = "mradermacher/Qwen2.5-3B-Instruct-abliterated-GGUF" },
                    modifier = Modifier.weight(1f).handCursor(),
                ) { Text("Uncensored 3B\n(Qwen • ~2GB)", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    repoInput = "mradermacher/Qwen3.5-2B_Abliterated-GGUF"
                    // IQ4_XS: the 1.17 GB imatrix-free quant of this repo — smaller
                    // than Q4_K_M with near-identical quality on a 2B.
                    quantInput = "IQ4_XS"
                },
                modifier = Modifier.fillMaxWidth().handCursor(),
            ) { Text("Qwen3.5 2B (uncensored • ~1.2GB)", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = repoInput,
                onValueChange = { repoInput = it },
                label = { Text("HuggingFace repo, repo URL, or .gguf URL") },
                placeholder = { Text("bartowski/Qwen2.5-0.5B-Instruct-GGUF") },
                singleLine = true,
                colors = outlineTextFieldColors(),
                supportingText = {
                    Text(
                        text = "Must be a GGUF repo (e.g. bartowski/…-GGUF or mradermacher/…-GGUF). Vanilla model repos like 'gpt2' don't contain .gguf files and won't work.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quantInput,
                onValueChange = { quantInput = it },
                label = { Text("Quant (optional — default picks Q4_K_M)") },
                placeholder = { Text("Q4_K_M") },
                singleLine = true,
                colors = outlineTextFieldColors(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = repoInput.isNotBlank(),
                onClick = {
                    // Normalize: strip a full HuggingFace URL down to a repo id
                    // so users can paste either "owner/repo", "https://huggingface.co/owner/repo",
                    // or the file URL and it just works.
                    val raw = repoInput.trim()
                    val normalized = when {
                        raw.startsWith("https://huggingface.co/") || raw.startsWith("http://huggingface.co/") -> {
                            val path = raw.substringAfter("huggingface.co/").trimEnd('/')
                            // Direct .gguf download URL: keep as-is, the script handles it.
                            if (path.contains("/resolve/") && path.endsWith(".gguf", ignoreCase = true)) {
                                raw
                            } // Otherwise reduce to owner/repo (drop any /tree/main/... or /blob/... suffix)
                            else {
                                path.split("/").take(2).joinToString("/")
                            }
                        }

                        else -> raw
                    }
                    manager.startPull(normalized, quantInput.trim().ifBlank { null })
                },
                modifier = Modifier.handCursor(),
            ) { Text("Download") }

            if (models.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloaded models",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                models.forEach { m ->
                    val isRunning = st.running && st.model == m.name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = humanSize(m.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (isRunning) {
                            OutlinedButton(
                                onClick = { manager.startStop() },
                                modifier = Modifier.handCursor(),
                            ) { Text("Stop") }
                        } else {
                            Button(
                                onClick = { manager.startServe(m.name) },
                                modifier = Modifier.handCursor(),
                            ) { Text("Run") }
                            IconButton(
                                onClick = { modelToDelete = m.name },
                                modifier = Modifier.handCursor(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${m.name}",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            if (st.running) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val baseUrl = manager.openAiBaseUrl
                        val existing = dataRepository.getConfiguredServiceInstances().firstOrNull {
                            it.serviceId == Service.OpenAICompatible.id &&
                                dataRepository.getInstanceBaseUrl(it.instanceId, Service.OpenAICompatible) == baseUrl
                        }
                        val instance = existing ?: dataRepository.addConfiguredService(Service.OpenAICompatible.id)
                            .also { dataRepository.updateInstanceBaseUrl(it.instanceId, baseUrl) }
                        message = "Added — it shows in the service picker as \"OpenAI-Compatible API\" (your local GGUF server). Open Services to pick the model."
                        scope.launch {
                            runCatching { dataRepository.validateConnection(Service.OpenAICompatible, instance.instanceId) }
                        }
                    },
                    modifier = Modifier.handCursor(),
                ) { Text("Add as service") }
            }
        }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val err = errorResult
    if (err != null) {
        ProvisionErrorDialog(
            result = err,
            manager = manager,
            onDismiss = { manager.acknowledgeOp() },
        )
    }

    val pendingDelete = modelToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete model?") },
            text = { Text("Permanently delete \"$pendingDelete\" from the device? You can re-download it later.") },
            confirmButton = {
                TextButton(onClick = {
                    manager.startDelete(pendingDelete)
                    modelToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProvisionErrorDialog(
    result: GgufServerManager.GenericResult,
    manager: GgufServerManager,
    onDismiss: () -> Unit,
) {
    var logTail by remember(result) { mutableStateOf<String?>(null) }
    var loadingLog by remember(result) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(result) {
        val path = result.logPath
        if (!path.isNullOrBlank()) {
            loadingLog = true
            logTail = runCatching { manager.readLogTail(path) }.getOrNull()
            loadingLog = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error: ${result.error ?: "unknown"}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                result.detail?.takeIf { it.isNotBlank() }?.let {
                    Text("Detail", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                }
                result.hint?.takeIf { it.isNotBlank() }?.let {
                    Text("Suggested fix", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                when {
                    loadingLog -> {
                        Text("Log", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("loading…", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    !logTail.isNullOrBlank() -> {
                        Text("Log (last 8KB)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = logTail.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.heightIn(max = 240.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = buildString {
                    appendLine("Error: ${result.error ?: "unknown"}")
                    result.detail?.takeIf { it.isNotBlank() }?.let { appendLine("Detail: $it") }
                    result.hint?.takeIf { it.isNotBlank() }?.let { appendLine("Hint: $it") }
                    logTail?.takeIf { it.isNotBlank() }?.let { appendLine("Log:\n$it") }
                }
                clipboard.setText(AnnotatedString(text))
            }) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
    )
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        val gb = mb / 1024.0
        "${(gb * 10).toLong() / 10.0} GB"
    } else {
        "${mb.toLong()} MB"
    }
}
