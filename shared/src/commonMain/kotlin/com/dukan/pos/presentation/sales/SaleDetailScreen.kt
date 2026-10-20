package com.dukan.pos.presentation.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.SystemClock
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.common.formatShort
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.Pill
import com.dukan.pos.core.designsystem.QtyStepper
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.SaleItem
import com.dukan.pos.domain.model.returnableQty
import com.dukan.pos.presentation.common.localized
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: Long,
    onBack: () -> Unit,
    viewModel: SaleDetailViewModel = koinViewModel { parametersOf(saleId) },
) {
    val sale by viewModel.sale.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    val zone = remember { SystemClock().timeZone() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                is SaleDetailEffect.ReturnSaved -> "${strings.returnSaved} · ${effect.reference}"
                is SaleDetailEffect.Failed -> effect.error.localized(strings)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val current = sale
    val anythingReturnable = current?.items?.any { it.returnableQty > 0 } == true

    if (draft.isOpen && current != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = viewModel::closeReturn, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.screenPadding)
                    .padding(bottom = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(strings.returnTitle, style = MaterialTheme.typography.titleLarge)

                current.items.filter { it.returnableQty > 0 }.forEach { item ->
                    val qty = draft.quantities[item.id] ?: 0.0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${item.returnableQty.formatQty()} ${item.unit.shortLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        QtyStepper(
                            qty = qty,
                            unitLabel = item.unit.shortLabel,
                            onDecrease = { viewModel.onQtyChange(item.id, qty - 1) },
                            onIncrease = { viewModel.onQtyChange(item.id, qty + 1) },
                        )
                    }
                }

                SectionHeader(strings.refundMethod)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    FilterChip(
                        selected = draft.refundMethod == RefundMethod.CASH,
                        onClick = { viewModel.onRefundMethodChange(RefundMethod.CASH) },
                        label = { Text(strings.refundCash) },
                    )
                    if (current.customerId != null) {
                        FilterChip(
                            selected = draft.refundMethod == RefundMethod.KHATA,
                            onClick = { viewModel.onRefundMethodChange(RefundMethod.KHATA) },
                            label = { Text(strings.refundToKhata) },
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.total, style = MaterialTheme.typography.bodyMedium)
                    MoneyText(viewModel.refundTotal(), style = MoneyStyles.large)
                }

                Button(
                    onClick = viewModel::onConfirmReturn,
                    enabled = draft.hasSelection && !draft.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (draft.isSaving) strings.saving else strings.returnTitle)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(current?.invoiceNo ?: strings.invoice) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = spacing.screenPadding, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Column {
                    MoneyText(current.total, style = MoneyStyles.counter)
                    Text(
                        buildString {
                            append(current.createdAt.toLocalDateTime(zone).formatShort())
                            current.customerName?.let { append(" · $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Pill("${strings.paid} ${current.paid.format("")}")
                    if (current.due.isPositive) {
                        Pill(
                            "${strings.due} ${current.due.format("")}",
                            container = MaterialTheme.colorScheme.errorContainer,
                            content = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = spacing.sm)) }

            items(current.items, key = { it.id }) { item -> SaleItemRow(item) }

            item {
                Spacer(Modifier.height(spacing.lg))
                if (anythingReturnable) {
                    OutlinedButton(onClick = viewModel::openReturn, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.returnItems)
                    }
                } else {
                    Text(
                        strings.alreadyFullyReturned,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleItemRow(item: SaleItem) {
    val strings = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.qty.formatQty()} ${item.unit.shortLabel} × ${item.unitPrice.format("")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.returnedQty > 0) {
                    Pill(
                        strings.returnedAlready.fmt(item.returnedQty.formatQty()),
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        MoneyText(item.lineTotal, style = MoneyStyles.small)
    }
}
