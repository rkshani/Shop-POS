package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.Category
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.StockMovement
import com.dukan.pos.domain.model.StockMovementType
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(query: String = "", categoryId: Long? = null): Flow<List<Product>>
    fun observeLowStock(): Flow<List<Product>>
    fun observeProduct(id: Long): Flow<Product?>
    fun observeCategories(): Flow<List<Category>>

    suspend fun findByBarcode(barcode: String): Product?
    suspend fun findById(id: Long): Product?
    suspend fun isBarcodeTaken(barcode: String, excludingId: Long?): Boolean

    /**
     * Creates or edits [product]'s catalogue fields. Its `stockQty` is ignored:
     * when [stockTarget] is non-null, stock is counted to that value through a
     * movement in the same transaction; null leaves stock untouched.
     */
    suspend fun save(product: Product, stockTarget: Double?, note: String?): Long
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun upsertCategory(category: Category): Long

    suspend fun adjustStock(
        productId: Long,
        qtyChange: Double,
        type: StockMovementType,
        note: String? = null,
    )

    fun observeMovements(productId: Long): Flow<List<StockMovement>>
}
