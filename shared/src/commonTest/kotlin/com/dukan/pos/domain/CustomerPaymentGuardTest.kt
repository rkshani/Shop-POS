package com.dukan.pos.domain

import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.LedgerEntry
import com.dukan.pos.domain.repository.CustomerRepository
import com.dukan.pos.domain.usecase.RecordCustomerPaymentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class PaymentGuardFake(private val balance: Long) : CustomerRepository {
    var writes = 0
    private val customer = Customer(id = 1L, name = "Customer", balance = Money(balance), createdAt = Instant.fromEpochMilliseconds(0))
    override fun observeCustomers(query: String, onlyWithBalance: Boolean): Flow<List<Customer>> = flowOf(listOf(customer))
    override fun observeCustomer(id: Long): Flow<Customer?> = flowOf(customer)
    override fun observeLedger(customerId: Long): Flow<List<LedgerEntry>> = flowOf(emptyList())
    override fun observeTotalOutstanding(): Flow<Money> = flowOf(customer.balance)
    override suspend fun findById(id: Long): Customer? = if (id == 1L) customer else null
    override suspend fun upsert(customer: Customer): Long = customer.id
    override suspend fun recordPayment(customerId: Long, amount: Money, note: String?) { writes++ }
    override suspend fun recordAdjustment(customerId: Long, signedAmount: Money, note: String?) = Unit
}

class CustomerPaymentGuardTest {
    @Test fun overpayment_rejected_by_default() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Failure>(RecordCustomerPaymentUseCase(repo)(1L, Money(501), null))
        assertEquals(0, repo.writes)
    }

    @Test fun legacy_allowOverpayment_flag_does_not_bypass_policy() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Failure>(RecordCustomerPaymentUseCase(repo)(1L, Money(501), null, allowOverpayment = true))
        assertEquals(0, repo.writes)
    }

    @Test fun zero_payment_rejected_without_write() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Failure>(RecordCustomerPaymentUseCase(repo)(1L, Money(0), null))
        assertEquals(0, repo.writes)
    }

    @Test fun negative_payment_rejected_without_write() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Failure>(RecordCustomerPaymentUseCase(repo)(1L, Money(-1), null))
        assertEquals(0, repo.writes)
    }

    @Test fun exact_balance_payment_allowed() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Success<Unit>>(RecordCustomerPaymentUseCase(repo)(1L, Money(500), null))
        assertEquals(1, repo.writes)
    }

    @Test fun missing_customer_rejected_without_write() = runTest {
        val repo = PaymentGuardFake(500)
        assertIs<AppResult.Failure>(RecordCustomerPaymentUseCase(repo)(777L, Money(100), null))
        assertEquals(0, repo.writes)
    }
}
