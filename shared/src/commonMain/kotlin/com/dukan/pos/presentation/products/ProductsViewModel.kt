package com.dukan.pos.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.domain.model.Category
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.usecase.ObserveProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class ProductsViewModel(
    private val repository: ProductRepository,
    observeProducts: ObserveProductsUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _categoryId = MutableStateFlow<Long?>(null)
    val categoryId: StateFlow<Long?> = _categoryId.asStateFlow()

    private val _onlyLowStock = MutableStateFlow(false)
    val onlyLowStock: StateFlow<Boolean> = _onlyLowStock.asStateFlow()

    val products: StateFlow<List<Product>> = observeProducts(_query, _categoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lowStock: StateFlow<List<Product>> = repository.observeLowStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) { _query.value = value }
    fun onCategorySelected(id: Long?) { _categoryId.value = id }
    fun onLowStockToggled() { _onlyLowStock.value = !_onlyLowStock.value }
}
