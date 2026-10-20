package com.dukan.pos.domain

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.StockConflictException
import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.PaymentMethod
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.RecordedSale
import com.dukan.pos.domain.repository.SaleRepository
import com.dukan.pos.domain.repository.SettingsRepository
import com.dukan.pos.domain.usecase.CheckoutUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSaleRepository(
    private val failWith: Exception? = null,
) : SaleRepository {
    var recorded: Sale? = null
    var seenPrefix: String? = null
    var seenAllowNegative: Boolean? = null

    override suspend fun recordSale(
        sale: Sale,
        invoicePrefix: String,
        allowNegativeStock: Boolean,
    ): RecordedSale {
        failWith?.let { throw it }
        recorded = sale
        seenPrefix = invoicePrefix
        seenAllowNegative = allowNegativeStock
        return RecordedSale(id = 1L, invoiceNo = "$invoicePrefix-00001")
    }

    override fun observeSales(from: Instant, until: Instant): Flow<List<Sale>> = flowOf(emptyList())
    override fun observeRecentSales(limit: Int): Flow<List<Sale>> = flowOf(emptyList())
    override fun observeSale(id: Long): Flow<Sale?> = flowOf(null)
    override suspend fun getSale(id: Long): Sale? = null
}

private class FakeSettingsRepository(private var settings: ShopSettings) : SettingsRepository {
    override fun observeSettings(): Flow<ShopSettings> = flowOf(settings)
    override suspend fun get(): ShopSettings = settings
    override suspend fun update(settings: ShopSettings) { this.settings = settings }
}

class FixedClock : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)
    override fun timeZone(): TimeZone = TimeZone.UTC
}

class CheckoutUseCaseTest {

    private fun product(stock: Double = 10.0) = Product(
        id = 1,
        name = "Chawal 1kg",
        unit = ProductUnit.KILOGRAM,
        costPrice = Money.fromRupees(220.0),
        salePrice = Money.fromRupees(260.0),
        stockQty = stock,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun useCase(
        sales: FakeSaleRepository = FakeSaleRepository(),
        settings: ShopSettings = ShopSettings(),
    ): Pair<FakeSaleRepository, CheckoutUseCase> =
        sales to CheckoutUseCase(sales, FakeSettingsRepository(settings), FixedClock())

    @Test
    fun empty_cart_cannot_be_charged() = runTest {
        val (_, checkout) = useCase()
        val result = checkout(Cart(), Money.ZERO)
        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.EmptyCart, result.error)
    }

    @Test
    fun selling_more_than_stock_is_blocked_unless_allowed() = runTest {
        val cart = Cart().upsert(product(stock = 2.0), 3.0)

        val (_, strict) = useCase()
        assertIs<AppResult.Failure>(strict(cart, Money.fromRupees(780.0)))

        val (_, lenient) = useCase(settings = ShopSettings(allowNegativeStock = true))
        assertIs<AppResult.Success<RecordedSale>>(lenient(cart, Money.fromRupees(780.0)))
    }

    @Test
    fun partial_payment_without_a_customer_is_refused() = runTest {
        val cart = Cart().upsert(product(), 1.0)
        val (_, checkout) = useCase()
        val result = checkout(cart, Money.fromRupees(100.0))
        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.CustomerRequiredForUdhaar, result.error)
    }

    @Test
    fun overpayment_is_not_recorded_as_revenue() = runTest {
        val cart = Cart().upsert(product(), 1.0)
        val (sales, checkout) = useCase()

        checkout(cart, Money.fromRupees(500.0))

        val recorded = requireNotNull(sales.recorded)
        assertEquals(Money.fromRupees(260.0), recorded.paid)
        assertTrue(recorded.isFullyPaid)
    }

    @Test
    fun udhaar_sale_snapshots_cost_and_leaves_balance_due() = runTest {
        val customer = Customer(id = 7, name = "Akram", createdAt = Instant.fromEpochMilliseconds(0))
        val cart = Cart(customer = customer, paymentMethod = PaymentMethod.UDHAAR)
            .upsert(product(), 2.0)

        val (sales, checkout) = useCase()
        val result = checkout(cart, Money.ZERO)

        assertIs<AppResult.Success<RecordedSale>>(result)
        val recorded = requireNotNull(sales.recorded)
        assertEquals(Money.fromRupees(520.0), recorded.due)
        assertEquals(Money.fromRupees(220.0), recorded.items.single().unitCost)
        assertEquals(Money.fromRupees(80.0), recorded.profit)
        assertEquals(7L, recorded.customerId)
    }

    @Test
    fun a_stock_race_inside_the_transaction_surfaces_as_not_enough_stock() = runTest {
        val cart = Cart().upsert(product(), 1.0)
        val (_, checkout) = useCase(
            sales = FakeSaleRepository(failWith = StockConflictException("Chawal 1kg", 0.0)),
        )

        val result = checkout(cart, Money.fromRupees(260.0))

        assertIs<AppResult.Failure>(result)
        assertIs<AppError.NotEnoughStock>(result.error)
    }

    @Test
    fun the_invoice_prefix_and_stock_rule_reach_the_repository() = runTest {
        val cart = Cart().upsert(product(), 1.0)
        val sales = FakeSaleRepository()
        val (_, checkout) = useCase(sales, ShopSettings(invoicePrefix = "BILL"))

        val result = checkout(cart, Money.fromRupees(260.0))

        assertIs<AppResult.Success<RecordedSale>>(result)
        assertEquals("BILL", sales.seenPrefix)
        assertEquals("BILL-00001", result.data.invoiceNo)
        assertEquals(false, sales.seenAllowNegative)
    }
}
