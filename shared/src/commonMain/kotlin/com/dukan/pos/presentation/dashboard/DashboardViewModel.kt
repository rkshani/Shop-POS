package com.dukan.pos.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.domain.model.DashboardSnapshot
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.SettingsRepository
import com.dukan.pos.domain.usecase.ObserveDashboardUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val snapshot: DashboardSnapshot = DashboardSnapshot(),
    val settings: ShopSettings = ShopSettings(),
    val isLoading: Boolean = true,
)

class DashboardViewModel(
    observeDashboard: ObserveDashboardUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> =
        combine(observeDashboard(), settingsRepository.observeSettings()) { snapshot, settings ->
            DashboardUiState(snapshot = snapshot, settings = settings, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )
}
