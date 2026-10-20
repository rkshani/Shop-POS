package com.dukan.pos.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.toLocalDate
import com.dukan.pos.data.local.DukanDatabase
import com.dukan.pos.data.local.entity.CustomerEntity
import com.dukan.pos.data.local.entity.ProductEntity
import com.dukan.pos.data.repository.CustomerRepositoryImpl
import com.dukan.pos.data.repository.ReportRepositoryImpl
import com.dukan.pos.data.repository.ReturnRepositoryImpl
import com.dukan.pos.data.repository.SaleRepositoryImpl
import com.dukan.pos.domain.FixedClock
import com.dukan.pos.domain.model.PaymentMethod
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.model.SaleItem
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.model.SaleReturnItem
import com.dukan.pos.domain.model.SalesSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Report figures computed by SQL against a real in-memory Room database. */
class ReportFiguresTest {

    private lateinit var db: DukanDatabase
    private val clock = FixedClock()
    private val dispatchers = object : AppDispatchers {
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val main = Dispatchers.Unconfined
    }

    private val sales by lazy { SaleRepositoryImpl(db.saleDao(), dispatchers) }
    private val returns by lazy { ReturnRepositoryImpl(db.returnDao(), dispatchers, clock) }
    private val customers by lazy { CustomerRepositoryImpl(db.customerDao(), dispatchers, clock) }
    private val reports by lazy {
        ReportRepositoryImpl(db.reportDao(), db.productDao(), db.customerDao(), dispatchers, clock)
    }

    /** Rs 100 sale price, Rs 80 cost. */
    private val productId = 1L

    @BeforeTest
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder<DukanDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        db.backupDao().putProducts(
            listOf(
                ProductEntity(
                    id = productId, name = "Ghee", unit = "PIECE", costPaisa = 8_000, salePaisa = 10_000,
                    stockQty = 50.0, lowStockAt = 0.0, isActive = true, updatedAtMillis = 0,
                ),
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun sell(
        qty: Double,
        discountPaisa: Long = 0,
        paidPaisa: Long? = null,
        method: PaymentMethod = PaymentMethod.CASH,
        customerId: Long? = null,
    ): Sale {
        val subTotal = Money((10_000 * qty).toLong())
        val total = subTotal - Money(discountPaisa)
        val recorded = sales.recordSale(
            sale = Sale(
                invoiceNo = "",
                customerId = customerId,
                items = listOf(
                    SaleItem(
                        productId = productId, productName = "Ghee", unit = ProductUnit.PIECE,
                        qty = qty, unitPrice = Money(10_000), unitCost = Money(8_000),
                    ),
                ),
                subTotal = subTotal,
                discount = Money(discountPaisa),
                total = total,
                paid = paidPaisa?.let(::Money) ?: total,
                paymentMethod = method,
                createdAt = clock.now(),
            ),
            invoicePrefix = "INV",
            allowNegativeStock = false,
        )
        return sales.getSale(recorded.id)!!
    }

    private suspend fun giveBack(sale: Sale, qty: Double, refundPaisa: Long, method: RefundMethod) {
        val line = sale.items.single()
        returns.recordReturn(
            SaleReturn(
                saleId = sale.id,
                customerId = sale.customerId,
                items = listOf(
                    SaleReturnItem(
                        saleItemId = line.id, productId = line.productId, productName = line.productName,
                        qty = qty, unitPrice = line.unitPrice, unitCost = line.unitCost,
                    ),
                ),
                refundMethod = method,
                allocatedRefund = Money(refundPaisa),
                createdAt = clock.now(),
            ),
        )
    }

    private suspend fun today(): SalesSummary {
        val date = clock.now().toLocalDate(clock.timeZone())
        return reports.observeSummary(date, date).first()
    }

    @Test
    fun fullyReturnedDiscountedSaleLeavesZeroProfit() = runTest {
        // Rs 100 item, Rs 10 discount → Rs 90 charged, Rs 10 profit.
        val sale = sell(qty = 1.0, discountPaisa = 1_000)
        assertEquals(Money(1_000), today().profit)

        giveBack(sale, qty = 1.0, refundPaisa = 9_000, method = RefundMethod.CASH)

        val summary = today()
        assertEquals(Money.ZERO, summary.profit)
        assertEquals(Money.ZERO, summary.grossSales)
        assertEquals(Money.ZERO, summary.cashCollected)
    }

    @Test
    fun cashCountsDrawerMoneyOnly() = runTest {
        val customerId = db.customerDao().upsert(CustomerEntity(name = "Aslam", createdAtMillis = 0))

        sell(qty = 1.0, method = PaymentMethod.CASH)                     // +100 cash
        sell(qty = 1.0, method = PaymentMethod.EASYPAISA)                // +100 digital
        val udhaar = sell(qty = 2.0, paidPaisa = 0, method = PaymentMethod.UDHAAR, customerId = customerId)
        customers.recordPayment(customerId, Money(4_000), note = null)   // +40 cash from khata
        giveBack(udhaar, qty = 1.0, refundPaisa = 10_000, method = RefundMethod.KHATA) // off the khata, not the drawer

        val summary = today()
        assertEquals(Money(14_000), summary.cashCollected)
        assertEquals(Money(10_000), summary.digitalCollected)
        assertEquals(Money(4_000), summary.khataCollected)
        assertEquals(Money(10_000), summary.returnedAmount)
    }

    @Test
    fun cashRefundLeavesTheDrawer() = runTest {
        val sale = sell(qty = 2.0, method = PaymentMethod.CASH)

        giveBack(sale, qty = 1.0, refundPaisa = 10_000, method = RefundMethod.CASH)

        assertEquals(Money(10_000), today().cashCollected)
    }
}
