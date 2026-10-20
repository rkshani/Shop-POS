package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.data.local.dao.CustomerDao
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.LedgerEntry
import com.dukan.pos.domain.model.LedgerEntryType
import com.dukan.pos.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomerRepositoryImpl(
    private val dao: CustomerDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : CustomerRepository {

    override fun observeCustomers(query: String, onlyWithBalance: Boolean): Flow<List<Customer>> =
        dao.observeCustomers(query.trim(), onlyWithBalance)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeCustomer(id: Long): Flow<Customer?> =
        dao.observeCustomer(id).map { it?.toDomain() }.flowOn(dispatchers.io)

    override fun observeLedger(customerId: Long): Flow<List<LedgerEntry>> =
        dao.observeLedger(customerId).map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)

    override fun observeTotalOutstanding(): Flow<Money> =
        dao.observeTotalOutstanding().map { Money(it) }.flowOn(dispatchers.io)

    override suspend fun findById(id: Long): Customer? = withContext(dispatchers.io) {
        dao.findById(id)?.toDomain()
    }

    override suspend fun upsert(customer: Customer): Long = withContext(dispatchers.io) {
        val rowId = dao.upsert(customer.toEntity())
        if (rowId > 0) rowId else customer.id
    }

    override suspend fun recordPayment(customerId: Long, amount: Money, note: String?) =
        withContext(dispatchers.io) {
            require(amount.paisa > 0L) { "Payment must be positive" }
            dao.applyLedgerDelta(
                customerId = customerId,
                signedAmountPaisa = -amount.paisa,
                type = LedgerEntryType.PAYMENT.name,
                note = note,
                saleId = null,
                createdAtMillis = clock.now().toEpochMilliseconds(),
            )
        }

    override suspend fun recordAdjustment(customerId: Long, signedAmount: Money, note: String?) =
        withContext(dispatchers.io) {
            dao.applyLedgerDelta(
                customerId = customerId,
                signedAmountPaisa = signedAmount.paisa,
                type = LedgerEntryType.ADJUSTMENT.name,
                note = note,
                saleId = null,
                createdAtMillis = clock.now().toEpochMilliseconds(),
            )
        }
}
