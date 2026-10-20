package com.dukan.pos.domain.usecase

import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.repository.ProductRepository

/**
 * Resolves a scanned barcode to a cart. Returns null when the barcode is
 * unknown so the UI can offer "add this product".
 */
class AddScannedProductUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(cart: Cart, barcode: String): Result? {
        val product = repository.findByBarcode(barcode.trim()) ?: return null
        return Result(cart = cart.upsert(product, qtyDelta = 1.0), product = product)
    }

    data class Result(val cart: Cart, val product: Product)
}
