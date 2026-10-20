package com.dukan.pos.presentation.common

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.i18n.Strings
import com.dukan.pos.core.i18n.fmt

/**
 * Errors travel out of the ViewModel as data, not as sentences. Only the
 * screen — which knows the current language — turns them into words.
 */
fun AppError.localized(strings: Strings): String = when (this) {
    AppError.EmptyCart -> strings.stockEmpty
    AppError.CustomerRequiredForUdhaar -> strings.addCustomerFirst
    is AppError.NotEnoughStock ->
        "${strings.outOfStock}: $productName (${available.formatQty()})"
    is AppError.Validation -> message.localized(strings)
    is AppError.Unexpected -> cause ?: strings.somethingWentWrong
}

fun UiText.localized(strings: Strings): String = when (this) {
    is UiText.Raw -> value
    is UiText.Localized -> when (key) {
        MessageKey.NAME_REQUIRED -> strings.name
        MessageKey.SALE_PRICE_REQUIRED -> strings.salePrice
        MessageKey.SALE_PRICE_BELOW_COST -> strings.marginPerUnit
        MessageKey.QTY_MUST_BE_POSITIVE -> strings.quantity
        MessageKey.BARCODE_ALREADY_USED -> strings.barcode
        MessageKey.NOT_ENOUGH_STOCK -> strings.outOfStock
        MessageKey.CART_IS_EMPTY -> strings.stockEmpty
        MessageKey.CUSTOMER_REQUIRED_FOR_UDHAAR -> strings.addCustomerFirst
        MessageKey.PAYMENT_EXCEEDS_DUE -> strings.enterValidAmount
        MessageKey.SOMETHING_WENT_WRONG -> strings.somethingWentWrong
    }.let { label -> if (args.isEmpty()) label else label.fmt(*args.toTypedArray()) }
}
