package com.inspiredandroid.kai.ui.sandbox

/**
 * A curated library of ready-to-run terminal commands aimed at a non-expert user.
 * Users can favorite the ones they use; favorites surface as `/` shortcuts in the
 * terminal input. Tapping a script fills it into the input so the user can review
 * (and edit placeholders like URLs) before running it.
 *
 * Commands are intentionally safe and read-mostly; anything that installs a package
 * does so inline so it "just works" on a fresh sandbox. Placeholders are written in
 * ANGLE_BRACKETS or obvious sample values the user is meant to replace.
 */
data class TerminalScript(
    val id: String,
    val title: String,
    val description: String,
    val command: String,
    val category: String,
)

object TerminalScriptLibrary {
    const val CAT_SYSTEM = "System"
    const val CAT_FILES = "Files"
    const val CAT_NETWORK = "Network"
    const val CAT_TEXT = "Text & Data"
    const val CAT_DEV = "Dev"
    const val CAT_FUN = "Fun"

    /** Display order for category headers. */
    val categoryOrder = listOf(CAT_SYSTEM, CAT_FILES, CAT_NETWORK, CAT_TEXT, CAT_DEV, CAT_FUN)

    val all: List<TerminalScript> = listOf(
        // System
        TerminalScript("disk-space", "Disk space", "How much storage is free", "df -h /root", CAT_SYSTEM),
        TerminalScript("memory", "Memory", "RAM total / free", "head -3 /proc/meminfo", CAT_SYSTEM),
        TerminalScript("system-info", "System info", "Kernel and architecture", "uname -a", CAT_SYSTEM),
        TerminalScript("processes", "Running processes", "What's running right now", "ps aux 2>/dev/null || ps", CAT_SYSTEM),
        TerminalScript("datetime", "Date & time", "Current date and time", "date", CAT_SYSTEM),
        // Files
        TerminalScript("list-files", "List files here", "Everything in the current folder", "ls -lah", CAT_FILES),
        TerminalScript("where-am-i", "Where am I", "Show the current folder", "pwd", CAT_FILES),
        TerminalScript("big-files", "Find big files", "Biggest files under /root", "du -ah /root 2>/dev/null | sort -rh | head -20", CAT_FILES),
        TerminalScript("make-folder", "Make a folder", "Create a folder (edit the name)", "mkdir -p myfolder && echo created myfolder", CAT_FILES),
        TerminalScript("tree", "Folder tree", "Visual folder layout", "command -v tree >/dev/null || apk add tree >/dev/null 2>&1; tree -L 2 /root", CAT_FILES),
        // Network
        TerminalScript("public-ip", "My public IP", "Your internet-facing IP address", "curl -s https://api.ipify.org; echo", CAT_NETWORK),
        TerminalScript("ping", "Ping test", "Check your connection", "ping -c 4 8.8.8.8", CAT_NETWORK),
        TerminalScript("download", "Download a file", "Save a file from a URL (edit the URL)", "curl -LO https://example.com/file.zip", CAT_NETWORK),
        TerminalScript("check-site", "Check a website", "See a site's response headers (edit the URL)", "curl -sSI https://example.com | head -20", CAT_NETWORK),
        TerminalScript("weather", "Weather", "Quick forecast (edit your city)", "curl -s 'https://wttr.in/London?format=3'; echo", CAT_NETWORK),
        // Text & Data
        TerminalScript("search-text", "Search text in files", "Find a word in files here (edit the word)", "grep -rn 'TODO' .", CAT_TEXT),
        TerminalScript("count-lines", "Count lines in a file", "Lines / words / chars (edit the file)", "wc file.txt", CAT_TEXT),
        TerminalScript("pretty-json", "Pretty-print JSON", "Format a JSON file (edit the file)", "command -v jq >/dev/null || apk add jq >/dev/null 2>&1; jq . data.json", CAT_TEXT),
        // Dev
        TerminalScript("python-version", "Python version", "Check Python is available", "python3 --version", CAT_DEV),
        TerminalScript("python-hello", "Run some Python", "Run a one-line Python snippet", "python3 -c \"print('hello from POSH')\"", CAT_DEV),
        TerminalScript("install-package", "Install a package", "Add an Alpine package (edit the name)", "apk add <package>", CAT_DEV),
        TerminalScript("update-packages", "Update package list", "Refresh available packages", "apk update", CAT_DEV),
        // Fun
        TerminalScript("cowsay", "Cow says", "A cow says something (edit the message)", "command -v cowsay >/dev/null || apk add cowsay >/dev/null 2>&1; cowsay 'Hello from POSH'", CAT_FUN),
        TerminalScript("figlet", "Big text banner", "Turn text into a big banner (edit the text)", "command -v figlet >/dev/null || apk add figlet >/dev/null 2>&1; figlet POSH", CAT_FUN),
    )

    fun byId(id: String): TerminalScript? = all.firstOrNull { it.id == id }

    /** The `/help` guide shown in the terminal. */
    val helpText: String = """
        POSH Terminal — quick guide

        This is a real Linux (Alpine) shell running on your phone. Type any command and
        press the send button (or Enter) to run it.

        Shortcuts
        • Type "/" to see your favorite scripts — tap one to drop it into the input,
          then edit and run it.
        • Type "/help" any time to see this guide.
        • Tap the list icon next to the input to open the full Script Library, where you
          can browse ready-made commands and star the ones you use most.

        Handy basics
        • ls            list files here
        • cd <folder>   move into a folder
        • pwd           show where you are
        • cat <file>    print a file
        • apk add <pkg> install a program
        • clear         clear the screen

        Notes
        • Your files live in /root and persist between sessions.
        • Long downloads: keep POSH open (Android stops background work).
        • Scripts with <angle brackets> or sample values (a URL, a filename) are meant to
          be edited before you run them.
    """.trimIndent()
}
