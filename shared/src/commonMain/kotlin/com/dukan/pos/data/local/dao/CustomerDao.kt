package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.dukan.pos.data.local.entity.CustomerEntity
import com.dukan.pos.data.local.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CustomerDao {

    @Query(
        """
        SELECT * FROM customers
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
          AND (:onlyWithBalance = 0 OR balancePaisa > 0)
        ORDER BY balancePaisa DESC, name COLLATE NOCASE ASC
        """,
    )
    abstract fun observeCustomers(query: String, onlyWithBalance: Boolean): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    abstract fun observeCustomer(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    abstract suspend fun findById(id: Long): CustomerEntity?

    @Query("SELECT CAST(COALESCE(SUM(balancePaisa), 0) AS INTEGER) FROM customers WHERE balancePaisa > 0")
    abstract fun observeTotalOutstanding(): Flow<Long>

    @Query("SELECT * FROM ledger_entries WHERE customerId = :customerId ORDER BY createdAtMillis DESC, id DESC")
    abstract fun observeLedger(customerId: Long): Flow<List<LedgerEntryEntity>>

    @Upsert
    abstract suspend fun upsert(customer: CustomerEntity): Long

    @Insert
    abstract suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Query("UPDATE customers SET balancePaisa = :balancePaisa WHERE id = :id")
    abstract suspend fun setBalance(id: Long, balancePaisa: Long)

    /**
     * Balance and ledger must move together or the khata stops adding up,
     * so both writes live in one transaction.
     */
    @Transaction
    open suspend fun applyLedgerDelta(
        customerId: Long,
        signedAmountPaisa: Long,
        type: String,
        note: String?,
        saleId: Long?,
        createdAtMillis: Long,
    ) {
        val customer = findById(customerId)
            ?: throw IllegalArgumentException("Customer $customerId not found")
        require(type == "SALE" || type == "PAYMENT" || type == "ADJUSTMENT" || type == "OPENING_BALANCE") {
            "Unsupported ledger entry type"
        }
        if (type == "PAYMENT") {
            require(signedAmountPaisa < 0L) { "Payment amount must be positive" }
            require(customer.balancePaisa > 0L && signedAmountPaisa >= -customer.balancePaisa) {
                "Payment exceeds outstanding khata balance"
            }
        }
        require(type != "ADJUSTMENT" || signedAmountPaisa != 0L) { "Zero adjustment is not permitted" }
        val newBalance = safeLedgerBalance(customer.balancePaisa, signedAmountPaisa)
        require(newBalance >= 0L) { "Negative khata balance requires an explicit customer-credit policy" }
        setBalance(customerId, newBalance)
        insertLedgerEntry(
            LedgerEntryEntity(
                customerId = customerId,
                type = type,
                amountPaisa = signedAmountPaisa,
                balanceAfterPaisa = newBalance,
                saleId = saleId,
                note = note,
                createdAtMillis = createdAtMillis,
            ),
        )
    }
}

