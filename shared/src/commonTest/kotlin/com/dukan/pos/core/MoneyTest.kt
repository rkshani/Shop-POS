package com.dukan.pos.core

import com.dukan.pos.core.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun repeated_addition_does_not_drift() {
        var total = Money.ZERO
        repeat(300) { total += Money.fromRupees(0.1) }
        assertEquals(Money.fromRupees(30.0), total)
    }

    @Test
    fun formats_with_separators_and_hides_empty_decimals() {
        assertEquals("Rs 1,250", Money.fromRupees(1250.0).format())
        assertEquals("Rs 1,250.50", Money.fromRupees(1250.5).format())
        assertEquals("-Rs 40", Money.fromRupees(-40.0).format())
    }

    @Test
    fun parses_lenient_user_input() {
        assertEquals(Money.fromRupees(1250.5), Money.parseOrNull("1,250.50"))
        assertEquals(Money.fromRupees(80.0), Money.parseOrNull(" Rs 80 "))
        assertNull(Money.parseOrNull("abc"))
        assertNull(Money.parseOrNull(""))
    }

    @Test
    fun multiplying_by_fractional_quantity_rounds_to_nearest_paisa() {
        assertEquals(Money(26250), Money.fromRupees(150.0) * 1.75)
    }
}
