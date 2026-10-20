package com.dukan.pos.presentation.purchase

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.designsystem.EmptyState
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.Pill
import com.dukan.pos.core.designsystem.QtyStepper
import com.dukan.pos.presentation.common.QuantityDialog
import com.dukan.pos.presentation.common.localized
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    viewModel: PurchaseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PurchaseEffect.Saved -> {
                    snackbarHostState.showSnackbar("${strings.purchaseSaved} · ${effect.reference}")
                    onBack()
                }
                is PurchaseEffect.Failed ->
                    snackbarHostState.showSnackbar(effect.error.localized(strings))
            }
        }
    }

    var qtyEntryFor by remember { mutableStateOf<Long?>(null) }
    state.lines.firstOrNull { it.product.id == qtyEntryFor }?.let { line ->
        QuantityDialog(
            title = line.product.name,
            unit = line.product.unit,
            unitPrice = line.unitCost,
            initialQty = line.qty,
            onConfirm = { qty ->
                viewModel.onQtyChange(line.product.id, qty)
                qtyEntryFor = null
            },
            onDismiss = { qtyEntryFor = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.newPurchase) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
        bottomBar = {
            if (!state.isEmpty) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings.total,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            MoneyText(state.total, style = MoneyStyles.large)
                        }
                        Button(onClick = viewModel::onSave, enabled = !state.isSaving) {
                            Text(if (state.isSaving) strings.saving else strings.savePurchase)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                OutlinedTextField(
                    value = state.supplier,
                    onValueChange = viewModel::onSupplierChange,
                    label = { Text(strings.supplier) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.billNo,
                    onValueChange = viewModel::onBillNoChange,
                    label = { Text(strings.billNo) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(spacing.screenPadding),
                placeholder = { Text(strings.addItemsToPurchase) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
            )

            if (state.lines.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(vertical = spacing.sm)) {
                        state.lines.forEach { line ->
                            PurchaseLineRow(
                                line = line,
                                onQtyChange = { viewModel.onQtyChange(line.product.id, it) },
                                onQtyClick = { qtyEntryFor = line.product.id },
                                onCostChange = { viewModel.onCostChange(line.product.id, it) },
                                onToggleCost = { viewModel.onUpdateCostToggled(line.product.id) },
                                onRemove = { viewModel.onRemoveLine(line.product.id) },
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            if (products.isEmpty() && state.isEmpty) {
                EmptyState(
                    title = strings.purchaseEmpty,
                    message = strings.purchaseEmptyBody,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = spacing.xxl),
                ) {
                    items(products, key = { it.id }) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onProductTapped(product) }
                                .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${strings.stock} ${product.stockQty.formatQty()} ${product.unit.shortLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(product.costPrice, style = MoneyStyles.small)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseLineRow(
    line: PurchaseLine,
    onQtyChange: (Double) -> Unit,
    onQtyClick: () -> Unit,
    onCostChange: (Money) -> Unit,
    onToggleCost: () -> Unit,
    onRemove: () -> Unit,
) {
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current

    Column(Modifier.padding(horizontal = spacing.screenPadding, vertical = spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    line.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MoneyText(line.lineTotal, style = MoneyStyles.small)
            }
            QtyStepper(
                qty = line.qty,
                unitLabel = line.product.unit.shortLabel,
                onDecrease = { onQtyChange(line.qty - 1) },
                onIncrease = { onQtyChange(line.qty + 1) },
                onQtyClick = onQtyClick,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = strings.delete)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = line.unitCost.rupees.toString().removeSuffix(".0"),
                onValueChange = { onCostChange(Money.parseOrNull(it) ?: Money.ZERO) },
                label = { Text(strings.costPrice) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.width(140.dp),
            )
            Spacer(Modifier.width(spacing.sm))
            Checkbox(checked = line.updatesCostPrice, onCheckedChange = { onToggleCost() })
            Text(strings.updateCostPrice, style = MaterialTheme.typography.bodySmall)
            if (line.costChanged) {
                Spacer(Modifier.width(spacing.sm))
                Pill(line.product.costPrice.format(""))
            }
        }
        Spacer(Modifier.height(spacing.xs))
    }
}
