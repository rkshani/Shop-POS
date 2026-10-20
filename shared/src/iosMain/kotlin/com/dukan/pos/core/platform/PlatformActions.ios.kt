package com.dukan.pos.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosPlatformActions : PlatformActions {

    private fun rootViewController(): UIViewController? =
        UIApplication.sharedApplication.keyWindow?.rootViewController

    override suspend fun saveTextFile(
        fileName: String,
        mimeType: String,
        content: String,
    ): Boolean {
        val root = rootViewController() ?: return false
        val path = NSTemporaryDirectory() + fileName
        val url = NSURL.fileURLWithPath(path)

        val written = (content as NSString).writeToURL(
            url = url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        if (!written) return false

        val controller = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
        root.presentViewController(controller, animated = true, completion = null)
        return true
    }

    override suspend fun readTextFile(): String? {
        val root = rootViewController() ?: return null

        return suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL,
                ) {
                    val text = NSString.stringWithContentsOfURL(
                        url = didPickDocumentAtURL,
                        encoding = NSUTF8StringEncoding,
                        error = null,
                    )
                    continuation.resume(text as String?)
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    continuation.resume(null)
                }
            }

            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.json", "public.text"),
                inMode = platform.UIKit.UIDocumentPickerMode.UIDocumentPickerModeImport,
            )
            picker.delegate = delegate
            // The delegate is weakly held by the picker, so pin it for the presentation.
            pickerDelegate = delegate
            root.presentViewController(picker, animated = true, completion = null)
        }
    }

    override fun openUrl(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        if (!UIApplication.sharedApplication.canOpenURL(nsUrl)) return false
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }

    private var pickerDelegate: NSObject? = null
}
