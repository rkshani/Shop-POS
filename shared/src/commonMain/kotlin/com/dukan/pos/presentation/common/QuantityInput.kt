package com.dukan.pos.presentation.common

import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.toQtyOrNull
import com.dukan.pos.domain.model.ProductUnit
import kotlin.math.abs
import kotlin.math.roundToLong

/** How the shopkeeper describes the quantity: "1.5 kg" or "Rs 50 ka". */
enum class QuantityInputMode { QUANTITY, AMOUNT }

sealed interface QuantityInputResult {
    data class Valid(val qty: Double) : QuantityInputResult
    data object Invalid : QuantityInputResult
    data object WholeNumberRequired : QuantityInputResult
    data class AboveMax(val max: Double) : QuantityInputResult
}

/** Typed quantities keep four decimals: enough for grams, without 0.1 + 0.2 noise. */
private const val QTY_SCALE = 10_000.0
private const val EPSILON = 0.00005

/**
 * Turns what was typed into a quantity, or says why it can't. [unitPrice] is
 * only needed for [QuantityInputMode.AMOUNT]; [maxQty] caps it (e.g. what is
 * still returnable on an invoice).
 */
fun resolveQuantityInput(
    text: String,
    mode: QuantityInputMode,
    unit: ProductUnit,
    unitPrice: Money,
    maxQty: Double? = null,
): QuantityInputResult {
    val number = text.toQtyOrNull() ?: return QuantityInputResult.Invalid
    if (number <= 0.0) return QuantityInputResult.Invalid

    val qty = when (mode) {
        QuantityInputMode.QUANTITY -> number.roundToScale()
        QuantityInputMode.AMOUNT -> {
            if (!unit.allowsFraction || !unitPrice.isPositive) return QuantityInputResult.Invalid
            // Not rounded: "Rs 50 ki cheeni" must price back to exactly Rs 50,
            // and 0.1786 kg at Rs 280 is Rs 50.01.
            val amountPaisa = (number * 100.0).roundToLong()
            amountPaisa.toDouble() / unitPrice.paisa
        }
    }

    if (qty <= 0.0) return QuantityInputResult.Invalid
    if (!unit.allowsFraction && abs(qty - qty.roundToLong()) > EPSILON) {
        return QuantityInputResult.WholeNumberRequired
    }
    if (maxQty != null && qty > maxQty + EPSILON) return QuantityInputResult.AboveMax(maxQty)
    return QuantityInputResult.Valid(qty)
}

private fun Double.roundToScale(): Double = (this * QTY_SCALE).roundToLong() / QTY_SCALE
