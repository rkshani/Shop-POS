package com.dukan.pos.presentation.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    cart: Cart,
    customers: List<Customer>,
    isSubmitting: Boolean,
    onCustomerSelected: (Customer?) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onDiscountChange: (Money) -> Unit,
    onConfirm: (Money, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current

    var discountText by remember { mutableStateOf("") }
    var paidText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Cash is the common case, so the paid field starts pre-filled with the total.
    LaunchedEffect(cart.total, cart.paymentMethod) {
        paidText = when (cart.paymentMethod) {
            PaymentMethod.UDHAAR -> ""
            else -> cart.total.rupees.toString().removeSuffix(".0")
        }
    }

    val paid = Money.parseOrNull(paidText) ?: Money.ZERO
    val due = cart.total - paid

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.screenPadding).padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(strings.amountDue, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(cart.total, style = MoneyStyles.counter)
                }
            }

            HorizontalDivider()

            SectionHeader(strings.payment)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(PaymentMethod.entries.toList()) { method ->
                    FilterChip(
                        selected = cart.paymentMethod == method,
                        onClick = { onPaymentMethodChange(method) },
                        label = { Text(method.label(strings)) },
                    )
                }
            }

            if (cart.paymentMethod == PaymentMethod.UDHAAR || due.isPositive) {
                SectionHeader(strings.customer)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(customers) { customer ->
                        FilterChip(
                            selected = cart.customer?.id == customer.id,
                            onClick = {
                                onCustomerSelected(if (cart.customer?.id == customer.id) null else customer)
                            },
                            label = { Text(customer.name) },
                        )
                    }
                }
                if (customers.isEmpty()) {
                    Text(
                        strings.addCustomerFirst,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        onDiscountChange(Money.parseOrNull(it) ?: Money.ZERO)
                    },
                    label = { Text(strings.discount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    label = { Text(strings.received) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("${strings.note} (${strings.optional})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (due.isPositive) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.goesOnUdhaar, style = MaterialTheme.typography.bodyMedium)
                    MoneyText(due, style = MoneyStyles.medium, color = MaterialTheme.colorScheme.error)
                }
            } else if (due.isNegative) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.changeBack, style = MaterialTheme.typography.bodyMedium)
                    MoneyText(Money(-due.paisa), style = MoneyStyles.medium)
                }
            }

            Spacer(Modifier.height(spacing.xs))
            Button(
                onClick = { onConfirm(paid, note.takeIf { it.isNotBlank() }) },
                enabled = !isSubmitting && !cart.isEmpty,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSubmitting) strings.saving else strings.saveSale)
            }
        }
    }
}

private fun PaymentMethod.label(strings: com.dukan.pos.core.i18n.Strings): String = when (this) {
    PaymentMethod.CASH -> strings.cash
    PaymentMethod.UDHAAR -> strings.udhaar
    PaymentMethod.EASYPAISA -> "Easypaisa"
    PaymentMethod.JAZZCASH -> "JazzCash"
    PaymentMethod.BANK -> "Bank"
    PaymentMethod.CARD -> "Card"
}
