package com.dukan.pos.domain

import com.dukan.pos.domain.model.allocatedRefundPaisa
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RefundAllocationTest {
    @Test fun completeDiscountedReturnRefundsInvoiceExactly() {
        assertEquals(90_000L, allocatedRefundPaisa(100_000L, 90_000L, 0L, 100_000L))
    }
    @Test fun repeatedPartialReturnsReconcileToInvoiceTotal() {
        val first = allocatedRefundPaisa(100_000L, 90_000L, 0L, 30_000L)
        val second = allocatedRefundPaisa(100_000L, 90_000L, 30_000L, 20_000L)
        val last = allocatedRefundPaisa(100_000L, 90_000L, 50_000L, 50_000L)
        assertEquals(90_000L, first + second + last)
    }
    @Test fun paisaRoundingAcrossInstallmentsIsStable() {
        val a = allocatedRefundPaisa(3L, 2L, 0L, 1L)
        val b = allocatedRefundPaisa(3L, 2L, 1L, 1L)
        val c = allocatedRefundPaisa(3L, 2L, 2L, 1L)
        assertEquals(2L, a + b + c)
    }
    @Test fun cannotRefundMoreThanInvoiceGross() {
        assertFailsWith<IllegalArgumentException> { allocatedRefundPaisa(100L, 90L, 80L, 30L) }
    }
}
