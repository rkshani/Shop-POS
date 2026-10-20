package com.dukan.pos.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.PaymentMethod
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.CustomerRepository
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.repository.SettingsRepository
import com.dukan.pos.domain.usecase.AddScannedProductUseCase
import com.dukan.pos.domain.usecase.CheckoutUseCase
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

data class PosUiState(
    val cart: Cart = Cart(),
    val settings: ShopSettings = ShopSettings(),
    val isScannerOpen: Boolean = false,
    val isCheckoutOpen: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface PosEffect {
    data class SaleCompleted(val saleId: Long, val invoiceNo: String) : PosEffect
    data class ProductAdded(val productName: String) : PosEffect
    data class Failed(val error: AppError) : PosEffect
    data class UnknownBarcode(val barcode: String) : PosEffect
}

class PosViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
    private val checkout: CheckoutUseCase,
    private val addScanned: AddScannedProductUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PosUiState())
    val state: StateFlow<PosUiState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _effects = Channel<PosEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val products: StateFlow<List<Product>> = _query
        .debounce(150.milliseconds)
        .flatMapLatest { productRepository.observeProducts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun onProductTapped(product: Product) = updateCart { it.upsert(product, qtyDelta = 1.0) }

    fun onQtyChange(productId: Long, qty: Double) = updateCart { it.setQty(productId, qty) }

    /** Typed quantity: sets the line if the product is already in the cart, adds it otherwise. */
    fun onQtyEntered(product: Product, qty: Double) = updateCart { cart ->
        if (cart.findLine(product.id) == null) cart.upsert(product, qtyDelta = qty) else cart.setQty(product.id, qty)
    }

    fun onUnitPriceChange(productId: Long, price: Money) = updateCart { it.setUnitPrice(productId, price) }

    fun onRemoveLine(productId: Long) = updateCart { it.removeLine(productId) }

    fun onDiscountChange(discount: Money) = updateCart { it.copy(discount = discount) }

    fun onCustomerSelected(customer: Customer?) = updateCart { it.copy(customer = customer) }

    fun onPaymentMethodChange(method: PaymentMethod) = updateCart { it.copy(paymentMethod = method) }

    fun onClearCart() = updateCart { it.clearLines().copy(customer = null, discount = Money.ZERO) }

    fun openScanner() = _state.update { it.copy(isScannerOpen = true) }
    fun closeScanner() = _state.update { it.copy(isScannerOpen = false) }
    fun openCheckout() = _state.update { it.copy(isCheckoutOpen = true) }
    fun closeCheckout() = _state.update { it.copy(isCheckoutOpen = false) }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val result = addScanned(_state.value.cart, barcode)
            if (result == null) {
                _effects.send(PosEffect.UnknownBarcode(barcode))
            } else {
                _state.update { it.copy(cart = result.cart) }
                _effects.send(PosEffect.ProductAdded(result.product.name))
            }
        }
    }

    fun onConfirmCheckout(amountPaid: Money, note: String?) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            when (val result = checkout(_state.value.cart, amountPaid, note)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            cart = Cart(paymentMethod = it.cart.paymentMethod),
                            isSubmitting = false,
                            isCheckoutOpen = false,
                        )
                    }
                    _effects.send(
                        PosEffect.SaleCompleted(result.data.id, result.data.invoiceNo),
                    )
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.send(PosEffect.Failed(result.error))
                }
            }
        }
    }

    private inline fun updateCart(transform: (Cart) -> Cart) {
        _state.update { it.copy(cart = transform(it.cart)) }
    }
}
