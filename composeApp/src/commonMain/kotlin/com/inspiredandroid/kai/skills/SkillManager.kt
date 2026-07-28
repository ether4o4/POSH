package com.inspiredandroid.kai.skills

import com.inspiredandroid.kai.SandboxController
import com.inspiredandroid.kai.SandboxSessions
import com.inspiredandroid.kai.getBackgroundDispatcher
import kai.composeapp.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * Manages the user's skills. Most skills live in the Linux sandbox at `~/skills/<id>/`
 * (each is a folder containing `SKILL.md` plus any bundled files); a small set of
 * "built-in" skills ships inside the app as compose resources and is merged into the
 * same in-memory cache so synchronous callers ([getInstalled], [getSkill]) stay cheap.
 * On id collision the sandbox copy wins, so users can override a built-in.
 *
 * The cache is (re)loaded after every install/uninstall and whenever the sandbox
 * becomes installed — built-ins are loaded then too, gated on sandbox availability
 * because they only make sense when their `execute_shell_command` writes can land.
 * On platforms without a sandbox the file ops are no-ops and `load()` never runs, so
 * no skills (built-in or otherwise) appear off-Android.
 */
class SkillManager(
    private val sandboxController: SandboxController,
    private val registry: SkillRegistry = SkillRegistry(),
    backgroundDispatcher: CoroutineContext = getBackgroundDispatcher(),
) {

    private val scope = CoroutineScope(SupervisorJob() + backgroundDispatcher)
    private val mutex = Mutex()

    private val _skills = MutableStateFlow<List<SkillManifest>>(emptyList())
    val skills: StateFlow<List<SkillManifest>> = _skills

    init {
        // Load once the sandbox is installed (the file ops resolve real paths only
        // then); the StateFlow re-emits when it flips, so reset/install refresh too.
        scope.launch {
            var wasInstalled = false
            sandboxController.status.collect { status ->
                if (status.installed && !wasInstalled) load()
                wasInstalled = status.installed
            }
        }
        // Hot-reload: watch the skills folder and reload live when a SKILL.md is added,
        // edited, or removed (e.g. the user edits one in the Terminal) — no restart needed.
        scope.launch { watchForChanges() }
    }

    fun getInstalled(): List<SkillManifest> = _skills.value

    fun getSkill(id: String): SkillManifest? = _skills.value.firstOrNull { it.id == id }

    suspend fun uninstall(id: String) {
        sandboxController.deleteEntry("$SKILLS_DIR/$id", recursive = true)
        load()
    }

    suspend fun installFromGitHub(owner: String, repo: String, ref: String, path: String): Result<SkillManifest> = registry.fetchSkillFiles(SkillSource.GitHub(owner, repo, ref, path)).mapCatching { install(it) }

    /** Installs a skill the user picked from the browse list, using its repo coordinates. */
    suspend fun installFromRegistryEntry(entry: RegistrySkillEntry): Result<SkillManifest> = installFromGitHub(entry.owner, entry.repo, entry.ref, entry.skillPath)

    /** Browses the curated marketplaces and returns the combined, searchable list. */
    suspend fun browseMarketplaces(): Result<List<RegistrySkillEntry>> = registry.browseMarketplaces(curatedSkillMarketplaces)

    /** Writes a downloaded skill into `~/skills/<id>/`, replacing any existing copy, then reloads. */
    internal suspend fun install(downloaded: DownloadedSkill): SkillManifest {
        val base = "$SKILLS_DIR/${downloaded.id}"
        sandboxController.deleteEntry(base, recursive = true) // replace if present
        sandboxController.writeTextFile("$base/SKILL.md", downloaded.rawSkillMd)
        for ((relPath, content) in downloaded.files) {
            val safe = relPath.split('/', '\\').filterNot { it.isEmpty() || it == ".." }
            if (safe.isEmpty()) continue
            sandboxController.writeTextFile("$base/${safe.joinToString("/")}", content)
        }
        load()
        return getSkill(downloaded.id) ?: error("Skill '${downloaded.id}' not found after install")
    }

    /** Reads every `~/skills/<id>/` folder back into the in-memory cache. */
    suspend fun load() {
        val skills = mutex.withLock {
            val sandboxSkills = sandboxController.listDirectory(SKILLS_DIR)
                .filter { it.isDirectory }
                .mapNotNull { dir ->
                    val base = "$SKILLS_DIR/${dir.name}"
                    val md = sandboxController.readTextFile("$base/SKILL.md") ?: return@mapNotNull null
                    val parsed = SkillFrontmatterParser.parse(md) as? SkillFrontmatterParser.Result.Ok
                        ?: return@mapNotNull null
                    val files = sandboxController.listDirectory(base)
                        .filter { !it.isDirectory && it.name != "SKILL.md" }
                        .map { it.name }
                        .sorted()
                    SkillManifest(
                        id = parsed.id,
                        displayName = SkillFrontmatterParser.displayName(parsed.id),
                        description = parsed.description,
                        body = parsed.body,
                        bundledFilePaths = files,
                        dependencies = parsed.dependencies,
                    )
                }
            // Sandbox-installed skills win on id collision so power users can override a built-in.
            val sandboxIds = sandboxSkills.mapTo(mutableSetOf()) { it.id }
            val builtIns = loadBuiltInSkills().filter { it.id !in sandboxIds }
            (builtIns + sandboxSkills).sortedBy { it.id }
        }
        _skills.value = skills
    }

    /**
     * Reads bundled SKILL.md files shipped in compose resources. They appear alongside
     * sandbox-installed skills, can be invoked as `/<id>` from chat, and cannot be
     * uninstalled. Updates flow with each app release — nothing is persisted to the
     * sandbox. A built-in whose resource read or frontmatter parse fails is silently
     * dropped (no user-facing failure for a missing/broken bundled asset).
     */
    private suspend fun loadBuiltInSkills(): List<SkillManifest> = BUILT_IN_SKILL_IDS.mapNotNull { id ->
        val bytes = runCatching { Res.readBytes("files/skills/$id/SKILL.md") }.getOrNull()
            ?: return@mapNotNull null
        val parsed = SkillFrontmatterParser.parse(bytes.decodeToString()) as? SkillFrontmatterParser.Result.Ok
            ?: return@mapNotNull null
        SkillManifest(
            id = parsed.id,
            displayName = SkillFrontmatterParser.displayName(parsed.id),
            description = parsed.description,
            body = parsed.body,
            isBuiltIn = true,
            dependencies = parsed.dependencies,
        )
    }

    // --- Dependency auto-install ---

    private val depsMutex = Mutex()
    private val depsInstalled = mutableSetOf<String>()

    /**
     * Installs the packages a skill declares (once per session) so they're present in
     * the sandbox before the skill is used. Bare tokens are Alpine `apk` packages; a
     * `pip:` prefix marks a Python library. Runs on the SYSTEM shell so it doesn't
     * disturb the chat's own session. No-op for skills without dependencies. Package
     * names are validated to a safe charset so a skill can't inject shell commands.
     */
    suspend fun ensureDependencies(id: String) {
        val skill = getSkill(id) ?: return
        if (skill.dependencies.isEmpty()) return
        val shouldInstall = depsMutex.withLock {
            if (id in depsInstalled) false else { depsInstalled.add(id); true }
        }
        if (!shouldInstall) return

        val safe = Regex("^[a-zA-Z0-9][a-zA-Z0-9._+-]*$")
        val apk = skill.dependencies.filterNot { it.startsWith("pip:") }.filter { safe.matches(it) }
        val pip = skill.dependencies.filter { it.startsWith("pip:") }
            .map { it.removePrefix("pip:").trim() }.filter { safe.matches(it) }
        val cmds = buildList {
            if (apk.isNotEmpty()) add("apk add --no-cache ${apk.joinToString(" ")}")
            if (pip.isNotEmpty()) add("pip install ${pip.joinToString(" ")}")
        }
        if (cmds.isEmpty()) return
        runCatching {
            sandboxController.executeCommand(cmds.joinToString(" && "), SandboxSessions.SYSTEM)
        }.onFailure {
            depsMutex.withLock { depsInstalled.remove(id) } // let it retry on the next turn
        }
    }

    // --- Hot-reload watcher ---

    /** Polls the skills folder's signature and reloads the cache when it changes. */
    private suspend fun watchForChanges() {
        var lastSig: String? = null
        while (true) {
            delay(WATCH_INTERVAL_MS)
            if (!sandboxController.status.value.installed) continue
            val sig = runCatching { skillsSignature() }.getOrNull() ?: continue
            if (lastSig != null && sig != lastSig) load()
            lastSig = sig
        }
    }

    /** A cheap fingerprint of the skills folder: each skill's SKILL.md mtime + size. */
    private suspend fun skillsSignature(): String {
        // `map` is inline, so the suspend listDirectory call inside it is allowed; the
        // final joinToString then runs over plain strings (its lambda is NOT inline).
        val parts = sandboxController.listDirectory(SKILLS_DIR)
            .filter { it.isDirectory }
            .sortedBy { it.name }
            .map { dir ->
                val md = sandboxController.listDirectory("$SKILLS_DIR/${dir.name}")
                    .firstOrNull { it.name == "SKILL.md" }
                "${dir.name}:${md?.lastModifiedMs ?: 0L}:${md?.sizeBytes ?: 0L}"
            }
        return parts.joinToString("|")
    }

    companion object {
        /** Absolute sandbox path of the skills folder (`~/skills`, home = `/root`). */
        const val SKILLS_DIR = "/root/skills"

        /** How often the hot-reload watcher polls the skills folder for edits. */
        private const val WATCH_INTERVAL_MS = 4000L

        /**
         * Ids of skills bundled in compose resources at
         * `composeResources/files/skills/<id>/SKILL.md`. Hardcoded so the asset path is
         * explicit at compile time and we don't need a resource directory listing.
         */
        private val BUILT_IN_SKILL_IDS = listOf("create-skill")
    }
}

/**
 * Parses several common forms users might paste to add a GitHub skill:
 * - `owner/repo`
 * - `owner/repo/path/to/skill`
 * - `https://github.com/owner/repo`
 * - `https://github.com/owner/repo/tree/<ref>/path/to/skill`
 *
 * Returns null on a shape we don't recognize so the dialog can surface a hint.
 */
fun parseGitHubSkillUrl(input: String): SkillSource.GitHub? {
    val trimmed = input.trim().removePrefix("https://").removePrefix("http://").removePrefix("github.com/")
    if (trimmed.isEmpty()) return null
    val parts = trimmed.trim('/').split('/').filter { it.isNotEmpty() }
    if (parts.size < 2) return null
    val owner = parts[0]
    val repo = parts[1]
    if (parts.size == 2) {
        return SkillSource.GitHub(owner = owner, repo = repo, ref = "main", path = "")
    }
    // owner/repo/tree/<ref>/<path…> or owner/repo/<path…> (assume main)
    return if (parts[2] == "tree" && parts.size >= 5) {
        val ref = parts[3]
        val path = parts.drop(4).joinToString("/")
        SkillSource.GitHub(owner = owner, repo = repo, ref = ref, path = path)
    } else {
        val path = parts.drop(2).joinToString("/")
        SkillSource.GitHub(owner = owner, repo = repo, ref = "main", path = path)
    }
}
