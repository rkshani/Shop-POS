package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.Sale
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

data class RecordedSale(val id: Long, val invoiceNo: String)

interface SaleRepository {
    /**
     * Persists the sale, decrements stock, writes stock movements and — for
     * udhaar — the customer ledger entry, all inside a single transaction.
     * The invoice number is allocated inside that transaction too, so it is
     * never guessed from a row count.
     *
     * Throws [com.dukan.pos.core.common.StockConflictException] when stock ran
     * out between the screen and the commit.
     */
    suspend fun recordSale(
        sale: Sale,
        invoicePrefix: String,
        allowNegativeStock: Boolean,
    ): RecordedSale

    fun observeSales(from: Instant, until: Instant): Flow<List<Sale>>
    fun observeRecentSales(limit: Int = 20): Flow<List<Sale>>
    fun observeSale(id: Long): Flow<Sale?>
    suspend fun getSale(id: Long): Sale?
}
