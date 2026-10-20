package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.data.local.dao.PurchaseDao
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.model.Supplier
import com.dukan.pos.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PurchaseRepositoryImpl(
    private val dao: PurchaseDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : PurchaseRepository {

    override suspend fun recordPurchase(purchase: Purchase): String = withContext(dispatchers.io) {
        val entity = purchase.toEntity().copy(createdAtMillis = clock.now().toEpochMilliseconds())
        val (_, reference) = dao.commitPurchase(
            purchase = entity,
            items = purchase.items.map { it.toEntity(purchaseId = 0L) },
            supplierName = purchase.supplierName,
        )
        reference
    }

    override fun observeRecent(limit: Int): Flow<List<Purchase>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)

    override fun observeSuppliers(): Flow<List<Supplier>> =
        dao.observeSuppliers().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatchers.io)
}
