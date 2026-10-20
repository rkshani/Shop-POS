package com.dukan.pos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.dukan.pos.DukanApp
import com.dukan.pos.core.platform.AndroidFileBridge

class MainActivity : ComponentActivity() {

    // Results go to the bridge, which outlives this Activity across rotation.
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
        AndroidFileBridge::deliverCreateResult,
    )

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
        AndroidFileBridge::deliverOpenResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Backup needs the Storage Access Framework, which needs an Activity.
        // The bridge keeps that dependency out of the shared module.
        AndroidFileBridge.createDocument = { fileName, _ -> createDocument.launch(fileName) }
        AndroidFileBridge.openDocument = {
            openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        setContent { DukanApp() }
    }

    override fun onDestroy() {
        AndroidFileBridge.detach(isChangingConfigurations)
        super.onDestroy()
    }
}
