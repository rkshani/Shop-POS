package com.dukan.pos.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.core.scanner.BarcodeScannerView
import com.dukan.pos.core.scanner.isCameraScanningSupported
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.presentation.common.localized
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    productId: Long,
    onDone: () -> Unit,
    viewModel: ProductEditViewModel = koinViewModel { parametersOf(productId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var scannerOpen by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProductEditEffect.Saved -> onDone()
                is ProductEditEffect.Failed ->
                    snackbarHostState.showSnackbar(effect.error.localized(strings))
            }
        }
    }

    if (scannerOpen) {
        BarcodeScannerView(
            onBarcode = {
                viewModel.onBarcodeScanned(it)
                scannerOpen = false
            },
            onClose = { scannerOpen = false },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) strings.editProduct else strings.newProduct) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(strings.name) },
                singleLine = true,
                isError = state.error != null && state.name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.nameUrdu,
                onValueChange = viewModel::onNameUrduChange,
                label = { Text("${strings.urduName} (${strings.optional})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = state.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    label = { Text(strings.barcode) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (isCameraScanningSupported()) {
                    IconButton(onClick = { scannerOpen = true }) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                }
            }

            SectionHeader(strings.unit)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(ProductUnit.entries.toList()) { unit ->
                    FilterChip(
                        selected = state.unit == unit,
                        onClick = { viewModel.onUnitChange(unit) },
                        label = { Text(unit.shortLabel) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(
                    value = state.costText,
                    onValueChange = viewModel::onCostChange,
                    label = { Text(strings.costPrice) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.priceText,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text(strings.salePrice) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.marginPerUnit, style = MaterialTheme.typography.bodyMedium)
                MoneyText(state.margin, style = MoneyStyles.medium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(
                    value = state.stockText,
                    onValueChange = viewModel::onStockChange,
                    label = { Text(strings.stock) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.lowStockText,
                    onValueChange = viewModel::onLowStockChange,
                    label = { Text(strings.alertAt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            state.error?.let {
                Text(it.localized(strings), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(bottom = spacing.xxl),
            ) {
                Text(if (state.isSaving) strings.saving else strings.save)
            }
        }
    }
}
