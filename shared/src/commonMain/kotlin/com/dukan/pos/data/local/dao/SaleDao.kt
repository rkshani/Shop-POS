package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.dukan.pos.core.common.StockConflictException
import com.dukan.pos.data.local.entity.CounterEntity
import com.dukan.pos.data.local.entity.LedgerEntryEntity
import com.dukan.pos.data.local.entity.SaleEntity
import com.dukan.pos.data.local.entity.SaleItemEntity
import com.dukan.pos.data.local.entity.SaleWithItems
import com.dukan.pos.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Owns the entire write path of a sale. A sale touches five tables and every
 * one of them must land or none of them — hence a single transactional DAO
 * rather than a repository stitching separate calls together.
 */
@Dao
abstract class SaleDao {

    @Transaction
    @Query("SELECT * FROM sales WHERE createdAtMillis >= :from AND createdAtMillis < :until ORDER BY createdAtMillis DESC")
    abstract fun observeSales(from: Long, until: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAtMillis DESC LIMIT :limit")
    abstract fun observeRecentSales(limit: Int): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    abstract fun observeSale(id: Long): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    abstract suspend fun getSale(id: Long): SaleWithItems?

    @Insert
    abstract suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<SaleItemEntity>)

    @Insert
    abstract suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Insert
    abstract suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Query("UPDATE products SET stockQty = stockQty - :qty, updatedAtMillis = :updatedAtMillis WHERE id = :productId")
    abstract suspend fun decrementStock(productId: Long, qty: Double, updatedAtMillis: Long)

    @Query("SELECT stockQty FROM products WHERE id = :productId")
    abstract suspend fun stockOf(productId: Long): Double?

    @Query("SELECT balancePaisa FROM customers WHERE id = :customerId")
    abstract suspend fun customerBalance(customerId: Long): Long?

    @Query("UPDATE customers SET balancePaisa = :balancePaisa WHERE id = :customerId")
    abstract suspend fun setCustomerBalance(customerId: Long, balancePaisa: Long)

    // ---- Invoice numbering ----

    @Query(
        """
        INSERT INTO counters(name, value) VALUES (:name, 1)
        ON CONFLICT(name) DO UPDATE SET value = value + 1
        """,
    )
    abstract suspend fun bumpCounter(name: String)

    @Query("SELECT value FROM counters WHERE name = :name")
    abstract suspend fun counterValue(name: String): Long?

    /**
     * Counter-backed, not `COUNT(*) + 1`: deleting a sale must never let an
     * invoice number be handed out twice, and two tills must never collide.
     */
    protected suspend fun allocateReference(name: String, prefix: String): String {
        bumpCounter(name)
        val next = counterValue(name) ?: 1L
        return "$prefix-${next.toString().padStart(5, '0')}"
    }

    @Transaction
    open suspend fun commitSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        invoicePrefix: String,
        allowNegativeStock: Boolean,
    ): Pair<Long, String> {
        require(items.isNotEmpty()) { "Sale must contain at least one line" }
        require(sale.totalPaisa >= 0 && sale.paidPaisa >= 0 && sale.paidPaisa <= sale.totalPaisa) {
            "Invalid sale payment amounts"
        }
        require(sale.subTotalPaisa >= 0 && sale.discountPaisa >= 0 &&
            sale.totalPaisa == sale.subTotalPaisa - sale.discountPaisa) { "Invalid sale totals" }
        require(sale.totalPaisa == sale.paidPaisa || sale.customerId != null) {
            "Unpaid sale requires a customer"
        }
        if (sale.customerId != null) require(customerBalance(sale.customerId) != null) {
            "Sale customer does not exist"
        }
        require(items.all { it.qty.isFinite() && it.qty > 0.0 && it.productId > 0L &&
            it.unitPricePaisa >= 0 && it.unitCostPaisa >= 0 }) { "Invalid sale item" }
        // Aggregate quantities before querying stock: separate lines may refer to one product.
        val quantities = items.groupBy { it.productId }
            .mapValues { (_, lines) -> lines.sumOf { it.qty } }
        require(quantities.values.all { it.isFinite() }) { "Invalid aggregate quantity" }
        for ((productId, qty) in quantities) {
            val available = stockOf(productId)
                ?: throw IllegalArgumentException("Sale product does not exist: $productId")
            if (!allowNegativeStock && qty > available + 0.0000001) {
                throw StockConflictException(items.first { it.productId == productId }.productName, available)
            }
        }

        val invoiceNo = allocateReference(CounterEntity.INVOICE, invoicePrefix)
        val saleId = insertSale(sale.copy(invoiceNo = invoiceNo))
        insertItems(items.map { it.copy(saleId = saleId) })

        val movements = items.map { item ->
            val before = stockOf(item.productId)
                ?: throw IllegalArgumentException("Sale product no longer exists: ${item.productId}")
            require(before.isFinite() && (before - item.qty).isFinite()) { "Invalid stock value on sale" }
            decrementStock(item.productId, item.qty, sale.createdAtMillis)
            StockMovementEntity(
                productId = item.productId,
                type = "SALE",
                qtyChange = -item.qty,
                qtyAfter = before - item.qty,
                saleId = saleId,
                note = invoiceNo,
                createdAtMillis = sale.createdAtMillis,
            )
        }
        insertMovements(movements)

        val duePaisa = sale.totalPaisa - sale.paidPaisa
        if (duePaisa > 0 && sale.customerId != null) {
            val current = customerBalance(sale.customerId)
                ?: throw IllegalArgumentException("Sale customer no longer exists")
            val newBalance = safeLedgerBalance(current, duePaisa)
            setCustomerBalance(sale.customerId, newBalance)
            insertLedgerEntry(
                LedgerEntryEntity(
                    customerId = sale.customerId,
                    type = "SALE",
                    amountPaisa = duePaisa,
                    balanceAfterPaisa = newBalance,
                    saleId = saleId,
                    note = invoiceNo,
                    createdAtMillis = sale.createdAtMillis,
                ),
            )
        }
        return saleId to invoiceNo
    }
}
