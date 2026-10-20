package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.repository.CustomerRepository

class RecordCustomerPaymentUseCase(
    private val repository: CustomerRepository,
) {
    suspend operator fun invoke(
        customerId: Long,
        amount: Money,
        note: String?,
        allowOverpayment: Boolean = false,
    ): AppResult<Unit> {
        if (!amount.isPositive) {
            return AppError.Validation("amount", UiText.Localized(MessageKey.QTY_MUST_BE_POSITIVE)).asFailure()
        }
        val customer = runCatching { repository.findById(customerId) }
            .getOrElse { return AppError.Unexpected(it.message).asFailure() }
            ?: return AppError.Unexpected("Customer $customerId not found").asFailure()

        // Do not accept customer credit silently, even when legacy callers pass
        // allowOverpayment=true. No customer-credit accounting flow exists yet.
        if (amount > customer.balance) {
            return AppError.Validation("amount", UiText.Localized(MessageKey.PAYMENT_EXCEEDS_DUE)).asFailure()
        }

        return runCatching { repository.recordPayment(customerId, amount, note) }
            .fold(
                onSuccess = { Unit.asSuccess() },
                onFailure = { AppError.Unexpected(it.message).asFailure() },
            )
    }
}
