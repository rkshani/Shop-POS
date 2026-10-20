package com.dukan.pos.core.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Desktop counters use a USB scanner, which types the code like a keyboard.
 * So the desktop "scanner" is just a focused text field.
 */
actual fun isCameraScanningSupported(): Boolean = false

@Composable
actual fun BarcodeScannerView(
    onBarcode: (String) -> Unit,
    onClose: () -> Unit,
) {
    var value by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Barcode scan karein ya type karein")
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
        )
        Button(onClick = { if (value.isNotBlank()) onBarcode(value.trim()) }) { Text("Add karein") }
        TextButton(onClick = onClose) { Text("Cancel") }
    }
}
