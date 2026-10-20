package com.dukan.pos.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.data.local.DukanDatabase
import com.dukan.pos.data.local.entity.SaleEntity
import com.dukan.pos.data.local.entity.SaleItemEntity
import com.dukan.pos.data.repository.BackupRepositoryImpl
import com.dukan.pos.data.repository.ProductRepositoryImpl
import com.dukan.pos.domain.FixedClock
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.StockMovementType
import com.dukan.pos.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Runs against a real in-memory Room database, because the stock bugs this
 * guards against lived between the DAO and the repository, not in either alone.
 */
class ProductStockPersistenceTest {

    private lateinit var db: DukanDatabase
    private val clock = FixedClock()
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val main = Dispatchers.Unconfined
    }

    private val repository by lazy { ProductRepositoryImpl(db.productDao(), dispatchers, clock) }
    private val saveProduct by lazy { SaveProductUseCase(repository, clock) }

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<DukanDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun product(id: Long = 0L, stock: Double, price: Long = 300L) = Product(
        id = id,
        name = "Cheeni",
        salePrice = Money.fromRupees(price),
        costPrice = Money.fromRupees(250L),
        stockQty = stock,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private suspend fun stockOf(id: Long) = db.productDao().currentStock(id)

    @Test
    fun openingStockIsStoredOnce() = runTest {
        val id = assertIs<AppResult.Success<Long>>(saveProduct(product(stock = 10.0))).data

        assertEquals(10.0, stockOf(id))
        val movements = db.productDao().observeMovements(id).first()
        assertEquals(1, movements.size)
        assertEquals("OPENING", movements.single().type)
        assertEquals(10.0, movements.single().qtyAfter)
    }

    @Test
    fun editingStockCountsToTheNewValue() = runTest {
        val id = assertIs<AppResult.Success<Long>>(saveProduct(product(stock = 10.0))).data

        saveProduct(product(id = id, stock = 15.0), updateStock = true)

        assertEquals(15.0, stockOf(id))
        assertEquals(15.0, db.productDao().observeMovements(id).first().first().qtyAfter)
    }

    @Test
    fun priceEditKeepsSalesMadeWhileScreenWasOpen() = runTest {
        val id = assertIs<AppResult.Success<Long>>(saveProduct(product(stock = 10.0))).data
        // Edit screen opens showing 10. Meanwhile two bags are sold at the counter.
        db.saleDao().commitSale(
            sale = SaleEntity(
                invoiceNo = "",
                subTotalPaisa = 60_000,
                discountPaisa = 0,
                totalPaisa = 60_000,
                paidPaisa = 60_000,
                paymentMethod = "CASH",
                createdAtMillis = 1,
            ),
            items = listOf(
                SaleItemEntity(
                    saleId = 0, productId = id, productName = "Cheeni", unit = "PIECE",
                    qty = 2.0, unitPricePaisa = 30_000, unitCostPaisa = 25_000,
                ),
            ),
            invoicePrefix = "INV",
            allowNegativeStock = false,
        )
        assertEquals(8.0, stockOf(id))

        // Shopkeeper only changed the price; the stale "10" in the form is not a count.
        saveProduct(product(id = id, stock = 10.0, price = 320L), updateStock = false)

        assertEquals(8.0, stockOf(id))
        assertEquals(32_000L, db.productDao().findById(id)!!.salePaisa)
    }

    @Test
    fun adjustStockWritesChangeAndMovementTogether() = runTest {
        val id = assertIs<AppResult.Success<Long>>(saveProduct(product(stock = 5.0))).data

        repository.adjustStock(id, -1.5, StockMovementType.WASTAGE, "Phata hua bag")

        assertEquals(3.5, stockOf(id))
        val latest = db.productDao().observeMovements(id).first().first()
        assertEquals("WASTAGE", latest.type)
        assertEquals(3.5, latest.qtyAfter)
    }

    @Test
    fun freshShopBackupRestoresWithoutTouchingSettings() = runTest {
        val backup = BackupRepositoryImpl(db.backupDao(), dispatchers)
        saveProduct(product(stock = 4.0))

        val json = backup.exportJson()
        backup.restoreJson(json)

        assertEquals(1, db.settingsDao().get()?.id)
        assertEquals(4.0, db.productDao().findById(1)?.stockQty)
    }
}
