package com.dukan.pos.presentation.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.designsystem.EmptyState
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.Pill
import com.dukan.pos.core.designsystem.QtyStepper
import com.dukan.pos.core.scanner.BarcodeScannerView
import com.dukan.pos.core.scanner.isCameraScanningSupported
import com.dukan.pos.domain.model.CartLine
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.domain.model.Product
import com.dukan.pos.presentation.common.QuantityDialog
import com.dukan.pos.presentation.common.localized
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onAddProduct: () -> Unit,
    viewModel: PosViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PosEffect.Failed -> snackbarHostState.showSnackbar(effect.error.localized(strings))
                is PosEffect.ProductAdded ->
                    snackbarHostState.showSnackbar(strings.productAdded.fmt(effect.productName))
                is PosEffect.SaleCompleted ->
                    snackbarHostState.showSnackbar("${strings.saleSaved} · ${effect.invoiceNo}")
                is PosEffect.UnknownBarcode -> {
                    val action = snackbarHostState.showSnackbar(
                        message = strings.barcodeNotLinked,
                        actionLabel = strings.add,
                    )
                    if (action == androidx.compose.material3.SnackbarResult.ActionPerformed) onAddProduct()
                }
            }
        }
    }

    if (state.isScannerOpen) {
        BarcodeScannerView(
            onBarcode = { barcode ->
                viewModel.onBarcodeScanned(barcode)
                viewModel.closeScanner()
            },
            onClose = viewModel::closeScanner,
        )
        return
    }

    // Loose items open typed entry straight away: nobody buys "1 kg" of everything.
    var qtyEntryFor by remember { mutableStateOf<Product?>(null) }
    qtyEntryFor?.let { product ->
        val line = state.cart.findLine(product.id)
        QuantityDialog(
            title = product.name,
            unit = product.unit,
            unitPrice = line?.unitPrice ?: product.salePrice,
            initialQty = line?.qty ?: 0.0,
            allowAmount = true,
            onConfirm = { qty ->
                viewModel.onQtyEntered(product, qty)
                qtyEntryFor = null
            },
            onDismiss = { qtyEntryFor = null },
        )
    }

    if (state.isCheckoutOpen) {
        CheckoutSheet(
            cart = state.cart,
            customers = customers,
            isSubmitting = state.isSubmitting,
            onCustomerSelected = viewModel::onCustomerSelected,
            onPaymentMethodChange = viewModel::onPaymentMethodChange,
            onDiscountChange = viewModel::onDiscountChange,
            onConfirm = viewModel::onConfirmCheckout,
            onDismiss = viewModel::closeCheckout,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { CounterStrip(state, viewModel::openCheckout, viewModel::onClearCart) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenPadding, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(strings.searchNameOrBarcode) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                )
                if (isCameraScanningSupported()) {
                    IconButton(onClick = viewModel::openScanner) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                }
            }

            if (state.cart.lines.isNotEmpty()) {
                CartSection(
                    lines = state.cart.lines,
                    onQtyChange = viewModel::onQtyChange,
                    onQtyClick = { line -> qtyEntryFor = line.product },
                    onRemove = viewModel::onRemoveLine,
                )
                HorizontalDivider()
            }

            if (products.isEmpty()) {
                EmptyState(
                    title = if (query.isBlank()) strings.noProductsYet else strings.nothingFound,
                    message = if (query.isBlank()) strings.noProductsYetBody else strings.nothingFoundBody,
                    modifier = Modifier.weight(1f),
                    action = { Button(onClick = onAddProduct) { Text(strings.addProduct) } },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = spacing.xl),
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            inCartQty = state.cart.findLine(product.id)?.qty ?: 0.0,
                            onClick = {
                                if (product.unit.allowsFraction) qtyEntryFor = product
                                else viewModel.onProductTapped(product)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, inCartQty: Double, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    "${product.stockQty.formatQty()} ${product.unit.shortLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.isOutOfStock) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (product.isLowStock && !product.isOutOfStock) Pill(strings.lowStock)
                if (inCartQty > 0) Pill(
                    strings.inCart.fmt(inCartQty.formatQty()),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        MoneyText(product.salePrice, style = MoneyStyles.medium)
    }
}

@Composable
private fun CartSection(
    lines: List<CartLine>,
    onQtyChange: (Long, Double) -> Unit,
    onQtyClick: (CartLine) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(vertical = spacing.sm)) {
            lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenPadding, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MoneyText(line.lineTotal, style = MoneyStyles.small)
                    }
                    QtyStepper(
                        qty = line.qty,
                        unitLabel = line.product.unit.shortLabel,
                        onDecrease = { onQtyChange(line.product.id, line.qty - 1) },
                        onIncrease = { onQtyChange(line.product.id, line.qty + 1) },
                        onQtyClick = { onQtyClick(line) },
                    )
                    IconButton(onClick = { onRemove(line.product.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.removeItem.fmt(line.product.name))
                    }
                }
            }
        }
    }
}

/**
 * The receipt tape at the bottom of the counter: total on the left, the one
 * action on the right. It is the only thing on screen that never scrolls away.
 */
@Composable
private fun CounterStrip(state: PosUiState, onCheckout: () -> Unit, onClear: () -> Unit) {
    if (state.cart.isEmpty) return
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    strings.itemsInCart.fmt(state.cart.itemCount.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(state.cart.total, style = MoneyStyles.large)
            }
            TextButton(onClick = onClear) { Text(strings.clear) }
            Spacer(Modifier.width(spacing.sm))
            Button(onClick = onCheckout) { Text(strings.charge) }
        }
    }
}
