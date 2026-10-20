package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.model.Supplier
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    /** Stock increase, optional cost-price update and movements, all in one transaction. */
    suspend fun recordPurchase(purchase: Purchase): String
    fun observeRecent(limit: Int = 30): Flow<List<Purchase>>
    fun observeSuppliers(): Flow<List<Supplier>>
}
