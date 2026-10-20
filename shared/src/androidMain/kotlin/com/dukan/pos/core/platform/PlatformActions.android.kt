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
 */
object AndroidFileBridge {
    var createDocument: ((fileName: String, mimeType: String, onResult: (Uri?) -> Unit) -> Unit)? = null
    var openDocument: ((onResult: (Uri?) -> Unit) -> Unit)? = null

    fun clear() {
        createDocument = null
        openDocument = null
    }
}

class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override suspend fun saveTextFile(
        fileName: String,
        mimeType: String,
        content: String,
    ): Boolean {
        val launcher = AndroidFileBridge.createDocument ?: return false
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            launcher(fileName, mimeType) { continuation.resume(it) }
        } ?: return false

        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.encodeToByteArray())
            } ?: return false
            true
        }.getOrDefault(false)
    }

    override suspend fun readTextFile(): String? {
        val launcher = AndroidFileBridge.openDocument ?: return null
        val uri = suspendCancellableCoroutine<Uri?> { continuation ->
            launcher { continuation.resume(it) }
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
