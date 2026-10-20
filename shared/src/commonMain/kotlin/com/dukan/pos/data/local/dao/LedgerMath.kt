package com.dukan.pos.data.local.dao

/** Checked paisa arithmetic. Never allow balance wraparound in persisted financial records. */
internal fun safeLedgerBalance(current: Long, delta: Long): Long {
    if (delta > 0L && current > Long.MAX_VALUE - delta) throw IllegalArgumentException("Ledger balance overflow")
    if (delta < 0L && current < Long.MIN_VALUE - delta) throw IllegalArgumentException("Ledger balance underflow")
    return current + delta
}
