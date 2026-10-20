package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.Instant

data class SaleReturn(
    val id: Long = 0L,
    val reference: String = "",
    val saleId: Long,
    val customerId: Long? = null,
    val items: List<SaleReturnItem> = emptyList(),
    val refundMethod: RefundMethod = RefundMethod.CASH,
    val note: String? = null,
    /** Authoritative calculated refund for discounted invoices; DAO rechecks it. */
    val allocatedRefund: Money? = null,
    val createdAt: Instant,
) {
    val refund: Money get() = allocatedRefund ?: items.fold(Money.ZERO) { acc, item -> acc + item.lineTotal }
    val isEmpty: Boolean get() = items.isEmpty()
}

data class SaleReturnItem(
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double,
    val unitPrice: Money,
    val unitCost: Money,
) {
    val lineTotal: Money get() = unitPrice * qty
}

enum class RefundMethod { CASH, KHATA }

/** How much of a sold line can still come back. */
val SaleItem.returnableQty: Double get() = (qty - returnedQty).coerceAtLeast(0.0)
