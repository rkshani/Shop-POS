package com.dukan.pos.core.scanner

import androidx.compose.runtime.Composable

/**
 * Camera scanning is the one genuinely platform-specific piece of this app.
 * Everything above it — lookup, cart, pricing — stays in common code.
 */
@Composable
expect fun BarcodeScannerView(
    onBarcode: (String) -> Unit,
    onClose: () -> Unit,
)

/** Desktop has no camera path, so the UI offers keyboard entry instead. */
expect fun isCameraScanningSupported(): Boolean
