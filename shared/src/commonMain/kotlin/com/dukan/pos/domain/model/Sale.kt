package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.Instant

data class Sale(
    val id: Long = 0L,
    val invoiceNo: String,
    val customerId: Long? = null,
    val customerName: String? = null,
    val items: List<SaleItem> = emptyList(),
    val subTotal: Money,
    val discount: Money = Money.ZERO,
    val total: Money,
    val paid: Money,
    val paymentMethod: PaymentMethod,
    val note: String? = null,
    val createdAt: Instant,
) {
    val due: Money get() = total - paid
    val hasReturns: Boolean get() = items.any { it.returnedQty > 0 }
    val isFullyPaid: Boolean get() = !due.isPositive

    /** Profit uses the cost snapshot taken at sale time, not today's cost. */
    val profit: Money
        get() = items.fold(Money.ZERO) { acc, item ->
            acc + (item.unitPrice - item.unitCost) * item.qty
        } - discount
}

data class SaleItem(
    val id: Long = 0L,
    val saleId: Long = 0L,
    val productId: Long,
    val productName: String,
    val unit: ProductUnit,
    val qty: Double,
    val unitPrice: Money,
    val unitCost: Money,
    val returnedQty: Double = 0.0,
) {
    val lineTotal: Money get() = unitPrice * qty
    val isFullyReturned: Boolean get() = returnedQty >= qty
}

enum class PaymentMethod { CASH, UDHAAR, EASYPAISA, JAZZCASH, BANK, CARD }

data class StockMovement(
    val id: Long = 0L,
    val productId: Long,
    val type: StockMovementType,
    /** Signed: purchases are positive, sales negative. */
    val qtyChange: Double,
    val qtyAfter: Double,
    val saleId: Long? = null,
    val note: String? = null,
    val createdAt: Instant,
)

enum class StockMovementType { PURCHASE, SALE, SALE_RETURN, ADJUSTMENT, WASTAGE, OPENING }
