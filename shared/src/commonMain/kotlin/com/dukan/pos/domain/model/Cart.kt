package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money

/**
 * The counter cart. Pure data with derived totals — no repository, no IO —
 * so every pricing rule is unit-testable without a database.
 */
data class Cart(
    val lines: List<CartLine> = emptyList(),
    val discount: Money = Money.ZERO,
    val customer: Customer? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.size
    val subTotal: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.lineTotal }
    val total: Money get() = (subTotal - discount).coerceAtLeastZero()
    val estimatedProfit: Money
        get() = lines.fold(Money.ZERO) { acc, line ->
            acc + (line.unitPrice - line.product.costPrice) * line.qty
        } - discount

    fun findLine(productId: Long): CartLine? = lines.firstOrNull { it.product.id == productId }

    fun upsert(product: Product, qtyDelta: Double): Cart {
        val existing = findLine(product.id)
        return if (existing == null) {
            if (qtyDelta <= 0) this
            else copy(lines = lines + CartLine(product, qtyDelta, product.salePrice))
        } else {
            val newQty = existing.qty + qtyDelta
            if (newQty <= 0) removeLine(product.id)
            else copy(lines = lines.map { if (it.product.id == product.id) it.copy(qty = newQty) else it })
        }
    }

    fun setQty(productId: Long, qty: Double): Cart =
        if (qty <= 0) removeLine(productId)
        else copy(lines = lines.map { if (it.product.id == productId) it.copy(qty = qty) else it })

    fun setUnitPrice(productId: Long, price: Money): Cart =
        copy(lines = lines.map { if (it.product.id == productId) it.copy(unitPrice = price) else it })

    fun removeLine(productId: Long): Cart =
        copy(lines = lines.filterNot { it.product.id == productId })

    fun clearLines(): Cart = Cart(paymentMethod = paymentMethod)
}

data class CartLine(
    val product: Product,
    val qty: Double,
    val unitPrice: Money,
) {
    val lineTotal: Money get() = unitPrice * qty
    val exceedsStock: Boolean get() = qty > product.stockQty
}

private fun Money.coerceAtLeastZero(): Money = if (isNegative) Money.ZERO else this
