package com.dukan.pos.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.designsystem.LocalCurrencySymbol
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.domain.model.ProductUnit

/**
 * Typed quantity entry for a cart, purchase or return line. Loose items
 * (kg, litre, metre…) can also be entered by amount — "Rs 50 ki cheeni".
 *
 * [unitPrice] prices the preview and the amount mode; [allowAmount] turns the
 * amount mode on where it makes sense (selling, not buying or returning).
 */
@Composable
fun QuantityDialog(
    title: String,
    unit: ProductUnit,
    unitPrice: Money,
    initialQty: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    maxQty: Double? = null,
    allowAmount: Boolean = false,
) {
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current
    val currency = LocalCurrencySymbol.current
    val amountAvailable = allowAmount && unit.allowsFraction && unitPrice.isPositive

    var mode by remember { mutableStateOf(QuantityInputMode.QUANTITY) }
    var input by remember {
        val text = initialQuantityText(initialQty)
        // Pre-selected, so typing replaces the old quantity instead of appending to it.
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val result = resolveQuantityInput(input.text, mode, unit, unitPrice, maxQty, initialQty)
    val confirm = { (result as? QuantityInputResult.Valid)?.let { onConfirm(it.qty) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                if (amountAvailable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        FilterChip(
                            selected = mode == QuantityInputMode.QUANTITY,
                            onClick = {
                                mode = QuantityInputMode.QUANTITY
                                input = TextFieldValue("")
                            },
                            label = { Text(strings.byQuantity) },
                        )
                        FilterChip(
                            selected = mode == QuantityInputMode.AMOUNT,
                            onClick = {
                                mode = QuantityInputMode.AMOUNT
                                input = TextFieldValue("")
                            },
                            label = { Text(strings.byAmount) },
                        )
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = {
                        Text(
                            when (mode) {
                                QuantityInputMode.QUANTITY -> "${strings.quantity} (${unit.shortLabel})"
                                QuantityInputMode.AMOUNT -> "${strings.amount} ($currency)"
                            },
                        )
                    },
                    isError = input.text.isNotBlank() && result !is QuantityInputResult.Valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )

                when (result) {
                    is QuantityInputResult.Valid -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${result.qty.formatQty()} ${unit.shortLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (unitPrice.isPositive) MoneyText(unitPrice * result.qty, style = MoneyStyles.medium)
                    }
                    QuantityInputResult.WholeNumberRequired -> ErrorLine(strings.wholeNumberOnly.fmt(unit.shortLabel))
                    is QuantityInputResult.AboveMax ->
                        ErrorLine(strings.atMost.fmt("${result.max.formatQty()} ${unit.shortLabel}"))
                    QuantityInputResult.Invalid ->
                        if (input.text.isNotBlank()) ErrorLine(strings.enterValidQuantity)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = result is QuantityInputResult.Valid) {
                Text(strings.done)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}

@Composable
private fun ErrorLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
