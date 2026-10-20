package com.dukan.pos.presentation

import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.presentation.common.QuantityInputMode.AMOUNT
import com.dukan.pos.presentation.common.QuantityInputMode.QUANTITY
import com.dukan.pos.presentation.common.QuantityInputResult
import com.dukan.pos.presentation.common.initialQuantityText
import com.dukan.pos.presentation.common.resolveQuantityInput
import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityInputTest {

    private val cheeniPerKg = Money.fromRupees(280L)

    @Test
    fun fractionalWeightIsAccepted() {
        assertEquals(
            QuantityInputResult.Valid(1.5),
            resolveQuantityInput("1.5", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg),
        )
        assertEquals(
            QuantityInputResult.Valid(0.25),
            resolveQuantityInput(".25", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg),
        )
    }

    @Test
    fun amountIsTurnedIntoWeightThatPricesBackToTheAmount() {
        val result = resolveQuantityInput("50", AMOUNT, ProductUnit.KILOGRAM, cheeniPerKg)

        val qty = (result as QuantityInputResult.Valid).qty
        assertEquals(0.1786, qty, absoluteTolerance = 0.0001)
        assertEquals(Money.fromRupees(50L), cheeniPerKg * qty)
    }

    @Test
    fun everyRupeeAmountPricesBackExactly() {
        val prices = listOf(280L, 333L, 1_799L, 95L, 12_500L).map { Money.fromRupees(it) }
        for (price in prices) {
            for (rupees in listOf(10L, 25L, 50L, 75L, 99L, 150L, 1_000L)) {
                val qty = (resolveQuantityInput(rupees.toString(), AMOUNT, ProductUnit.KILOGRAM, price)
                    as QuantityInputResult.Valid).qty
                assertEquals(Money.fromRupees(rupees), price * qty, "Rs $rupees at ${price.format()}")
            }
        }
    }

    @Test
    fun reopeningAnAmountLineAndConfirmingKeepsTheAmount() {
        val qty = (resolveQuantityInput("50", AMOUNT, ProductUnit.KILOGRAM, cheeniPerKg)
            as QuantityInputResult.Valid).qty
        val prefilled = initialQuantityText(qty)

        val reconfirmed = resolveQuantityInput(prefilled, QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg, initialQty = qty)

        assertEquals(QuantityInputResult.Valid(qty), reconfirmed)
        assertEquals(Money.fromRupees(50L), cheeniPerKg * qty)
    }

    @Test
    fun editedTextIsReadAgainEvenWithAnInitialQty() {
        assertEquals(
            QuantityInputResult.Valid(0.25),
            resolveQuantityInput("0.25", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg, initialQty = 0.17857142857142858),
        )
    }

    @Test
    fun piecesMustBeWhole() {
        assertEquals(
            QuantityInputResult.WholeNumberRequired,
            resolveQuantityInput("2.5", QUANTITY, ProductUnit.PIECE, Money.fromRupees(30L)),
        )
        assertEquals(
            QuantityInputResult.Valid(12.0),
            resolveQuantityInput("12", QUANTITY, ProductUnit.PIECE, Money.fromRupees(30L)),
        )
    }

    @Test
    fun amountModeIsOnlyForLooseItemsWithAPrice() {
        assertEquals(
            QuantityInputResult.Invalid,
            resolveQuantityInput("50", AMOUNT, ProductUnit.PIECE, Money.fromRupees(30L)),
        )
        assertEquals(
            QuantityInputResult.Invalid,
            resolveQuantityInput("50", AMOUNT, ProductUnit.KILOGRAM, Money.ZERO),
        )
    }

    @Test
    fun junkZeroAndNegativeAreRejected() {
        listOf("", "abc", "0", "-1", "NaN", "Infinity", "0.00001").forEach { text ->
            assertEquals(
                QuantityInputResult.Invalid,
                resolveQuantityInput(text, QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg),
                "input '$text'",
            )
        }
    }

    @Test
    fun cappedAtWhatIsStillReturnable() {
        assertEquals(
            QuantityInputResult.AboveMax(0.5),
            resolveQuantityInput("0.75", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg, maxQty = 0.5),
        )
        assertEquals(
            QuantityInputResult.Valid(0.5),
            resolveQuantityInput("0.5", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg, maxQty = 0.5),
        )
    }

    @Test
    fun floatingPointNoiseIsRoundedAway() {
        assertEquals(
            QuantityInputResult.Valid(0.3),
            resolveQuantityInput("0.30000000000000004", QUANTITY, ProductUnit.KILOGRAM, cheeniPerKg),
        )
    }
}
