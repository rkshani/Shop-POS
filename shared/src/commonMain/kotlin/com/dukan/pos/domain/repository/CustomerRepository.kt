package com.dukan.pos.domain.repository

import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeCustomers(query: String = "", onlyWithBalance: Boolean = false): Flow<List<Customer>>
    fun observeCustomer(id: Long): Flow<Customer?>
    fun observeLedger(customerId: Long): Flow<List<LedgerEntry>>
    fun observeTotalOutstanding(): Flow<Money>

    suspend fun findById(id: Long): Customer?
    suspend fun upsert(customer: Customer): Long
    suspend fun recordPayment(customerId: Long, amount: Money, note: String?)
    suspend fun recordAdjustment(customerId: Long, signedAmount: Money, note: String?)
}
