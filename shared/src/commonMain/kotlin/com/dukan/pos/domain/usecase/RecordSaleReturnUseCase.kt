package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.ReturnConflictException
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.repository.ReturnRepository

class RecordSaleReturnUseCase(
    private val repository: ReturnRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(saleReturn: SaleReturn): AppResult<String> {
        val lines = saleReturn.items.filter { it.qty > 0 }
        if (lines.isEmpty()) {
            return AppError.Validation("items", UiText.Localized(MessageKey.CART_IS_EMPTY)).asFailure()
        }
        // Refunding into a khata only makes sense when there is a khata.
        if (saleReturn.refundMethod == RefundMethod.KHATA && saleReturn.customerId == null) {
            return AppError.CustomerRequiredForUdhaar.asFailure()
        }

        return try {
            repository.recordReturn(
                saleReturn.copy(items = lines, createdAt = clock.now()),
            ).asSuccess()
        } catch (conflict: ReturnConflictException) {
            AppError.NotEnoughStock(conflict.productName, conflict.returnable).asFailure()
        } catch (error: Exception) {
            AppError.Unexpected(error.message).asFailure()
        }
    }
}
