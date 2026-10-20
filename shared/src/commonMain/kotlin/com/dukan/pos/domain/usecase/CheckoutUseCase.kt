package com.dukan.pos.domain.usecase

import com.dukan.pos.core.common.AppError
import com.dukan.pos.core.common.AppResult
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.MessageKey
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.StockConflictException
import com.dukan.pos.core.common.UiText
import com.dukan.pos.core.common.asFailure
import com.dukan.pos.core.common.asSuccess
import com.dukan.pos.domain.model.Cart
import com.dukan.pos.domain.model.PaymentMethod
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.model.SaleItem
import com.dukan.pos.domain.repository.RecordedSale
import com.dukan.pos.domain.repository.SaleRepository
import com.dukan.pos.domain.repository.SettingsRepository

/**
 * The one place a sale is allowed to be created. Validates the cart, snapshots
 * prices and costs onto the sale lines, then hands a complete [Sale] to the
 * repository which persists it atomically.
 *
 * The stock check here is for a fast, friendly error. The authoritative one
 * runs inside the database transaction.
 */
class CheckoutUseCase(
    private val saleRepository: SaleRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        cart: Cart,
        amountPaid: Money,
        note: String? = null,
    ): AppResult<RecordedSale> {
        if (cart.isEmpty) return AppError.EmptyCart.asFailure()
        if (amountPaid < Money.ZERO) {
            return AppError.Validation("amountPaid", UiText.Localized(MessageKey.QTY_MUST_BE_POSITIVE)).asFailure()
        }

        val settings = settingsRepository.get()

        if (cart.paymentMethod == PaymentMethod.UDHAAR && cart.customer == null) {
            return AppError.CustomerRequiredForUdhaar.asFailure()
        }

        if (!settings.allowNegativeStock) {
            cart.lines.firstOrNull { it.exceedsStock }?.let { line ->
                return AppError.NotEnoughStock(line.product.name, line.product.stockQty).asFailure()
            }
        }

        cart.lines.firstOrNull { it.qty <= 0 }?.let {
            return AppError.Validation("qty", UiText.Localized(MessageKey.QTY_MUST_BE_POSITIVE)).asFailure()
        }

        val total = cart.total
        val paid = when {
            cart.paymentMethod == PaymentMethod.UDHAAR && amountPaid.isZero -> Money.ZERO
            amountPaid > total -> total // change is handed back, not recorded as revenue
            else -> amountPaid
        }

        if (paid < total && cart.customer == null) {
            return AppError.CustomerRequiredForUdhaar.asFailure()
        }

        val sale = Sale(
            invoiceNo = "", // allocated inside the transaction
            customerId = cart.customer?.id,
            customerName = cart.customer?.name,
            items = cart.lines.map { line ->
                SaleItem(
                    productId = line.product.id,
                    productName = line.product.name,
                    unit = line.product.unit,
                    qty = line.qty,
                    unitPrice = line.unitPrice,
                    unitCost = line.product.costPrice,
                )
            },
            subTotal = cart.subTotal,
            discount = cart.discount,
            total = total,
            paid = paid,
            paymentMethod = cart.paymentMethod,
            note = note,
            createdAt = clock.now(),
        )

        return try {
            saleRepository.recordSale(
                sale = sale,
                invoicePrefix = settings.invoicePrefix,
                allowNegativeStock = settings.allowNegativeStock,
            ).asSuccess()
        } catch (conflict: StockConflictException) {
            AppError.NotEnoughStock(conflict.productName, conflict.available).asFailure()
        } catch (error: Exception) {
            AppError.Unexpected(error.message).asFailure()
        }
    }
}
