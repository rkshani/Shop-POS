package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.LocalDate

data class SalesSummary(
    val salesCount: Int = 0,
    val grossSales: Money = Money.ZERO,
    val discountGiven: Money = Money.ZERO,
    val cashCollected: Money = Money.ZERO,
    val udhaarAdded: Money = Money.ZERO,
    val profit: Money = Money.ZERO,
    val returnedAmount: Money = Money.ZERO,
) {
    val averageTicket: Money
        get() = if (salesCount == 0) Money.ZERO else Money(grossSales.paisa / salesCount)
}

data class DailyPoint(
    val date: LocalDate,
    val sales: Money,
    val profit: Money,
)

data class TopProduct(
    val productId: Long,
    val name: String,
    val qtySold: Double,
    val revenue: Money,
    val profit: Money,
)

data class DashboardSnapshot(
    val today: SalesSummary = SalesSummary(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalUdhaarOutstanding: Money = Money.ZERO,
    val stockValueAtCost: Money = Money.ZERO,
    val lastSevenDays: List<DailyPoint> = emptyList(),
)
