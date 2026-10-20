package com.dukan.pos.presentation.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class KhataViewModel(
    private val repository: CustomerRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _onlyWithBalance = MutableStateFlow(false)
    val onlyWithBalance: StateFlow<Boolean> = _onlyWithBalance.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> =
        combine(_query, _onlyWithBalance) { q, only -> q to only }
            .flatMapLatest { (q, only) -> repository.observeCustomers(q, only) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalOutstanding: StateFlow<Money> = repository.observeTotalOutstanding()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.ZERO)

    fun onQueryChange(value: String) { _query.value = value }

    fun onFilterToggled() { _onlyWithBalance.value = !_onlyWithBalance.value }

    fun addCustomer(name: String, phone: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.upsert(
                Customer(
                    name = name.trim(),
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = Clock.System.now(),
                ),
            )
        }
    }
}
