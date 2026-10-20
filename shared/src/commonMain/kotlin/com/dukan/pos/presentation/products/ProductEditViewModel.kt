package com.dukan.pos.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.toQtyOrNull
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormState(
    val id: Long = 0L,
    val name: String = "",
    val nameUrdu: String = "",
    val barcode: String = "",
    val unit: ProductUnit = ProductUnit.PIECE,
    val costText: String = "",
    val priceText: String = "",
    val stockText: String = "",
    val lowStockText: String = "",
    val isSaving: Boolean = false,
    val error: AppError? = null,
) {
    val isEditing: Boolean get() = id != 0L
    val margin: Money
        get() {
            val cost = Money.parseOrNull(costText) ?: Money.ZERO
            val price = Money.parseOrNull(priceText) ?: Money.ZERO
            return price - cost
        }
}

sealed interface ProductEditEffect {
    data object Saved : ProductEditEffect
    data class Failed(val error: AppError) : ProductEditEffect
}

class ProductEditViewModel(
    private val productId: Long,
    private val repository: ProductRepository,
    private val saveProduct: SaveProductUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState(id = productId))
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    private val _effects = Channel<ProductEditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Stock as loaded; only a changed value is written back as a stock count. */
    private var loadedStockText: String? = null

    init {
        if (productId != 0L) {
            viewModelScope.launch {
                repository.findById(productId)?.let { product ->
                    loadedStockText = product.stockQty.toString().removeSuffix(".0")
                    _state.value = ProductFormState(
                        id = product.id,
                        name = product.name,
                        nameUrdu = product.nameUrdu.orEmpty(),
                        barcode = product.barcode.orEmpty(),
                        unit = product.unit,
                        costText = product.costPrice.rupees.trimZero(),
                        priceText = product.salePrice.rupees.trimZero(),
                        stockText = loadedStockText.orEmpty(),
                        lowStockText = product.lowStockAt.toString().removeSuffix(".0"),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onNameUrduChange(value: String) = _state.update { it.copy(nameUrdu = value) }
    fun onBarcodeChange(value: String) = _state.update { it.copy(barcode = value, error = null) }
    fun onUnitChange(value: ProductUnit) = _state.update { it.copy(unit = value) }
    fun onCostChange(value: String) = _state.update { it.copy(costText = value) }
    fun onPriceChange(value: String) = _state.update { it.copy(priceText = value, error = null) }
    fun onStockChange(value: String) = _state.update { it.copy(stockText = value) }
    fun onLowStockChange(value: String) = _state.update { it.copy(lowStockText = value) }

    fun onBarcodeScanned(value: String) = onBarcodeChange(value)

    fun onSave() {
        val form = _state.value
        if (form.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val product = Product(
                id = form.id,
                name = form.name,
                nameUrdu = form.nameUrdu.takeIf { it.isNotBlank() },
                barcode = form.barcode.takeIf { it.isNotBlank() },
                unit = form.unit,
                costPrice = Money.parseOrNull(form.costText) ?: Money.ZERO,
                salePrice = Money.parseOrNull(form.priceText) ?: Money.ZERO,
                stockQty = form.stockText.toQtyOrNull() ?: 0.0,
                lowStockAt = form.lowStockText.toQtyOrNull() ?: 0.0,
                updatedAt = clock.now(),
            )
            val stockEdited = form.id == 0L || form.stockText.trim() != loadedStockText
            when (val result = saveProduct(product, updateStock = stockEdited)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(ProductEditEffect.Saved)
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSaving = false, error = result.error) }
                    _effects.send(ProductEditEffect.Failed(result.error))
                }
            }
        }
    }
}

private fun Double.trimZero(): String = toString().removeSuffix(".0")
