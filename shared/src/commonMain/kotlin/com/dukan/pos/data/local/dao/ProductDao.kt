package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.dukan.pos.data.local.entity.CategoryEntity
import com.dukan.pos.data.local.entity.ProductEntity
import com.dukan.pos.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

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
    fun observeProducts(query: String, categoryId: Long?): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM products
        WHERE isActive = 1 AND lowStockAt > 0 AND stockQty <= lowStockAt
        ORDER BY (stockQty - lowStockAt) ASC
        """,
    )
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products WHERE barcode = :barcode AND (:excludingId IS NULL OR id != :excludingId)")
    suspend fun countWithBarcode(barcode: String, excludingId: Long?): Int

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND lowStockAt > 0 AND stockQty <= lowStockAt AND stockQty > 0")
    fun observeLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND stockQty <= 0")
    fun observeOutOfStockCount(): Flow<Int>

    @Query("SELECT CAST(COALESCE(SUM(costPaisa * stockQty), 0) AS INTEGER) FROM products WHERE isActive = 1")
    fun observeStockValueAtCost(): Flow<Long>

    @Upsert
    suspend fun upsert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET isActive = :active, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, updatedAtMillis: Long)

    @Query("UPDATE products SET stockQty = stockQty + :delta, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun applyStockDelta(id: Long, delta: Double, updatedAtMillis: Long)

    @Query("SELECT stockQty FROM products WHERE id = :id")
    suspend fun currentStock(id: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeMovements(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity): Long
}
