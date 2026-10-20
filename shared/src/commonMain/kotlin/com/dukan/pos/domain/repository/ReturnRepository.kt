package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.SaleReturn
import kotlinx.coroutines.flow.Flow

interface ReturnRepository {
    suspend fun recordReturn(saleReturn: SaleReturn): String
    fun observeReturnsForSale(saleId: Long): Flow<List<SaleReturn>>
}
