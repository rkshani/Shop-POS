package com.dukan.pos.core.common

/**
 * Thrown from inside a database transaction so the whole sale rolls back.
 * The stock check has to happen here, not in the use case: between reading
 * stock on the POS screen and committing the sale, another till (or the same
 * shop on a tablet) can have sold the last bag.
 */
class StockConflictException(
    val productName: String,
    val available: Double,
) : Exception("Not enough stock for $productName (available $available)")

class ReturnConflictException(
    val productName: String,
    val returnable: Double,
) : Exception("Only $returnable of $productName can still be returned")
