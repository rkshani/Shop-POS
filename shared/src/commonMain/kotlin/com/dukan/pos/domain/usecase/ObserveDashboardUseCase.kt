package com.dukan.pos.domain.usecase

import com.dukan.pos.domain.model.DashboardSnapshot
import com.dukan.pos.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow

class ObserveDashboardUseCase(
    private val repository: ReportRepository,
) {
    operator fun invoke(): Flow<DashboardSnapshot> = repository.observeDashboard()
}
