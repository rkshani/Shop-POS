package com.dukan.pos.presentation.khata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.SystemClock
import com.dukan.pos.core.common.formatShort
import com.dukan.pos.core.designsystem.EmptyState
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.presentation.common.localized
import com.dukan.pos.domain.model.LedgerEntry
import com.dukan.pos.domain.model.LedgerEntryType
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    onBack: () -> Unit,
    viewModel: CustomerDetailViewModel = koinViewModel { parametersOf(customerId) },
) {
    val customer by viewModel.customer.collectAsStateWithLifecycle()
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val spacing = LocalSpacing.current
    val zone = remember { SystemClock().timeZone() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                is CustomerDetailEffect.Failed -> effect.error.localized(strings)
                CustomerDetailEffect.PaymentRecorded -> strings.paymentRecorded
                CustomerDetailEffect.InvalidAmount -> strings.enterValidAmount
                CustomerDetailEffect.NoPhone -> strings.noPhoneForReminder
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: strings.navKhata) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = spacing.screenPadding)) {
                Text(
                    strings.balance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(
                    customer?.balance ?: Money.ZERO,
                    style = MoneyStyles.counter,
                    color = if (customer?.hasOutstanding == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(strings.collectPayment) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        viewModel.onPaymentReceived(amount, note.takeIf { it.isNotBlank() })
                        amount = ""
                        note = ""
                    },
                    enabled = amount.isNotBlank(),
                ) { Text(strings.record) }
            }

            if (customer?.hasOutstanding == true) {
                OutlinedButton(
                    onClick = {
                        val target = customer ?: return@OutlinedButton
                        viewModel.onSendReminder(
                            strings.reminderTemplate.fmt(
                                target.name,
                                settings.shopName,
                                target.balance.format(settings.currencySymbol),
                                settings.shopName,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenPadding),
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = null)
                    Spacer(Modifier.width(spacing.sm))
                    Text(strings.sendReminder)
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("${strings.note} (${strings.optional})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(spacing.screenPadding),
            )

            HorizontalDivider()

            if (ledger.isEmpty()) {
                EmptyState(
                    title = strings.ledgerEmpty,
                    message = strings.ledgerEmptyBody,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = spacing.xxl),
                ) {
                    items(ledger, key = { it.id }) { entry ->
                        LedgerRow(entry, zone)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(entry: LedgerEntry, zone: kotlinx.datetime.TimeZone) {
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenPadding, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                when (entry.type) {
                    LedgerEntryType.SALE -> strings.udhaarSale
                    LedgerEntryType.PAYMENT -> strings.paymentReceived
                    LedgerEntryType.ADJUSTMENT -> strings.adjustment
                    LedgerEntryType.OPENING_BALANCE -> strings.openingBalance
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                buildString {
                    append(entry.createdAt.toLocalDateTime(zone).formatShort())
                    entry.note?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                entry.amount,
                style = MoneyStyles.small,
                color = if (entry.amount.isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                strings.remaining.fmt(entry.balanceAfter.format("")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
