package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.data.local.dao.ProductDao
import com.dukan.pos.data.local.entity.StockMovementEntity
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.Category
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.StockMovement
import com.dukan.pos.domain.model.StockMovementType
import com.dukan.pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val dao: ProductDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ProductRepository {

    override fun observeProducts(query: String, categoryId: Long?): Flow<List<Product>> =
        dao.observeProducts(query.trim(), categoryId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeLowStock(): Flow<List<Product>> =
        dao.observeLowStock().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)

    override fun observeProduct(id: Long): Flow<Product?> =
        dao.observeProduct(id).map { it?.toDomain() }.flowOn(dispatchers.io)

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)

    override suspend fun findByBarcode(barcode: String): Product? = withContext(dispatchers.io) {
        dao.findByBarcode(barcode)?.toDomain()
    }

    override suspend fun findById(id: Long): Product? = withContext(dispatchers.io) {
        dao.findById(id)?.toDomain()
    }

    override suspend fun isBarcodeTaken(barcode: String, excludingId: Long?): Boolean =
        withContext(dispatchers.io) { dao.countWithBarcode(barcode, excludingId) > 0 }

    override suspend fun upsert(product: Product): Long = withContext(dispatchers.io) {
        val rowId = dao.upsert(product.toEntity())
        // Upsert returns -1 for an update; fall back to the id we already had.
        if (rowId > 0) rowId else product.id
    }

    override suspend fun setActive(id: Long, active: Boolean) = withContext(dispatchers.io) {
        dao.setActive(id, active, clock.now().toEpochMilliseconds())
    }

    override suspend fun upsertCategory(category: Category): Long = withContext(dispatchers.io) {
        val rowId = dao.upsertCategory(category.toEntity())
        if (rowId > 0) rowId else category.id
    }

    override suspend fun adjustStock(
        productId: Long,
        qtyChange: Double,
        type: StockMovementType,
        note: String?,
    ) = withContext(dispatchers.io) {
        val now = clock.now().toEpochMilliseconds()
        dao.applyStockDelta(productId, qtyChange, now)
        val after = dao.currentStock(productId) ?: 0.0
        dao.insertMovement(
            StockMovementEntity(
                productId = productId,
                type = type.name,
                qtyChange = qtyChange,
                qtyAfter = after,
                note = note,
                createdAtMillis = now,
            ),
        )
    }

    override fun observeMovements(productId: Long): Flow<List<StockMovement>> =
        dao.observeMovements(productId).map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)
}
