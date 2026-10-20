package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.repository.PurchaseRepository

class RecordPurchaseUseCase(
    private val repository: PurchaseRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(purchase: Purchase): AppResult<String> {
        if (purchase.isEmpty) {
            return AppError.Validation("items", UiText.Localized(MessageKey.CART_IS_EMPTY)).asFailure()
        }
        purchase.items.firstOrNull { it.qty <= 0 }?.let {
            return AppError.Validation("qty", UiText.Localized(MessageKey.QTY_MUST_BE_POSITIVE)).asFailure()
        }

        return runCatching {
            repository.recordPurchase(purchase.copy(createdAt = clock.now()))
        }.fold(
            onSuccess = { it.asSuccess() },
            onFailure = { AppError.Unexpected(it.message).asFailure() },
        )
    }
}
