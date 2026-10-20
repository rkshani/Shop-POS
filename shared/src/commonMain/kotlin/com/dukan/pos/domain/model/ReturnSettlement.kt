package com.dukan.pos.domain.model

/** Conservative invoice-level cash refund cap.
 *
 * Only the money recorded as paid on the ORIGINAL invoice is treated as
 * refundable cash. Later khata collections cannot be attributed to this
 * invoice with the current ledger schema. Never infer additional cash received.
 * KHATA returns are separately checked against the LIVE customer balance.
 */
fun validateCashRefund(
    invoicePaidPaisa: Long,
    cashAlreadyRefundedPaisa: Long,
    requestedCashRefundPaisa: Long,
) {
    require(invoicePaidPaisa >= 0L && cashAlreadyRefundedPaisa >= 0L &&
        requestedCashRefundPaisa >= 0L) { "Negative refund settlement amount" }
    require(cashAlreadyRefundedPaisa <= invoicePaidPaisa) {
        "Existing cash refunds exceed recorded invoice payment"
    }
    require(requestedCashRefundPaisa <= invoicePaidPaisa - cashAlreadyRefundedPaisa) {
        "Cash refund exceeds money recorded as paid on this invoice; choose khata adjustment where applicable"
    }
}
