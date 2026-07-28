package com.inspiredandroid.kai

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.inspiredandroid.kai.data.TaskScheduler
import com.inspiredandroid.kai.sandbox.GgufServerManager
import com.inspiredandroid.kai.sandbox.sandboxModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KaiApplication : Application() {

    private val taskScheduler: TaskScheduler by inject()

    // Constructed eagerly in onCreate so its init-block watcher arms and the GGUF runtime
    // script auto-installs once the sandbox is ready, without waiting for the Settings card.
    private val ggufServerManager: GgufServerManager by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KaiApplication)
            modules(appModule, sandboxModule)
        }
        // Force construction of the GGUF manager (see field doc above).
        ggufServerManager
        // Track app foreground state so the scheduler only pushes a heartbeat notification
        // when the in-app banner isn't visible. ViewModel lifecycle is the wrong signal —
        // it survives backgrounding and only clears on Activity destruction.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                taskScheduler.appInForeground = true
            }
            override fun onStop(owner: LifecycleOwner) {
                taskScheduler.appInForeground = false
            }
        })
    }
}
