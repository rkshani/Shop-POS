package com.dukan.pos.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.model.SaleReturnItem
import com.dukan.pos.domain.model.returnableQty
import com.dukan.pos.domain.model.allocatedRefundPaisa
import kotlin.math.roundToLong
import com.dukan.pos.domain.repository.ReturnRepository
import com.dukan.pos.domain.repository.SaleRepository
import com.dukan.pos.domain.usecase.RecordSaleReturnUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface SaleDetailEffect {
    data class ReturnSaved(val reference: String) : SaleDetailEffect
    data class Failed(val error: AppError) : SaleDetailEffect
}

data class ReturnDraft(
    /** saleItemId → quantity coming back. */
    val quantities: Map<Long, Double> = emptyMap(),
    val refundMethod: RefundMethod = RefundMethod.CASH,
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
) {
    val hasSelection: Boolean get() = quantities.values.any { it > 0 }
}

class SaleDetailViewModel(
    private val saleId: Long,
    private val saleRepository: SaleRepository,
    private val returnRepository: ReturnRepository,
    private val recordReturn: RecordSaleReturnUseCase,
) : ViewModel() {

    val sale: StateFlow<Sale?> = saleRepository.observeSale(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val returns: StateFlow<List<SaleReturn>> = returnRepository.observeReturnsForSale(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow(ReturnDraft())
    val draft: StateFlow<ReturnDraft> = _draft.asStateFlow()

    private val _effects = Channel<SaleDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun openReturn() {
        val current = sale.value ?: return
        val refundMethod = if (current.customerId != null && current.due.isPositive) {
            // Money never actually changed hands on an unpaid udhaar sale, so
            // the honest default is to knock it off the khata.
            RefundMethod.KHATA
        } else {
            RefundMethod.CASH
        }
        _draft.value = ReturnDraft(isOpen = true, refundMethod = refundMethod)
    }

    fun closeReturn() = _draft.update { ReturnDraft() }

    fun onQtyChange(saleItemId: Long, qty: Double) {
        val item = sale.value?.items?.firstOrNull { it.id == saleItemId } ?: return
        val clamped = qty.coerceIn(0.0, item.returnableQty)
        _draft.update { it.copy(quantities = it.quantities + (saleItemId to clamped)) }
    }

    fun onRefundMethodChange(method: RefundMethod) = _draft.update { it.copy(refundMethod = method) }

    fun refundTotal(): Money {
        val current = sale.value ?: return Money.ZERO
        if (current.subTotal.paisa <= 0L) return Money.ZERO
        val previous = current.items.sumOf { item ->
            (item.returnedQty * item.unitPrice.paisa).roundToLong()
        }
        val selected = _draft.value.quantities.entries.sumOf { (itemId, qty) ->
            val item = current.items.firstOrNull { it.id == itemId }
            if (item == null || qty <= 0.0) 0L else (qty * item.unitPrice.paisa).roundToLong()
        }
        return Money(allocatedRefundPaisa(current.subTotal.paisa, current.total.paisa, previous, selected))
    }

    fun onConfirmReturn() {
        val current = sale.value ?: return
        val draftNow = _draft.value
        if (draftNow.isSaving || !draftNow.hasSelection) return
        _draft.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val items = draftNow.quantities
                .filterValues { it > 0 }
                .mapNotNull { (itemId, qty) ->
                    val item = current.items.firstOrNull { it.id == itemId } ?: return@mapNotNull null
                    SaleReturnItem(
                        saleItemId = itemId,
                        productId = item.productId,
                        productName = item.productName,
                        qty = qty,
                        unitPrice = item.unitPrice,
                        unitCost = item.unitCost,
                    )
                }

            val result = recordReturn(
                SaleReturn(
                    saleId = saleId,
                    customerId = current.customerId,
                    items = items,
                    refundMethod = draftNow.refundMethod,
                    allocatedRefund = refundTotal(),
                    createdAt = Clock.System.now(),
                ),
            )
            when (result) {
                is AppResult.Success -> {
                    _draft.value = ReturnDraft()
                    _effects.send(SaleDetailEffect.ReturnSaved(result.data))
                }
                is AppResult.Failure -> {
                    _draft.update { it.copy(isSaving = false) }
                    _effects.send(SaleDetailEffect.Failed(result.error))
                }
            }
        }
    }
}
