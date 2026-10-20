package com.dukan.pos.core.common

/**
 * Text produced by the domain layer that the UI resolves.
 * Keeps domain code free of Compose/resource imports while still letting
 * every message ship in both English and Urdu.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Localized(val key: MessageKey, val args: List<String> = emptyList()) : UiText
}

enum class MessageKey {
    NAME_REQUIRED,
    SALE_PRICE_REQUIRED,
    SALE_PRICE_BELOW_COST,
    QTY_MUST_BE_POSITIVE,
    BARCODE_ALREADY_USED,
    NOT_ENOUGH_STOCK,
    CART_IS_EMPTY,
    CUSTOMER_REQUIRED_FOR_UDHAAR,
    PAYMENT_EXCEEDS_DUE,
    SOMETHING_WENT_WRONG,
}
