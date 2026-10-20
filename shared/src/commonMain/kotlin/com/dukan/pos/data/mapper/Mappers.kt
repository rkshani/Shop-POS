package com.dukan.pos.data.mapper

import com.dukan.pos.core.common.Money
import com.dukan.pos.data.local.entity.CategoryEntity
import com.dukan.pos.data.local.entity.CustomerEntity
import com.dukan.pos.data.local.entity.LedgerEntryEntity
import com.dukan.pos.data.local.entity.ProductEntity
import com.dukan.pos.data.local.entity.SaleEntity
import com.dukan.pos.data.local.entity.SaleItemEntity
import com.dukan.pos.data.local.entity.SaleWithItems
import com.dukan.pos.data.local.entity.PurchaseEntity
import com.dukan.pos.data.local.entity.PurchaseItemEntity
import com.dukan.pos.data.local.entity.PurchaseWithItems
import com.dukan.pos.data.local.entity.SaleReturnEntity
import com.dukan.pos.data.local.entity.SaleReturnItemEntity
import com.dukan.pos.data.local.entity.SaleReturnWithItems
import com.dukan.pos.data.local.entity.ShopSettingsEntity
import com.dukan.pos.data.local.entity.SupplierEntity
import com.dukan.pos.data.local.entity.StockMovementEntity
import com.dukan.pos.domain.model.AppLanguage
import com.dukan.pos.domain.model.Category
import com.dukan.pos.domain.model.Customer
import com.dukan.pos.domain.model.LedgerEntry
import com.dukan.pos.domain.model.LedgerEntryType
import com.dukan.pos.domain.model.PaymentMethod
import com.dukan.pos.domain.model.Product
import com.dukan.pos.domain.model.ProductUnit
import com.dukan.pos.domain.model.Purchase
import com.dukan.pos.domain.model.PurchaseItem
import com.dukan.pos.domain.model.RefundMethod
import com.dukan.pos.domain.model.Sale
import com.dukan.pos.domain.model.SaleItem
import com.dukan.pos.domain.model.SaleReturn
import com.dukan.pos.domain.model.SaleReturnItem
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.model.Supplier
import com.dukan.pos.domain.model.StockMovement
import com.dukan.pos.domain.model.StockMovementType
import com.dukan.pos.domain.model.ThemeMode
import kotlinx.datetime.Instant

// ---- Product ----

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    nameUrdu = nameUrdu,
    barcode = barcode,
    categoryId = categoryId,
    unit = enumOr(unit, ProductUnit.PIECE),
    costPrice = Money(costPaisa),
    salePrice = Money(salePaisa),
    stockQty = stockQty,
    lowStockAt = lowStockAt,
    isActive = isActive,
    updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    nameUrdu = nameUrdu,
    barcode = barcode,
    categoryId = categoryId,
    unit = unit.name,
    costPaisa = costPrice.paisa,
    salePaisa = salePrice.paisa,
    stockQty = stockQty,
    lowStockAt = lowStockAt,
    isActive = isActive,
    updatedAtMillis = updatedAt.toEpochMilliseconds(),
    searchText = buildSearchText(name, nameUrdu, barcode),
)

/**
 * One lower-cased haystack per product, pipe-separated so a search can anchor
 * on a field boundary. Written on every upsert — never computed at query time.
 */
fun buildSearchText(name: String, nameUrdu: String?, barcode: String?): String =
    listOfNotNull(name, nameUrdu, barcode)
        .joinToString("|") { it.trim().lowercase() }

fun CategoryEntity.toDomain() = Category(id = id, name = name, sortOrder = sortOrder)

fun Category.toEntity() = CategoryEntity(id = id, name = name, sortOrder = sortOrder)

// ---- Customer / Ledger ----

fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    phone = phone,
    address = address,
    balance = Money(balancePaisa),
    creditLimit = creditLimitPaisa?.let(::Money),
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

fun Customer.toEntity() = CustomerEntity(
    id = id,
    name = name,
    phone = phone,
    address = address,
    balancePaisa = balance.paisa,
    creditLimitPaisa = creditLimit?.paisa,
    createdAtMillis = createdAt.toEpochMilliseconds(),
)

fun LedgerEntryEntity.toDomain() = LedgerEntry(
    id = id,
    customerId = customerId,
    type = enumOr(type, LedgerEntryType.ADJUSTMENT),
    amount = Money(amountPaisa),
    balanceAfter = Money(balanceAfterPaisa),
    saleId = saleId,
    note = note,
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

// ---- Sale ----

fun SaleWithItems.toDomain() = Sale(
    id = sale.id,
    invoiceNo = sale.invoiceNo,
    customerId = sale.customerId,
    customerName = sale.customerName,
    items = items.map { it.toDomain() },
    subTotal = Money(sale.subTotalPaisa),
    discount = Money(sale.discountPaisa),
    total = Money(sale.totalPaisa),
    paid = Money(sale.paidPaisa),
    paymentMethod = enumOr(sale.paymentMethod, PaymentMethod.CASH),
    note = sale.note,
    createdAt = Instant.fromEpochMilliseconds(sale.createdAtMillis),
)

fun SaleItemEntity.toDomain() = SaleItem(
    id = id,
    saleId = saleId,
    productId = productId,
    productName = productName,
    unit = enumOr(unit, ProductUnit.PIECE),
    qty = qty,
    unitPrice = Money(unitPricePaisa),
    unitCost = Money(unitCostPaisa),
    returnedQty = returnedQty,
)

fun Sale.toEntity() = SaleEntity(
    id = id,
    invoiceNo = invoiceNo,
    customerId = customerId,
    customerName = customerName,
    subTotalPaisa = subTotal.paisa,
    discountPaisa = discount.paisa,
    totalPaisa = total.paisa,
    paidPaisa = paid.paisa,
    paymentMethod = paymentMethod.name,
    note = note,
    createdAtMillis = createdAt.toEpochMilliseconds(),
)

fun SaleItem.toEntity(saleId: Long) = SaleItemEntity(
    id = id,
    saleId = saleId,
    productId = productId,
    productName = productName,
    unit = unit.name,
    qty = qty,
    unitPricePaisa = unitPrice.paisa,
    unitCostPaisa = unitCost.paisa,
    returnedQty = returnedQty,
)

fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    productId = productId,
    type = enumOr(type, StockMovementType.ADJUSTMENT),
    qtyChange = qtyChange,
    qtyAfter = qtyAfter,
    saleId = saleId,
    note = note,
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

// ---- Settings ----

fun ShopSettingsEntity.toDomain() = ShopSettings(
    shopName = shopName,
    ownerName = ownerName,
    phone = phone,
    address = address,
    currencySymbol = currencySymbol,
    language = enumOr(language, AppLanguage.ENGLISH),
    theme = enumOr(theme, ThemeMode.SYSTEM),
    allowNegativeStock = allowNegativeStock,
    receiptFooter = receiptFooter,
    invoicePrefix = invoicePrefix,
)

fun ShopSettings.toEntity() = ShopSettingsEntity(
    shopName = shopName,
    ownerName = ownerName,
    phone = phone,
    address = address,
    currencySymbol = currencySymbol,
    language = language.name,
    theme = theme.name,
    allowNegativeStock = allowNegativeStock,
    receiptFooter = receiptFooter,
    invoicePrefix = invoicePrefix,
)

/** Enum columns are stored as names; unknown values fall back instead of crashing. */
private inline fun <reified E : Enum<E>> enumOr(value: String, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == value } ?: fallback

// ---- Purchases ----

fun SupplierEntity.toDomain() = Supplier(
    id = id,
    name = name,
    phone = phone,
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

fun PurchaseWithItems.toDomain() = Purchase(
    id = purchase.id,
    reference = purchase.reference,
    supplierName = purchase.supplierName,
    billNo = purchase.billNo,
    items = items.map { it.toDomain() },
    note = purchase.note,
    createdAt = Instant.fromEpochMilliseconds(purchase.createdAtMillis),
)

fun PurchaseItemEntity.toDomain() = PurchaseItem(
    productId = productId,
    productName = productName,
    unit = ProductUnit.PIECE,
    qty = qty,
    unitCost = Money(unitCostPaisa),
    updatesCostPrice = updatesCostPrice,
)

fun Purchase.toEntity() = PurchaseEntity(
    id = id,
    reference = reference,
    supplierName = supplierName,
    billNo = billNo,
    totalPaisa = total.paisa,
    note = note,
    createdAtMillis = createdAt.toEpochMilliseconds(),
)

fun PurchaseItem.toEntity(purchaseId: Long) = PurchaseItemEntity(
    purchaseId = purchaseId,
    productId = productId,
    productName = productName,
    qty = qty,
    unitCostPaisa = unitCost.paisa,
    updatesCostPrice = updatesCostPrice,
)

// ---- Returns ----

fun SaleReturnWithItems.toDomain() = SaleReturn(
    id = saleReturn.id,
    reference = saleReturn.reference,
    saleId = saleReturn.saleId,
    customerId = saleReturn.customerId,
    items = items.map { it.toDomain() },
    refundMethod = enumOr(saleReturn.refundMethod, RefundMethod.CASH),
    note = saleReturn.note,
    allocatedRefund = Money(saleReturn.refundPaisa),
    createdAt = Instant.fromEpochMilliseconds(saleReturn.createdAtMillis),
)

fun SaleReturnItemEntity.toDomain() = SaleReturnItem(
    saleItemId = saleItemId,
    productId = productId,
    productName = productName,
    qty = qty,
    unitPrice = Money(unitPricePaisa),
    unitCost = Money(unitCostPaisa),
)

fun SaleReturn.toEntity() = SaleReturnEntity(
    id = id,
    reference = reference,
    saleId = saleId,
    customerId = customerId,
    refundPaisa = refund.paisa,
    refundMethod = refundMethod.name,
    note = note,
    createdAtMillis = createdAt.toEpochMilliseconds(),
)

fun SaleReturnItem.toEntity(returnId: Long) = SaleReturnItemEntity(
    returnId = returnId,
    saleItemId = saleItemId,
    productId = productId,
    productName = productName,
    qty = qty,
    unitPricePaisa = unitPrice.paisa,
    unitCostPaisa = unitCost.paisa,
)
