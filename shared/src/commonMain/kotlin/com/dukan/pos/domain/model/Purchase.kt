package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.Instant

data class Supplier(
    val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val createdAt: Instant,
)

/** Maal aane ki entry: stock in, and optionally a new cost price. */
data class Purchase(
    val id: Long = 0L,
    val reference: String = "",
    val supplierName: String? = null,
    val billNo: String? = null,
    val items: List<PurchaseItem> = emptyList(),
    val note: String? = null,
    val createdAt: Instant,
) {
    val total: Money get() = items.fold(Money.ZERO) { acc, item -> acc + item.lineTotal }
    val isEmpty: Boolean get() = items.isEmpty()
}

data class PurchaseItem(
    val productId: Long,
    val productName: String,
    val unit: ProductUnit,
    val qty: Double,
    val unitCost: Money,
    /**
     * Off by default. Cost prices should follow the latest bill, but a one-off
     * emergency purchase at a corner shop's price should not reprice the shelf.
     */
    val updatesCostPrice: Boolean = true,
) {
    val lineTotal: Money get() = unitCost * qty
}
