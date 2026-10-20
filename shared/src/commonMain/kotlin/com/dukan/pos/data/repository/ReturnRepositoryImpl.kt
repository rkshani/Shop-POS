package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.data.local.dao.ReturnDao
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.repository.ReturnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReturnRepositoryImpl(
    private val dao: ReturnDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ReturnRepository {

    override suspend fun recordReturn(saleReturn: SaleReturn): String = withContext(dispatchers.io) {
        val entity = saleReturn.toEntity().copy(createdAtMillis = clock.now().toEpochMilliseconds())
        val (_, reference) = dao.commitReturn(
            entity = entity,
            items = saleReturn.items.map { it.toEntity(returnId = 0L) },
        )
        reference
    }

    override fun observeReturnsForSale(saleId: Long): Flow<List<SaleReturn>> =
        dao.observeReturnsForSale(saleId)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)
}
