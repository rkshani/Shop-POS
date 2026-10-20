package com.dukan.pos.domain

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.ReturnConflictException
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.SaleItem
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.model.SaleReturnItem
import com.dukan.pos.domain.model.returnableQty
import com.dukan.pos.domain.repository.ReturnRepository
import com.dukan.pos.domain.usecase.RecordSaleReturnUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeReturnRepository(
    private val failWith: Exception? = null,
) : ReturnRepository {
    var recorded: SaleReturn? = null
    override suspend fun recordReturn(saleReturn: SaleReturn): String {
        failWith?.let { throw it }
        recorded = saleReturn
        return "RET-00001"
    }
    override fun observeReturnsForSale(saleId: Long): Flow<List<SaleReturn>> = flowOf(emptyList())
}

private class ReturnTestClock : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)
    override fun timeZone(): TimeZone = TimeZone.UTC
}

class ReturnTest {

    private fun soldItem(qty: Double, returned: Double) = SaleItem(
        id = 5,
        saleId = 1,
        productId = 9,
        productName = "Cooking oil 1L",
        unit = ProductUnit.LITRE,
        qty = qty,
        unitPrice = Money.fromRupees(600.0),
        unitCost = Money.fromRupees(520.0),
        returnedQty = returned,
    )

    private fun returnLine(qty: Double) = SaleReturnItem(
        saleItemId = 5,
        productId = 9,
        productName = "Cooking oil 1L",
        qty = qty,
        unitPrice = Money.fromRupees(600.0),
        unitCost = Money.fromRupees(520.0),
    )

    private fun draft(
        items: List<SaleReturnItem>,
        method: RefundMethod = RefundMethod.CASH,
        customerId: Long? = null,
    ) = SaleReturn(
        saleId = 1,
        customerId = customerId,
        items = items,
        refundMethod = method,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun returnable_quantity_accounts_for_earlier_returns() {
        assertEquals(3.0, soldItem(qty = 5.0, returned = 2.0).returnableQty)
        assertEquals(0.0, soldItem(qty = 5.0, returned = 5.0).returnableQty)
        // Never negative, even if data drifts.
        assertEquals(0.0, soldItem(qty = 5.0, returned = 7.0).returnableQty)
    }

    @Test
    fun refund_total_is_the_sum_of_returned_lines() {
        assertEquals(Money.fromRupees(1200.0), draft(listOf(returnLine(2.0))).refund)
    }

    @Test
    fun an_empty_return_is_refused() = runTest {
        val useCase = RecordSaleReturnUseCase(FakeReturnRepository(), ReturnTestClock())
        val result = useCase(draft(emptyList()))
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun zero_quantity_lines_are_dropped_before_saving() = runTest {
        val repository = FakeReturnRepository()
        val useCase = RecordSaleReturnUseCase(repository, ReturnTestClock())

        useCase(draft(listOf(returnLine(0.0), returnLine(1.0))))

        assertEquals(1, repository.recorded?.items?.size)
    }

    @Test
    fun refunding_into_a_khata_requires_a_customer() = runTest {
        val useCase = RecordSaleReturnUseCase(FakeReturnRepository(), ReturnTestClock())
        val result = useCase(draft(listOf(returnLine(1.0)), RefundMethod.KHATA, customerId = null))
        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.CustomerRequiredForUdhaar, result.error)
    }

    @Test
    fun a_database_level_conflict_becomes_a_readable_error() = runTest {
        val useCase = RecordSaleReturnUseCase(
            FakeReturnRepository(failWith = ReturnConflictException("Cooking oil 1L", 1.0)),
            ReturnTestClock(),
        )
        val result = useCase(draft(listOf(returnLine(3.0))))
        assertIs<AppResult.Failure>(result)
        assertIs<AppError.NotEnoughStock>(result.error)
    }
}
