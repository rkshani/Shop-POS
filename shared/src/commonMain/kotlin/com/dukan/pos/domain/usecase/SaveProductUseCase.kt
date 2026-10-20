package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.repository.ProductRepository

class SaveProductUseCase(
    private val repository: ProductRepository,
    private val clock: Clock,
) {

    /**
     * [updateStock] says whether [Product.stockQty] is a count the shopkeeper
     * actually entered. A new product always takes it as opening stock; an edit
     * only touches stock when the stock field was changed, so saving a price
     * change never resets stock to what the screen showed when it opened.
     */
    suspend operator fun invoke(
        product: Product,
        updateStock: Boolean = product.id == 0L,
    ): AppResult<Long> {
        if (product.name.isBlank()) {
            return AppError.Validation("name", UiText.Localized(MessageKey.NAME_REQUIRED)).asFailure()
        }
        if (!product.salePrice.isPositive) {
            return AppError.Validation("salePrice", UiText.Localized(MessageKey.SALE_PRICE_REQUIRED)).asFailure()
        }
        val stockTarget = product.stockQty.takeIf { updateStock || product.id == 0L }
        if (stockTarget != null && !stockTarget.isFinite()) {
            return AppError.Validation("stock", UiText.Localized(MessageKey.QTY_MUST_BE_POSITIVE)).asFailure()
        }

        return try {
            val barcode = product.barcode?.trim()?.takeIf { it.isNotEmpty() }
            if (barcode != null && repository.isBarcodeTaken(barcode, product.id.takeIf { it != 0L })) {
                return AppError.Validation("barcode", UiText.Localized(MessageKey.BARCODE_ALREADY_USED)).asFailure()
            }

            val normalized = product.copy(
                name = product.name.trim(),
                nameUrdu = product.nameUrdu?.trim()?.takeIf { it.isNotEmpty() },
                barcode = barcode,
                updatedAt = clock.now(),
            )

            // Opening / manual stock corrections are recorded so the ledger stays auditable.
            repository.save(
                product = normalized,
                stockTarget = stockTarget,
                note = if (product.id == 0L) "Opening stock" else "Edited from product screen",
            ).asSuccess()
        } catch (error: Exception) {
            AppError.Unexpected(error.message).asFailure()
        }
    }
}
