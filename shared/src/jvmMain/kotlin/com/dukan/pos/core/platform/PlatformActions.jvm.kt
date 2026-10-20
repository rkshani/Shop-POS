package com.dukan.pos.core.platform

import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopPlatformActions : PlatformActions {

    override suspend fun saveTextFile(
        fileName: String,
        mimeType: String,
        content: String,
    ): Boolean {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save backup"
            selectedFile = File(fileName)
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return false
        return runCatching {
            chooser.selectedFile.writeText(content)
            true
        }.getOrDefault(false)
    }

    override suspend fun readTextFile(): String? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose backup file"
            fileFilter = FileNameExtensionFilter("Backup files", "json", "dukan")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
        return runCatching { chooser.selectedFile.readText() }.getOrNull()
    }

    override fun openUrl(url: String): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return false
        Desktop.getDesktop().browse(URI(url))
        true
    }.getOrDefault(false)
}
