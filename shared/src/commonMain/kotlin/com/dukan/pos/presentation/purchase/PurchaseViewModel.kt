package com.dukan.pos.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.model.PurchaseItem
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.usecase.RecordPurchaseUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PurchaseLine(
    val product: Product,
    val qty: Double,
    val unitCost: Money,
    val updatesCostPrice: Boolean = true,
) {
    val lineTotal: Money get() = unitCost * qty
    /** Highlighted in the UI — a jump in buying price is the thing to notice. */
    val costChanged: Boolean get() = unitCost != product.costPrice
}

data class PurchaseUiState(
    val lines: List<PurchaseLine> = emptyList(),
    val supplier: String = "",
    val billNo: String = "",
    val isSaving: Boolean = false,
) {
    val total: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.lineTotal }
    val isEmpty: Boolean get() = lines.isEmpty()
}

sealed interface PurchaseEffect {
    data class Saved(val reference: String) : PurchaseEffect
    data class Failed(val error: AppError) : PurchaseEffect
}

class PurchaseViewModel(
    private val productRepository: ProductRepository,
    private val recordPurchase: RecordPurchaseUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseUiState())
    val state: StateFlow<PurchaseUiState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _effects = Channel<PurchaseEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val products: StateFlow<List<Product>> = _query
        .debounce(150.milliseconds)
        .flatMapLatest { productRepository.observeProducts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) { _query.value = value }

    fun onSupplierChange(value: String) = _state.update { it.copy(supplier = value) }
    fun onBillNoChange(value: String) = _state.update { it.copy(billNo = value) }

    fun onProductTapped(product: Product) = _state.update { state ->
        if (state.lines.any { it.product.id == product.id }) {
            state.copy(
                lines = state.lines.map {
                    if (it.product.id == product.id) it.copy(qty = it.qty + 1) else it
                },
            )
        } else {
            // The last known cost is the best guess; the shopkeeper corrects it
            // only when the bill actually changed.
            state.copy(lines = state.lines + PurchaseLine(product, 1.0, product.costPrice))
        }
    }

    fun onQtyChange(productId: Long, qty: Double) = _state.update { state ->
        if (qty <= 0) state.copy(lines = state.lines.filterNot { it.product.id == productId })
        else state.copy(lines = state.lines.map { if (it.product.id == productId) it.copy(qty = qty) else it })
    }

    fun onCostChange(productId: Long, cost: Money) = _state.update { state ->
        state.copy(lines = state.lines.map { if (it.product.id == productId) it.copy(unitCost = cost) else it })
    }

    fun onUpdateCostToggled(productId: Long) = _state.update { state ->
        state.copy(
            lines = state.lines.map {
                if (it.product.id == productId) it.copy(updatesCostPrice = !it.updatesCostPrice) else it
            },
        )
    }

    fun onRemoveLine(productId: Long) = _state.update { state ->
        state.copy(lines = state.lines.filterNot { it.product.id == productId })
    }

    fun onSave() {
        val current = _state.value
        if (current.isSaving || current.isEmpty) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val purchase = Purchase(
                supplierName = current.supplier.takeIf { it.isNotBlank() },
                billNo = current.billNo.takeIf { it.isNotBlank() },
                items = current.lines.map { line ->
                    PurchaseItem(
                        productId = line.product.id,
                        productName = line.product.name,
                        unit = line.product.unit,
                        qty = line.qty,
                        unitCost = line.unitCost,
                        updatesCostPrice = line.updatesCostPrice,
                    )
                },
                createdAt = clock.now(),
            )
            when (val result = recordPurchase(purchase)) {
                is AppResult.Success -> {
                    _state.value = PurchaseUiState()
                    _effects.send(PurchaseEffect.Saved(result.data))
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(PurchaseEffect.Failed(result.error))
                }
            }
        }
    }
}
