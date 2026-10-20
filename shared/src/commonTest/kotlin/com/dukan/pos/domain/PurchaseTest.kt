package com.dukan.pos.domain

import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.model.PurchaseItem
import com.dukan.pos.domain.model.Supplier
import com.dukan.pos.domain.repository.PurchaseRepository
import com.dukan.pos.domain.usecase.RecordPurchaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakePurchaseRepository : PurchaseRepository {
    var recorded: Purchase? = null
    override suspend fun recordPurchase(purchase: Purchase): String {
        recorded = purchase
        return "PUR-00001"
    }
    override fun observeRecent(limit: Int): Flow<List<Purchase>> = flowOf(emptyList())
    override fun observeSuppliers(): Flow<List<Supplier>> = flowOf(emptyList())
}

private class StoppedClock : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)
    override fun timeZone(): TimeZone = TimeZone.UTC
}

class PurchaseTest {

    private fun item(qty: Double, cost: Double, updatesCost: Boolean = true) = PurchaseItem(
        productId = 1,
        productName = "Sugar 50kg bag",
        unit = ProductUnit.KILOGRAM,
        qty = qty,
        unitCost = Money.fromRupees(cost),
        updatesCostPrice = updatesCost,
    )

    private fun purchase(items: List<PurchaseItem>) = Purchase(
        supplierName = "Karachi Traders",
        items = items,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun total_is_quantity_times_cost_across_lines() {
        val bill = purchase(listOf(item(2.0, 9500.0), item(1.0, 4200.0)))
        assertEquals(Money.fromRupees(23200.0), bill.total)
    }

    @Test
    fun an_empty_bill_is_refused() = runTest {
        val useCase = RecordPurchaseUseCase(FakePurchaseRepository(), StoppedClock())
        assertIs<AppResult.Failure>(useCase(purchase(emptyList())))
    }

    @Test
    fun a_zero_quantity_line_is_refused() = runTest {
        val useCase = RecordPurchaseUseCase(FakePurchaseRepository(), StoppedClock())
        assertIs<AppResult.Failure>(useCase(purchase(listOf(item(0.0, 100.0)))))
    }

    @Test
    fun the_cost_update_flag_travels_through_untouched() = runTest {
        val repository = FakePurchaseRepository()
        val useCase = RecordPurchaseUseCase(repository, StoppedClock())

        useCase(purchase(listOf(item(1.0, 100.0, updatesCost = false))))

        assertEquals(false, repository.recorded?.items?.single()?.updatesCostPrice)
    }

    @Test
    fun the_recorded_time_comes_from_the_clock_not_the_caller() = runTest {
        val repository = FakePurchaseRepository()
        val useCase = RecordPurchaseUseCase(repository, StoppedClock())

        useCase(purchase(listOf(item(1.0, 100.0))))

        assertEquals(StoppedClock().now(), repository.recorded?.createdAt)
    }
}
