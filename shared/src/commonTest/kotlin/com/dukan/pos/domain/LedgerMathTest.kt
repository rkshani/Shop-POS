package com.dukan.pos.domain

import com.dukan.pos.data.local.dao.safeLedgerBalance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LedgerMathTest {
    @Test fun adds_sale_due_without_losing_paisa() {
        assertEquals(750L, safeLedgerBalance(500L, 250L))
    }

    @Test fun accepts_exact_payment() {
        assertEquals(0L, safeLedgerBalance(500L, -500L))
    }

    @Test fun rejects_positive_overflow() {
        assertFailsWith<IllegalArgumentException> { safeLedgerBalance(Long.MAX_VALUE, 1L) }
    }

    @Test fun rejects_negative_underflow() {
        assertFailsWith<IllegalArgumentException> { safeLedgerBalance(Long.MIN_VALUE, -1L) }
    }
}
