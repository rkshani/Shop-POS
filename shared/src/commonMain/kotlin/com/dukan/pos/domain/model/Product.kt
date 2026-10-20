package com.dukan.pos.domain.model

import com.dukan.pos.core.common.Money
import kotlinx.datetime.Instant

data class Product(
    val id: Long = 0L,
    val name: String,
    val nameUrdu: String? = null,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val unit: ProductUnit = ProductUnit.PIECE,
    val costPrice: Money = Money.ZERO,
    val salePrice: Money = Money.ZERO,
    val stockQty: Double = 0.0,
    val lowStockAt: Double = 0.0,
    val isActive: Boolean = true,
    val updatedAt: Instant,
) {
    val isLowStock: Boolean get() = lowStockAt > 0 && stockQty <= lowStockAt
    val isOutOfStock: Boolean get() = stockQty <= 0
    val marginPerUnit: Money get() = salePrice - costPrice
    val stockValueAtCost: Money get() = costPrice * stockQty

    /** Name shown on the counter: Urdu when present, English otherwise. */
    fun displayName(preferUrdu: Boolean): String =
        if (preferUrdu && !nameUrdu.isNullOrBlank()) nameUrdu else name
}

enum class ProductUnit(val shortLabel: String, val allowsFraction: Boolean) {
    PIECE("pc", false),
    PACK("pack", false),
    DOZEN("dz", false),
    KILOGRAM("kg", true),
    GRAM("g", true),
    LITRE("L", true),
    MILLILITRE("ml", true),
    METER("m", true),
}

data class Category(
    val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
)
