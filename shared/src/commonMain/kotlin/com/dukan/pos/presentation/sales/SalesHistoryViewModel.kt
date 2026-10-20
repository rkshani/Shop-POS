package com.dukan.pos.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.repository.SaleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SalesHistoryViewModel(
    repository: SaleRepository,
) : ViewModel() {

    val sales: StateFlow<List<Sale>> = repository.observeRecentSales(limit = 100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
