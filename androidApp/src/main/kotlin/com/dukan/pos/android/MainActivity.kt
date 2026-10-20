package com.dukan.pos.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.dukan.pos.DukanApp
import com.dukan.pos.core.platform.AndroidFileBridge

class MainActivity : ComponentActivity() {

    private var pendingCreate: ((Uri?) -> Unit)? = null
    private var pendingOpen: ((Uri?) -> Unit)? = null

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        pendingCreate?.invoke(uri)
        pendingCreate = null
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingOpen?.invoke(uri)
        pendingOpen = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Backup needs the Storage Access Framework, which needs an Activity.
        // The bridge keeps that dependency out of the shared module.
        AndroidFileBridge.createDocument = { fileName, _, onResult ->
            pendingCreate = onResult
            createDocument.launch(fileName)
        }
        AndroidFileBridge.openDocument = { onResult ->
            pendingOpen = onResult
            openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        setContent { DukanApp() }
    }

    override fun onDestroy() {
        AndroidFileBridge.clear()
        super.onDestroy()
    }
}
