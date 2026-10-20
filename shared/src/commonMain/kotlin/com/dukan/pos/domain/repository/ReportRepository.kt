package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.DailyPoint
import com.dukan.pos.domain.model.DashboardSnapshot
import com.dukan.pos.domain.model.SalesSummary
import com.dukan.pos.domain.model.TopProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ReportRepository {
    fun observeDashboard(): Flow<DashboardSnapshot>
    fun observeSummary(from: LocalDate, until: LocalDate): Flow<SalesSummary>
    fun observeDailySeries(from: LocalDate, until: LocalDate): Flow<List<DailyPoint>>
    fun observeTopProducts(from: LocalDate, until: LocalDate, limit: Int = 10): Flow<List<TopProduct>>
}
