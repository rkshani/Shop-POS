package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.Instant

/**
 * A khata customer. [balance] is what the customer owes the shop:
 * positive = udhaar outstanding, negative = advance paid.
 */
data class Customer(
    val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val balance: Money = Money.ZERO,
    val creditLimit: Money? = null,
    val createdAt: Instant,
) {
    val hasOutstanding: Boolean get() = balance.isPositive
    val isOverLimit: Boolean get() = creditLimit?.let { balance > it } == true
}

data class LedgerEntry(
    val id: Long = 0L,
    val customerId: Long,
    val type: LedgerEntryType,
    /** Signed against the customer balance: sale adds, payment subtracts. */
    val amount: Money,
    val balanceAfter: Money,
    val saleId: Long? = null,
    val note: String? = null,
    val createdAt: Instant,
)

enum class LedgerEntryType { SALE, PAYMENT, ADJUSTMENT, OPENING_BALANCE }
