package com.dukan.pos.domain

import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.ProductUnit
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartTest {

    private fun product(
        id: Long,
        sale: Double,
        cost: Double = 0.0,
        stock: Double = 100.0,
    ) = Product(
        id = id,
        name = "Item $id",
        unit = ProductUnit.PIECE,
        costPrice = Money.fromRupees(cost),
        salePrice = Money.fromRupees(sale),
        stockQty = stock,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun adding_same_product_twice_increases_quantity() {
        val cart = Cart()
            .upsert(product(1, 50.0), 1.0)
            .upsert(product(1, 50.0), 1.0)

        assertEquals(1, cart.itemCount)
        assertEquals(2.0, cart.lines.single().qty)
        assertEquals(Money.fromRupees(100.0), cart.subTotal)
    }

    @Test
    fun reducing_quantity_to_zero_removes_the_line() {
        val cart = Cart().upsert(product(1, 50.0), 1.0).upsert(product(1, 50.0), -1.0)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun discount_never_pushes_total_below_zero() {
        val cart = Cart()
            .upsert(product(1, 50.0), 2.0)
            .copy(discount = Money.fromRupees(500.0))

        assertEquals(Money.ZERO, cart.total)
    }

    @Test
    fun profit_uses_cost_price_and_subtracts_discount() {
        val cart = Cart()
            .upsert(product(1, sale = 100.0, cost = 70.0), 3.0)
            .copy(discount = Money.fromRupees(20.0))

        // (100 - 70) * 3 = 90, minus the 20 discount
        assertEquals(Money.fromRupees(70.0), cart.estimatedProfit)
    }

    @Test
    fun line_flags_itself_when_quantity_passes_available_stock() {
        val cart = Cart().upsert(product(1, 50.0, stock = 2.0), 3.0)
        assertTrue(cart.lines.single().exceedsStock)
    }

    @Test
    fun manual_price_override_applies_to_that_line_only() {
        val cart = Cart()
            .upsert(product(1, 50.0), 1.0)
            .upsert(product(2, 50.0), 1.0)
            .setUnitPrice(1, Money.fromRupees(45.0))

        assertEquals(Money.fromRupees(95.0), cart.subTotal)
    }
}
