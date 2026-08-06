package com.inspiredandroid.kai.sandbox

import com.inspiredandroid.kai.plugins.PluginManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sandboxModule = module {
    single<LinuxSandboxManager> { LinuxSandboxManager(androidContext(), get()) }
    // Orchestrates the on-device GGUF runtime (llama.cpp server in the sandbox). get()
    // resolves the app-wide SandboxController; the manager drives morsllm on the SYSTEM shell.
    single<GgufServerManager> { GgufServerManager(androidContext(), get()) }
    // Hosts FoneClaw-compatible plugin APKs: discovers them, reads their tool
    // manifests, and binds them on demand so their tools join POSH's agent.
    single<PluginManager> { PluginManager(androidContext(), get()) }
}
