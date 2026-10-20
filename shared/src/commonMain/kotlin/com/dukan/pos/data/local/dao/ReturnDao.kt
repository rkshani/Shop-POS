package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.dukan.pos.core.common.ReturnConflictException
import com.dukan.pos.domain.model.allocatedRefundPaisa
import com.dukan.pos.domain.model.validateCashRefund
import com.dukan.pos.data.local.entity.CounterEntity
import com.dukan.pos.data.local.entity.LedgerEntryEntity
import com.dukan.pos.data.local.entity.SaleItemEntity
import com.dukan.pos.data.local.entity.SaleEntity
import com.dukan.pos.data.local.entity.SaleReturnEntity
import com.dukan.pos.data.local.entity.SaleReturnItemEntity
import com.dukan.pos.data.local.entity.SaleReturnWithItems
import com.dukan.pos.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToLong

/**
 * A return is a sale run backwards: stock comes in, money goes out (or comes
 * off the khata), and the original sale line records how much has come back so
 * the same packet cannot be returned twice.
 */
@Dao
abstract class ReturnDao {

    @Transaction
    @Query("SELECT * FROM sale_returns WHERE saleId = :saleId ORDER BY createdAtMillis DESC")
    abstract fun observeReturnsForSale(saleId: Long): Flow<List<SaleReturnWithItems>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    abstract suspend fun originalSale(saleId: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    abstract suspend fun saleLines(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE id = :saleItemId")
    abstract suspend fun originalSaleItem(saleItemId: Long): SaleItemEntity?

    @Query("SELECT COALESCE(SUM(refundPaisa), 0) FROM sale_returns WHERE saleId = :saleId AND refundMethod = 'CASH'")
    abstract suspend fun cashRefundedForSale(saleId: Long): Long

    @Query("SELECT COALESCE(SUM(refundPaisa), 0) FROM sale_returns WHERE saleId = :saleId")
    abstract suspend fun refundedForSale(saleId: Long): Long

    @Query("SELECT returnedQty FROM sale_items WHERE id = :saleItemId")
    abstract suspend fun returnedQtyOf(saleItemId: Long): Double?

    @Query("SELECT qty FROM sale_items WHERE id = :saleItemId")
    abstract suspend fun soldQtyOf(saleItemId: Long): Double?

    @Query("UPDATE sale_items SET returnedQty = returnedQty + :qty WHERE id = :saleItemId")
    abstract suspend fun addReturnedQty(saleItemId: Long, qty: Double)

    @Query("SELECT stockQty FROM products WHERE id = :productId")
    abstract suspend fun stockOf(productId: Long): Double?

    @Query("UPDATE products SET stockQty = stockQty + :qty, updatedAtMillis = :updatedAtMillis WHERE id = :productId")
    abstract suspend fun incrementStock(productId: Long, qty: Double, updatedAtMillis: Long)

    @Query("SELECT balancePaisa FROM customers WHERE id = :customerId")
    abstract suspend fun customerBalance(customerId: Long): Long?

    @Query("UPDATE customers SET balancePaisa = :balancePaisa WHERE id = :customerId")
    abstract suspend fun setCustomerBalance(customerId: Long, balancePaisa: Long)

    @Insert
    abstract suspend fun insertReturn(entity: SaleReturnEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<SaleReturnItemEntity>)

    @Insert
    abstract suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Insert
    abstract suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Query(
        """
        INSERT INTO counters(name, value) VALUES (:name, 1)
        ON CONFLICT(name) DO UPDATE SET value = value + 1
        """,
    )
    abstract suspend fun bumpCounter(name: String)

    @Query("SELECT value FROM counters WHERE name = :name")
    abstract suspend fun counterValue(name: String): Long?

    @Transaction
    open suspend fun commitReturn(
        entity: SaleReturnEntity,
        items: List<SaleReturnItemEntity>,
    ): Pair<Long, String> {
        require(items.isNotEmpty()) { "Return must contain at least one line" }
        val sale = originalSale(entity.saleId) ?: throw IllegalArgumentException("Original sale missing")
        require(entity.refundMethod == "CASH" || entity.refundMethod == "KHATA") { "Invalid refund method" }
        require(entity.customerId == sale.customerId) { "Return customer mismatch" }
        require(entity.refundPaisa >= 0L && entity.refundPaisa <= sale.totalPaisa - refundedForSale(entity.saleId)) {
            "Refund exceeds remaining sale total"
        }
        require(items.map { it.saleItemId }.distinct().size == items.size) {
            "Duplicate sale lines in return"
        }
        items.forEach { item ->
            require(item.qty.isFinite() && item.qty > 0.0) { "Invalid return quantity" }
            val original = originalSaleItem(item.saleItemId)
                ?: throw IllegalArgumentException("Original sale line missing")
            require(original.saleId == entity.saleId && original.productId == item.productId &&
                original.unitPricePaisa == item.unitPricePaisa &&
                original.unitCostPaisa == item.unitCostPaisa) { "Return line does not match original sale" }
            val sold = soldQtyOf(item.saleItemId) ?: 0.0
            val alreadyReturned = returnedQtyOf(item.saleItemId) ?: 0.0
            val returnable = sold - alreadyReturned
            if (item.qty > returnable + 0.0001) {
                throw ReturnConflictException(item.productName, returnable)
            }
        }

        // Use the trusted sale-line snapshots, not prices or totals supplied by UI.
        val originalLines = saleLines(entity.saleId)
        require(originalLines.isNotEmpty() && originalLines.all { line ->
            line.qty.isFinite() && line.qty > 0.0 &&
                line.returnedQty.isFinite() && line.returnedQty >= 0.0 &&
                line.returnedQty <= line.qty + 0.0001 &&
                line.unitPricePaisa >= 0L && line.unitCostPaisa >= 0L
        }) { "Original sale contains corrupt item quantities or prices" }
        require(sale.subTotalPaisa > 0 && sale.totalPaisa in 0L..sale.subTotalPaisa) {
            "Original invoice totals are invalid"
        }
        val previouslyReturnedGross = originalLines.sumOf { line ->
            (line.returnedQty * line.unitPricePaisa).roundToLong()
        }
        val newlyReturnedGross = items.sumOf { line ->
            (line.qty * (originalLines.first { it.id == line.saleItemId }).unitPricePaisa).roundToLong()
        }
        val expectedRefund = allocatedRefundPaisa(
            sale.subTotalPaisa, sale.totalPaisa, previouslyReturnedGross, newlyReturnedGross,
        )
        require(entity.refundPaisa == expectedRefund) { "Refund amount does not match original items" }
        if (entity.refundMethod == "CASH") {
            validateCashRefund(sale.paidPaisa, cashRefundedForSale(entity.saleId), entity.refundPaisa)
        }
        if (entity.refundMethod == "KHATA") {
            val customerId = entity.customerId ?: throw IllegalArgumentException("Khata return requires a customer")
            val balance = customerBalance(customerId) ?: throw IllegalArgumentException("Customer missing")
            require(entity.refundPaisa <= balance) { "Refund exceeds outstanding khata; use cash refund policy" }
        }
        bumpCounter(CounterEntity.RETURN)
        val next = counterValue(CounterEntity.RETURN) ?: 1L
        val reference = "RET-${next.toString().padStart(5, '0')}"

        val returnId = insertReturn(entity.copy(reference = reference))
        insertItems(items.map { it.copy(returnId = returnId) })

        val movements = items.map { item ->
            addReturnedQty(item.saleItemId, item.qty)
            val before = stockOf(item.productId)
                ?: throw IllegalArgumentException("Returned product no longer exists: ${item.productId}")
            require(before.isFinite() && (before + item.qty).isFinite()) {
                "Invalid stock value on return"
            }
            incrementStock(item.productId, item.qty, entity.createdAtMillis)
            StockMovementEntity(
                productId = item.productId,
                type = "SALE_RETURN",
                qtyChange = item.qty,
                qtyAfter = before + item.qty,
                saleId = entity.saleId,
                note = reference,
                createdAtMillis = entity.createdAtMillis,
            )
        }
        insertMovements(movements)

        // Refunding to the khata reduces what the customer owes instead of
        // taking cash out of a till that may not have it.
        if (entity.refundMethod == "KHATA" && entity.customerId != null && entity.refundPaisa > 0) {
            val current = customerBalance(entity.customerId)
                ?: throw IllegalArgumentException("Return customer no longer exists")
            val newBalance = current - entity.refundPaisa
            setCustomerBalance(entity.customerId, newBalance)
            insertLedgerEntry(
                LedgerEntryEntity(
                    customerId = entity.customerId,
                    type = "ADJUSTMENT",
                    amountPaisa = -entity.refundPaisa,
                    balanceAfterPaisa = newBalance,
                    saleId = entity.saleId,
                    note = reference,
                    createdAtMillis = entity.createdAtMillis,
                ),
            )
        }
        return returnId to reference
    }
}
