package com.inspiredandroid.kai.sandbox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.inspiredandroid.kai.shared.R

/**
 * Foreground service held for the lifetime of a running local GGUF model.
 *
 * A served model keeps llama-server resident with gigabytes of the model in
 * memory. That process is a child of this app's process group, so under memory
 * pressure Android's low-memory killer can take down the whole app mid-inference
 * — experienced as a force close, and the reason a phone that runs a large model
 * fine in a persistent-notification app (e.g. Termux) can still lose a smaller
 * one here. Holding an ongoing foreground service lowers the app's oom_score_adj
 * so the OS treats it as user-visible work and spares it, exactly as that
 * persistent notification does. Started when serving begins; stopped when the
 * model stops.
 */
class GgufServerService : Service() {

    companion object {
        private const val CHANNEL_ID = "posh_local_model_channel"
        private const val NOTIFICATION_ID = 9004

        fun start(context: Context) {
            val intent = Intent(context, GgufServerService::class.java)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GgufServerService::class.java)
            runCatching { context.stopService(intent) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Local model",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while an on-device GGUF model is running."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Local model running — keeping it in memory. Stop it to free RAM.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
