package com.dukan.pos.presentation.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Money
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.LedgerEntry
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.CustomerRepository
import com.dukan.pos.domain.repository.SettingsRepository
import com.dukan.pos.domain.usecase.RecordCustomerPaymentUseCase
import com.dukan.pos.domain.usecase.SendUdhaarReminderUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CustomerDetailEffect {
    data class Failed(val error: AppError) : CustomerDetailEffect
    data object PaymentRecorded : CustomerDetailEffect
    data object InvalidAmount : CustomerDetailEffect
    data object NoPhone : CustomerDetailEffect
}

class CustomerDetailViewModel(
    private val customerId: Long,
    private val repository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
    private val recordPayment: RecordCustomerPaymentUseCase,
    private val sendReminder: SendUdhaarReminderUseCase,
) : ViewModel() {

    val customer: StateFlow<Customer?> = repository.observeCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ledger: StateFlow<List<LedgerEntry>> = repository.observeLedger(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<ShopSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopSettings())

    private val _effects = Channel<CustomerDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onPaymentReceived(amountText: String, note: String?) {
        val amount = Money.parseOrNull(amountText)
        if (amount == null) {
            viewModelScope.launch { _effects.send(CustomerDetailEffect.InvalidAmount) }
            return
        }
        viewModelScope.launch {
            when (val result = recordPayment(customerId, amount, note)) {
                is AppResult.Success -> _effects.send(CustomerDetailEffect.PaymentRecorded)
                is AppResult.Failure -> _effects.send(CustomerDetailEffect.Failed(result.error))
            }
        }
    }

    /** [message] arrives already translated and filled in by the screen. */
    fun onSendReminder(message: String) {
        val current = customer.value ?: return
        viewModelScope.launch {
            when (sendReminder(current, message)) {
                SendUdhaarReminderUseCase.Result.Opened -> Unit
                SendUdhaarReminderUseCase.Result.NoPhone -> _effects.send(CustomerDetailEffect.NoPhone)
                SendUdhaarReminderUseCase.Result.NoApp -> _effects.send(CustomerDetailEffect.NoPhone)
            }
        }
    }
}
