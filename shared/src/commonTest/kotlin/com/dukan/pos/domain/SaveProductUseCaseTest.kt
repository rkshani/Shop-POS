package com.dukan.pos.domain

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Category
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.StockMovement
import com.dukan.pos.domain.model.StockMovementType
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class RecordingProductRepository(
    private val failWith: Exception? = null,
) : ProductRepository {
    var savedProduct: Product? = null
    var savedStockTarget: Double? = null
    var saveCalls = 0

    override suspend fun save(product: Product, stockTarget: Double?, note: String?): Long {
        failWith?.let { throw it }
        saveCalls++
        savedProduct = product
        savedStockTarget = stockTarget
        return if (product.id == 0L) 42L else product.id
    }

    override fun observeProducts(query: String, categoryId: Long?): Flow<List<Product>> = flowOf(emptyList())
    override fun observeLowStock(): Flow<List<Product>> = flowOf(emptyList())
    override fun observeProduct(id: Long): Flow<Product?> = flowOf(null)
    override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun findByBarcode(barcode: String): Product? = null
    override suspend fun findById(id: Long): Product? = null
    override suspend fun isBarcodeTaken(barcode: String, excludingId: Long?): Boolean = false
    override suspend fun setActive(id: Long, active: Boolean) = Unit
    override suspend fun upsertCategory(category: Category): Long = 0L
    override suspend fun adjustStock(productId: Long, qtyChange: Double, type: StockMovementType, note: String?) = Unit
    override fun observeMovements(productId: Long): Flow<List<StockMovement>> = flowOf(emptyList())
}

class SaveProductUseCaseTest {

    private fun product(id: Long = 0L, stock: Double = 10.0) = Product(
        id = id,
        name = "Daal Chana",
        salePrice = Money.fromRupees(300L),
        costPrice = Money.fromRupees(250L),
        stockQty = stock,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun newProductPassesOpeningStockAsTarget() = runTest {
        val repo = RecordingProductRepository()
        val result = SaveProductUseCase(repo, FixedClock())(product(stock = 10.0))

        assertEquals(AppResult.Success(42L), result)
        assertEquals(10.0, repo.savedStockTarget)
    }

    @Test
    fun editWithoutStockChangeLeavesStockAlone() = runTest {
        val repo = RecordingProductRepository()
        SaveProductUseCase(repo, FixedClock())(product(id = 7, stock = 10.0), updateStock = false)

        assertEquals(1, repo.saveCalls)
        assertNull(repo.savedStockTarget)
    }

    @Test
    fun editWithStockChangeSetsCount() = runTest {
        val repo = RecordingProductRepository()
        SaveProductUseCase(repo, FixedClock())(product(id = 7, stock = 15.0), updateStock = true)

        assertEquals(15.0, repo.savedStockTarget)
    }

    @Test
    fun nonFiniteStockRejectedBeforeSaving() = runTest {
        val repo = RecordingProductRepository()
        val result = SaveProductUseCase(repo, FixedClock())(product(stock = Double.NaN))

        assertIs<AppResult.Failure>(result)
        assertEquals(0, repo.saveCalls)
    }

    @Test
    fun databaseErrorBecomesFailureInsteadOfCrash() = runTest {
        val repo = RecordingProductRepository(failWith = IllegalStateException("UNIQUE constraint failed"))
        val result = SaveProductUseCase(repo, FixedClock())(product())

        val failure = assertIs<AppResult.Failure>(result)
        assertIs<AppError.Unexpected>(failure.error)
    }
}
