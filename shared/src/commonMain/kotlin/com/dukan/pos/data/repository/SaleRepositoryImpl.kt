package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.data.local.dao.SaleDao
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.repository.RecordedSale
import com.dukan.pos.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SaleRepositoryImpl(
    private val dao: SaleDao,
    private val dispatchers: AppDispatchers,
) : SaleRepository {

    override suspend fun recordSale(
        sale: Sale,
        invoicePrefix: String,
        allowNegativeStock: Boolean,
    ): RecordedSale = withContext(dispatchers.io) {
        val (id, invoiceNo) = dao.commitSale(
            sale = sale.toEntity(),
            items = sale.items.map { it.toEntity(saleId = 0L) },
            invoicePrefix = invoicePrefix,
            allowNegativeStock = allowNegativeStock,
        )
        RecordedSale(id = id, invoiceNo = invoiceNo)
    }

    override fun observeSales(from: Instant, until: Instant): Flow<List<Sale>> =
        dao.observeSales(from.toEpochMilliseconds(), until.toEpochMilliseconds())
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeRecentSales(limit: Int): Flow<List<Sale>> =
        dao.observeRecentSales(limit)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeSale(id: Long): Flow<Sale?> =
        dao.observeSale(id).map { it?.toDomain() }.flowOn(dispatchers.io)

    override suspend fun getSale(id: Long): Sale? = withContext(dispatchers.io) {
        dao.getSale(id)?.toDomain()
    }
}
