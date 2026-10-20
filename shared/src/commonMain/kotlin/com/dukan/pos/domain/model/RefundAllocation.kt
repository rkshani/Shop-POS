package com.dukan.pos.domain.model

import kotlin.math.roundToLong

/**
 * Allocate the actual invoice total across cumulative gross returned value.
 * Cumulative rounding ensures several partial returns add up to the exact
 * invoice total when every sold unit has been returned.
 * Caller must validate quantities and invoice monetary invariants first.
 */
fun allocatedRefundPaisa(
    invoiceSubtotalPaisa: Long,
    invoiceTotalPaisa: Long,
    grossPreviouslyReturnedPaisa: Long,
    grossNewlyReturnedPaisa: Long,
): Long {
    require(invoiceSubtotalPaisa > 0L && invoiceTotalPaisa in 0L..invoiceSubtotalPaisa)
    require(grossPreviouslyReturnedPaisa in 0L..invoiceSubtotalPaisa)
    require(grossNewlyReturnedPaisa >= 0L && grossNewlyReturnedPaisa <= invoiceSubtotalPaisa - grossPreviouslyReturnedPaisa)
    fun allocation(gross: Long): Long =
        (invoiceTotalPaisa.toDouble() * gross.toDouble() / invoiceSubtotalPaisa.toDouble()).roundToLong()
    return allocation(grossPreviouslyReturnedPaisa + grossNewlyReturnedPaisa) - allocation(grossPreviouslyReturnedPaisa)
}
