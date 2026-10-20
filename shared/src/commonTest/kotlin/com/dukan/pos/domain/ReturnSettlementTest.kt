package com.dukan.pos.domain

import com.dukan.pos.domain.model.validateCashRefund
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReturnSettlementTest {
    @Test fun fullyPaidSaleAllowsRecordedAmount() {
        validateCashRefund(100_000L, 40_000L, 60_000L)
    }

    @Test fun unpaidSaleCannotGenerateCashRefund() {
        assertFailsWith<IllegalArgumentException> { validateCashRefund(0L, 0L, 1L) }
    }

    @Test fun partiallyPaidSaleCannotRefundMoreCashThanTaken() {
        assertFailsWith<IllegalArgumentException> { validateCashRefund(30_000L, 20_000L, 10_001L) }
        validateCashRefund(30_000L, 20_000L, 10_000L)
    }

    @Test fun invalidAndCorruptSettlementsAreRejected() {
        assertFailsWith<IllegalArgumentException> { validateCashRefund(-1L, 0L, 0L) }
        assertFailsWith<IllegalArgumentException> { validateCashRefund(100L, 101L, 0L) }
        assertFailsWith<IllegalArgumentException> { validateCashRefund(100L, 0L, -1L) }
    }
}
