package com.inspiredandroid.kai.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inspiredandroid.kai.BackIcon
import com.inspiredandroid.kai.SandboxController
import com.inspiredandroid.kai.SandboxSessions
import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.data.MemoryEntry
import com.inspiredandroid.kai.ui.KaiOutlinedTextField
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.settings.AgentContent
import com.inspiredandroid.kai.ui.settings.PendingDeletion
import com.inspiredandroid.kai.ui.settings.PlatformGgufModelsCard
import com.inspiredandroid.kai.ui.settings.PlatformPluginsCard
import com.inspiredandroid.kai.ui.settings.SandboxViewModel
import com.inspiredandroid.kai.ui.settings.SettingsCard
import com.inspiredandroid.kai.ui.settings.SettingsViewModel
import com.inspiredandroid.kai.ui.settings.SkillsSection
import com.inspiredandroid.kai.ui.settings.TerminalContent
import com.inspiredandroid.kai.ui.settings.TerminalDarkBg
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-screen destinations for the hub tiles. Each is a focused page for one
 * surface (terminal, skills, config, memory, models, projects) instead of a
 * detour through the settings tabs.
 */
@Composable
private fun HubPageScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            // In landscape the camera cutout sits along a side edge, so without this
            // the page content runs underneath it.
            .displayCutoutPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.handCursor()) {
                Icon(BackIcon, contentDescription = null, tint = cs.onBackground)
            }
            Column {
                Text(
                    title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = cs.onBackground,
                    letterSpacing = 2.sp,
                )
                Text(
                    subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cap the reading width so forms and lists don't span a landscape screen.
            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val sandboxController = koinInject<SandboxController>()
    val status by sandboxController.status.collectAsState()
    HubPageScaffold("TERMINAL", "the live shell", onBack, scrollable = false) {
        if (!status.ready) {
            Spacer(Modifier.height(16.dp))
            Text(
                "The Linux sandbox isn't running. Set it up under Settings > Sandbox first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = TerminalDarkBg,
            ) {
                TerminalContent(
                    sandboxController = sandboxController,
                    modifier = Modifier.fillMaxSize(),
                    darkBackground = true,
                )
            }
        }
    }
}

@Composable
fun SkillsScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    sandboxViewModel: SandboxViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sandboxState by sandboxViewModel.state.collectAsStateWithLifecycle()
    val sandboxController = koinInject<SandboxController>()
    val scope = rememberCoroutineScope()

    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newInstructions by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    HubPageScaffold("SKILLS", "create · save · activate", onBack) {
        SettingsCard {
            Text(
                "Create a skill",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "A skill is a reusable instruction bundle the AI can invoke by name. It is saved into the sandbox and hot-reloads immediately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            KaiOutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            KaiOutlinedTextField(
                value = newDescription,
                onValueChange = { newDescription = it },
                label = { Text("Description (when should the AI use it?)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            KaiOutlinedTextField(
                value = newInstructions,
                onValueChange = { newInstructions = it },
                label = { Text("Instructions") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !saving && newName.isNotBlank() && newInstructions.isNotBlank() && sandboxState.sandboxInstalled,
                onClick = {
                    val id = newName.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                    if (id.isEmpty()) {
                        saveMessage = "Name must contain letters or digits."
                        return@Button
                    }
                    saving = true
                    saveMessage = null
                    scope.launch {
                        sandboxController.executeCommand(
                            command = "mkdir -p /root/skills/$id",
                            sessionId = SandboxSessions.SYSTEM,
                        )
                        val md = buildString {
                            appendLine("---")
                            appendLine("name: ${newName.trim()}")
                            appendLine("description: ${newDescription.trim().ifEmpty { newName.trim() }}")
                            appendLine("---")
                            appendLine()
                            append(newInstructions.trim())
                            appendLine()
                        }
                        val ok = sandboxController.writeTextFile("/root/skills/$id/SKILL.md", md)
                        saving = false
                        if (ok) {
                            saveMessage = "Saved and active as /$id."
                            newName = ""
                            newDescription = ""
                            newInstructions = ""
                            viewModel.refreshInstalledSkills()
                        } else {
                            saveMessage = "Save failed — is the sandbox running?"
                        }
                    }
                },
                modifier = Modifier.handCursor(),
            ) { Text(if (saving) "Saving…" else "Save skill") }
            if (!sandboxState.sandboxInstalled) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Skills live in the Linux sandbox — set it up under Settings > Sandbox first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            saveMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard {
            SkillsSection(
                skills = uiState.skills,
                onUninstallSkill = viewModel.actions.onUninstallSkill,
                onToggleSkill = viewModel.actions.onToggleSkill,
                showAddDialog = uiState.showAddSkillDialog,
                onShowAddDialog = viewModel.actions.onShowAddSkillDialog,
                onInstallGitHub = viewModel.actions.onInstallGitHubSkill,
                onInstallBrowsed = viewModel.actions.onInstallBrowsedSkill,
                isInstalling = uiState.isInstallingSkill,
                installError = uiState.skillInstallError,
                browsableSkills = uiState.browsableSkills,
                isBrowsing = uiState.isBrowsingSkills,
                browseFailed = uiState.browseSkillsFailed,
                isSandboxInstalled = sandboxState.sandboxInstalled,
                onNavigateToSandbox = onOpenSettings,
            )
        }
    }
}

@Composable
fun ConfigScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    HubPageScaffold("CONFIG", "persona · behavior", onBack) {
        AgentContent(uiState = uiState, actions = viewModel.actions)
    }
}

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val pending = uiState.pendingDeletion
    val memories = remember(uiState.memories, pending) {
        if (pending is PendingDeletion.Memory) uiState.memories.filter { it.key != pending.key } else uiState.memories
    }
    HubPageScaffold("MEMORY", "what it knows", onBack) {
        if (memories.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "No memories yet. The AI stores things it learns about you here as you chat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        memories.forEach { memory ->
            MemoryRow(memory = memory, onDelete = { viewModel.actions.onDeleteMemory(memory.key) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MemoryRow(memory: MemoryEntry, onDelete: () -> Unit) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    memory.key,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.handCursor()) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ModelsScreen(onBack: () -> Unit) {
    HubPageScaffold("MODELS", "search · download · run", onBack) {
        // Android-only GGUF runtime card: search a HuggingFace repo, download a
        // quant, list downloaded models, serve one locally. No-op elsewhere.
        PlatformGgufModelsCard()
    }
}

@Composable
fun PluginsScreen(onBack: () -> Unit) {
    HubPageScaffold("PLUGINS", "extend the agent", onBack) {
        // Android-only: discover FoneClaw-compatible plugin APKs and toggle their
        // tools into POSH's agent. No-op elsewhere.
        PlatformPluginsCard()
    }
}

@Composable
fun ProjectsScreen(onBack: () -> Unit) {
    val appSettings = koinInject<AppSettings>()
    val sandboxController = koinInject<SandboxController>()
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf(appSettings.getProjectNames()) }
    var newProject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    fun dirFor(name: String) = "/root/projects/" + name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    HubPageScaffold("PROJECTS", "start · resume", onBack) {
        SettingsCard {
            Text(
                "Start a project",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Each project gets its own folder in the sandbox (under /root/projects) and stays in this list until you delete it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            KaiOutlinedTextField(
                value = newProject,
                onValueChange = { newProject = it },
                label = { Text("Project name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = newProject.isNotBlank(),
                onClick = {
                    val name = newProject.trim()
                    if (projects.any { it.equals(name, ignoreCase = true) }) {
                        message = "A project with that name already exists."
                        return@Button
                    }
                    projects = listOf(name) + projects
                    appSettings.setProjectNames(projects)
                    newProject = ""
                    message = "Created ${dirFor(name)}"
                    scope.launch {
                        sandboxController.executeCommand(
                            command = "mkdir -p ${dirFor(name)}",
                            sessionId = SandboxSessions.SYSTEM,
                        )
                    }
                },
                modifier = Modifier.handCursor(),
            ) { Text("Create") }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Text(
                "No projects yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        projects.forEach { name ->
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            dirFor(name),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { deleteTarget = name }, modifier = Modifier.handCursor()) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"$target\"?") },
            text = { Text("This removes the project from the list and deletes its folder (${dirFor(target)}) in the sandbox.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        projects = projects.filterNot { it == target }
                        appSettings.setProjectNames(projects)
                        deleteTarget = null
                        scope.launch {
                            sandboxController.executeCommand(
                                command = "rm -rf ${dirFor(target)}",
                                sessionId = SandboxSessions.SYSTEM,
                            )
                        }
                    },
                    modifier = Modifier.handCursor(),
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, modifier = Modifier.handCursor()) { Text("Cancel") }
            },
        )
    }
}
