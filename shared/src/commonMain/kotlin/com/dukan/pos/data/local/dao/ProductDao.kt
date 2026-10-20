package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dukan.pos.data.local.entity.CategoryEntity
import com.dukan.pos.data.local.entity.ProductEntity
import com.dukan.pos.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    /**
     * Two-stage match against the single indexed [ProductEntity.searchText]:
     * a prefix hit can use the index, the word-boundary hit is the fallback.
     * Deliberately not `LIKE '%q%'` on three separate columns — that was a full
     * table scan per keystroke, three times over.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1
          AND (:query = ''
               OR searchText LIKE :query || '%'
               OR searchText LIKE '% ' || :query || '%'
               OR searchText LIKE '%|' || :query || '%')
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY
          CASE WHEN searchText LIKE :query || '%' THEN 0 ELSE 1 END,
          name COLLATE NOCASE ASC
        LIMIT 200
        """,
    )
    abstract fun observeProducts(query: String, categoryId: Long?): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1 AND lowStockAt > 0 AND stockQty <= lowStockAt
        ORDER BY (stockQty - lowStockAt) ASC
        """,
    )
    abstract fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract fun observeProduct(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract suspend fun findById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    abstract suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products WHERE barcode = :barcode AND (:excludingId IS NULL OR id != :excludingId)")
    abstract suspend fun countWithBarcode(barcode: String, excludingId: Long?): Int

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND lowStockAt > 0 AND stockQty <= lowStockAt AND stockQty > 0")
    abstract fun observeLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND stockQty <= 0")
    abstract fun observeOutOfStockCount(): Flow<Int>

    @Query("SELECT CAST(COALESCE(SUM(costPaisa * stockQty), 0) AS INTEGER) FROM products WHERE isActive = 1")
    abstract fun observeStockValueAtCost(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(product: ProductEntity): Long

    /**
     * Catalogue fields only. `stockQty` is deliberately absent: stock moves
     * through [applyStockDelta] plus a movement row, never by overwriting the
     * column with a value the edit screen read minutes ago.
     */
    @Query(
        """
        UPDATE products SET
            name = :name,
            nameUrdu = :nameUrdu,
            barcode = :barcode,
            categoryId = :categoryId,
            unit = :unit,
            costPaisa = :costPaisa,
            salePaisa = :salePaisa,
            lowStockAt = :lowStockAt,
            isActive = :isActive,
            updatedAtMillis = :updatedAtMillis,
            searchText = :searchText
        WHERE id = :id
        """,
    )
    abstract suspend fun updateDetails(
        id: Long,
        name: String,
        nameUrdu: String?,
        barcode: String?,
        categoryId: Long?,
        unit: String,
        costPaisa: Long,
        salePaisa: Long,
        lowStockAt: Double,
        isActive: Boolean,
        updatedAtMillis: Long,
        searchText: String,
    ): Int

    @Query("UPDATE products SET isActive = :active, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    abstract suspend fun setActive(id: Long, active: Boolean, updatedAtMillis: Long)

    @Query("UPDATE products SET stockQty = stockQty + :delta, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    abstract suspend fun applyStockDelta(id: Long, delta: Double, updatedAtMillis: Long)

    @Query("SELECT stockQty FROM products WHERE id = :id")
    abstract suspend fun currentStock(id: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAtMillis DESC, id DESC LIMIT 200")
    abstract fun observeMovements(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Upsert
    abstract suspend fun upsertCategory(category: CategoryEntity): Long

    /**
     * Creates or edits a product in one transaction.
     *
     * A new row always starts at zero stock; its opening quantity arrives as an
     * OPENING movement. When [stockTarget] is set on an existing product, the
     * stock is counted to that value against what the database holds *now*, so
     * sales made while the edit screen was open are not wiped out.
     * [stockTarget] = null leaves stock untouched.
     */
    @Transaction
    open suspend fun saveProduct(
        product: ProductEntity,
        stockTarget: Double?,
        note: String?,
        nowMillis: Long,
    ): Long {
        require(stockTarget == null || stockTarget.isFinite()) { "Invalid stock quantity" }
        val isNew = product.id == 0L
        val id = if (isNew) {
            insert(product.copy(stockQty = 0.0))
        } else {
            val updated = updateDetails(
                id = product.id,
                name = product.name,
                nameUrdu = product.nameUrdu,
                barcode = product.barcode,
                categoryId = product.categoryId,
                unit = product.unit,
                costPaisa = product.costPaisa,
                salePaisa = product.salePaisa,
                lowStockAt = product.lowStockAt,
                isActive = product.isActive,
                updatedAtMillis = product.updatedAtMillis,
                searchText = product.searchText,
            )
            require(updated == 1) { "Product ${product.id} not found" }
            product.id
        }

        if (stockTarget != null) {
            val current = currentStock(id) ?: error("Product $id vanished mid-save")
            val delta = stockTarget - current
            if (delta != 0.0) {
                recordMovement(
                    productId = id,
                    delta = delta,
                    type = if (isNew) "OPENING" else "ADJUSTMENT",
                    note = note,
                    nowMillis = nowMillis,
                )
            }
        }
        return id
    }

    /** Stock change plus its audit row, together or not at all. */
    @Transaction
    open suspend fun adjustStock(
        productId: Long,
        delta: Double,
        type: String,
        note: String?,
        nowMillis: Long,
    ) {
        require(delta.isFinite()) { "Invalid stock change" }
        requireNotNull(currentStock(productId)) { "Product $productId not found" }
        recordMovement(productId, delta, type, note, nowMillis)
    }

    protected suspend fun recordMovement(
        productId: Long,
        delta: Double,
        type: String,
        note: String?,
        nowMillis: Long,
    ) {
        applyStockDelta(productId, delta, nowMillis)
        val after = currentStock(productId) ?: error("Product $productId vanished mid-movement")
        insertMovement(
            StockMovementEntity(
                productId = productId,
                type = type,
                qtyChange = delta,
                qtyAfter = after,
                note = note,
                createdAtMillis = nowMillis,
            ),
        )
    }
}
