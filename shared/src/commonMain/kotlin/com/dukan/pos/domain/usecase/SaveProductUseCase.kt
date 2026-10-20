package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.StockMovementType
import com.dukan.pos.domain.repository.ProductRepository

class SaveProductUseCase(
    private val repository: ProductRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(product: Product): AppResult<Long> {
        if (product.name.isBlank()) {
            return AppError.Validation("name", UiText.Localized(MessageKey.NAME_REQUIRED)).asFailure()
        }
        if (!product.salePrice.isPositive) {
            return AppError.Validation("salePrice", UiText.Localized(MessageKey.SALE_PRICE_REQUIRED)).asFailure()
        }
        val barcode = product.barcode?.trim()?.takeIf { it.isNotEmpty() }
        if (barcode != null && repository.isBarcodeTaken(barcode, product.id.takeIf { it != 0L })) {
            return AppError.Validation("barcode", UiText.Localized(MessageKey.BARCODE_ALREADY_USED)).asFailure()
        }

        val existing = if (product.id != 0L) repository.findById(product.id) else null
        val normalized = product.copy(
            name = product.name.trim(),
            nameUrdu = product.nameUrdu?.trim()?.takeIf { it.isNotEmpty() },
            barcode = barcode,
            updatedAt = clock.now(),
        )

        val id = repository.upsert(normalized)

        // Opening / manual stock corrections are recorded so the ledger stays auditable.
        val delta = normalized.stockQty - (existing?.stockQty ?: 0.0)
        if (delta != 0.0) {
            repository.adjustStock(
                productId = id,
                qtyChange = delta,
                type = if (existing == null) StockMovementType.OPENING else StockMovementType.ADJUSTMENT,
                note = if (existing == null) "Opening stock" else "Edited from product screen",
            )
        }
        return id.asSuccess()
    }
}
