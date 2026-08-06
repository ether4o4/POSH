package com.inspiredandroid.kai.plugins

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A bound connection to one plugin APK's extension service. Calls the plugin's
 * single AIDL method `Bundle executeTool(Bundle)` by hand-writing the AIDL wire
 * format, so POSH needs no generated stub and no `aidl` build feature (the KMP
 * `androidLibrary` target does not reliably run the AIDL compiler).
 *
 * The wire format matches what `aidl` generates for a one-method interface:
 *   data:  writeInterfaceToken(DESCRIPTOR); writeInt(1); bundle.writeToParcel(data)
 *   txn:   IBinder.FIRST_CALL_TRANSACTION
 *   reply: readException(); if readInt()!=0 -> Bundle.CREATOR.createFromParcel(reply)
 */
class PluginConnection(
    private val context: Context,
    private val packageName: String,
) {
    private var binder: IBinder? = null
    private var connecting: CompletableDeferred<IBinder?>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service
            connecting?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }

        override fun onBindingDied(name: ComponentName?) {
            binder = null
            connecting?.complete(null)
        }
    }

    private suspend fun ensureBound(timeoutMs: Long): IBinder? {
        binder?.let { return it }
        val existing = connecting
        if (existing != null) return withTimeoutOrNull(timeoutMs) { existing.await() }

        val deferred = CompletableDeferred<IBinder?>()
        connecting = deferred
        val intent = Intent(PluginProtocol.BIND_ACTION).apply { setPackage(packageName) }
        val ok = try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: SecurityException) {
            false
        }
        if (!ok) {
            connecting = null
            return null
        }
        val result = withTimeoutOrNull(timeoutMs) { deferred.await() }
        connecting = null
        return result
    }

    /**
     * Execute [toolName] with [argsJson] on the plugin. Returns the plugin's
     * reply Bundle, or null if the plugin could not be bound or did not reply
     * within [timeoutMs]. Callers read [PluginProtocol.KEY_TEXT] /
     * [PluginProtocol.KEY_STATUS] / error keys from the returned Bundle.
     */
    suspend fun executeTool(toolName: String, argsJson: String, timeoutMs: Long): Bundle? {
        val service = ensureBound(timeoutMs) ?: return null
        val request = Bundle().apply {
            putString(PluginProtocol.KEY_TOOL_NAME, toolName)
            putString(PluginProtocol.KEY_ARGS_JSON, argsJson)
        }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(PluginProtocol.AIDL_DESCRIPTOR)
            data.writeInt(1)
            request.writeToParcel(data, 0)
            service.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0)
            reply.readException()
            if (reply.readInt() != 0) {
                Bundle.CREATOR.createFromParcel(reply)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun unbind() {
        if (binder != null || connecting != null) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: IllegalArgumentException) {
                // Already unbound.
            }
        }
        binder = null
        connecting = null
    }
}
