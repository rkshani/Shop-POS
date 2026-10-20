package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dukan.pos.data.local.entity.CounterEntity
import com.dukan.pos.data.local.entity.PurchaseEntity
import com.dukan.pos.data.local.entity.PurchaseItemEntity
import com.dukan.pos.data.local.entity.PurchaseWithItems
import com.dukan.pos.data.local.entity.StockMovementEntity
import com.dukan.pos.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

/**
 * Stock only ever goes up here. Cost price updates ride along in the same
 * transaction so a bill can never half-apply: new stock at the old cost would
 * quietly corrupt every profit figure after it.
 */
@Dao
abstract class PurchaseDao {

    @Transaction
    @Query("SELECT * FROM purchases ORDER BY createdAtMillis DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<PurchaseWithItems>>

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :id")
    abstract suspend fun getPurchase(id: Long): PurchaseWithItems?

    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE name = :name LIMIT 1")
    abstract suspend fun findSupplierByName(name: String): SupplierEntity?

    @Upsert
    abstract suspend fun upsertSupplier(supplier: SupplierEntity): Long

    @Insert
    abstract suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Insert
    abstract suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("SELECT stockQty FROM products WHERE id = :productId")
    abstract suspend fun stockOf(productId: Long): Double?

    @Query("UPDATE products SET stockQty = stockQty + :qty, updatedAtMillis = :updatedAtMillis WHERE id = :productId")
    abstract suspend fun incrementStock(productId: Long, qty: Double, updatedAtMillis: Long)

    @Query("UPDATE products SET costPaisa = :costPaisa, updatedAtMillis = :updatedAtMillis WHERE id = :productId")
    abstract suspend fun updateCost(productId: Long, costPaisa: Long, updatedAtMillis: Long)

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
    open suspend fun commitPurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        supplierName: String?,
    ): Pair<Long, String> {
        require(items.isNotEmpty()) { "Purchase must contain at least one line" }
        require(items.all { it.qty.isFinite() && it.qty > 0.0 && it.productId > 0L &&
            it.unitCostPaisa >= 0L }) { "Invalid purchase item" }
        for (productId in items.map { it.productId }.distinct()) {
            require(stockOf(productId) != null) { "Purchase product does not exist: $productId" }
        }
        val supplierId = supplierName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            findSupplierByName(name)?.id
                ?: upsertSupplier(
                    SupplierEntity(name = name, createdAtMillis = purchase.createdAtMillis),
                )
        }

        bumpCounter(CounterEntity.PURCHASE)
        val next = counterValue(CounterEntity.PURCHASE) ?: 1L
        val reference = "PUR-${next.toString().padStart(5, '0')}"

        val purchaseId = insertPurchase(
            purchase.copy(
                reference = reference,
                supplierId = supplierId,
                supplierName = supplierName?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        insertItems(items.map { it.copy(purchaseId = purchaseId) })

        val movements = items.map { item ->
            val before = stockOf(item.productId) ?: 0.0
            incrementStock(item.productId, item.qty, purchase.createdAtMillis)
            if (item.updatesCostPrice) {
                updateCost(item.productId, item.unitCostPaisa, purchase.createdAtMillis)
            }
            StockMovementEntity(
                productId = item.productId,
                type = "PURCHASE",
                qtyChange = item.qty,
                qtyAfter = before + item.qty,
                note = reference,
                createdAtMillis = purchase.createdAtMillis,
            )
        }
        insertMovements(movements)
        return purchaseId to reference
    }
}
