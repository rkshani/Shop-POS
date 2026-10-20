package com.dukan.pos.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Storage Access Framework needs an Activity to launch from, so MainActivity
 * registers its launchers here. Keeping the bridge to two lambdas means the
 * Activity holds no app logic and the logic holds no Activity reference.
 *
 * The waiting callback lives here, not in the Activity: a rotation while the
 * picker is open recreates the Activity, and the result is delivered to the
 * new one. If the callback lived in the old Activity the result was dropped
 * and the backup screen stayed "exporting" until the app was restarted.
 */
object AndroidFileBridge {
    var createDocument: ((fileName: String, mimeType: String) -> Unit)? = null
    var openDocument: (() -> Unit)? = null

    private var pendingCreate: ((Uri?) -> Unit)? = null
    private var pendingOpen: ((Uri?) -> Unit)? = null

    /** Returns false when no Activity is attached to launch the picker. */
    fun requestCreate(fileName: String, mimeType: String, onResult: (Uri?) -> Unit): Boolean {
        val launch = createDocument ?: return false
        deliverCreateResult(null) // a superseded request must not hang
        pendingCreate = onResult
        launch(fileName, mimeType)
        return true
    }

    fun requestOpen(onResult: (Uri?) -> Unit): Boolean {
        val launch = openDocument ?: return false
        deliverOpenResult(null)
        pendingOpen = onResult
        launch()
        return true
    }

    fun deliverCreateResult(uri: Uri?) {
        val callback = pendingCreate ?: return
        pendingCreate = null
        callback(uri)
    }

    fun deliverOpenResult(uri: Uri?) {
        val callback = pendingOpen ?: return
        pendingOpen = null
        callback(uri)
    }

    /**
     * Called when the Activity is destroyed. A configuration change keeps the
     * waiting callbacks for the recreated Activity; a real finish cancels them
     * so nothing waits forever.
     */
    fun detach(isChangingConfigurations: Boolean) {
        createDocument = null
        openDocument = null
        if (!isChangingConfigurations) {
            deliverCreateResult(null)
            deliverOpenResult(null)
        }
    }
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override suspend fun saveTextFile(
        fileName: String,
        mimeType: String,
        content: String,
    ): Boolean {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            val launched = AndroidFileBridge.requestCreate(fileName, mimeType) { uri ->
                if (continuation.isActive) continuation.resume(uri)
            }
            if (!launched) continuation.resume(null)
        } ?: return false

        return runCatching {
            // "wt" truncates: plain "w" leaves the tail of a longer old file
            // behind on some providers, which corrupts an overwritten backup.
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(content.encodeToByteArray())
            } ?: return false
            true
        }.getOrDefault(false)
    }

    override suspend fun readTextFile(): String? {
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            val launched = AndroidFileBridge.requestOpen { uri ->
                if (continuation.isActive) continuation.resume(uri)
            }
            if (!launched) continuation.resume(null)
        } ?: return null

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().decodeToString()
            }
        }.getOrNull()
    }

    override fun openUrl(url: String): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
