package com.dukan.pos.data.repository

import com.dukan.pos.data.local.dao.DatabaseSnapshot

/** Validate an untrusted backup before entering the destructive replace transaction. */
internal fun validateRestoreSnapshot(s: DatabaseSnapshot) {
    require(s.formatVersion == 1) { "Unsupported backup version" }
    require(s.settings.size == 1 && s.settings.single().id == 1) { "Backup requires exactly one valid shop settings row" }
    fun <T> idsUnique(ids: List<T>): Boolean = ids.size == ids.toSet().size
    require(idsUnique(s.categories.map { it.id }) && idsUnique(s.products.map { it.id }) &&
        idsUnique(s.customers.map { it.id }) && idsUnique(s.suppliers.map { it.id }) &&
        idsUnique(s.sales.map { it.id }) && idsUnique(s.saleItems.map { it.id }) &&
        idsUnique(s.saleReturns.map { it.id }) && idsUnique(s.saleReturnItems.map { it.id }) &&
        idsUnique(s.purchases.map { it.id }) && idsUnique(s.purchaseItems.map { it.id }) &&
        idsUnique(s.ledgerEntries.map { it.id }) && idsUnique(s.stockMovements.map { it.id }) &&
        idsUnique(s.counters.map { it.name })) { "Backup contains duplicate primary keys" }
    val categories = s.categories.map { it.id }.toSet()
    val products = s.products.map { it.id }.toSet()
    val customers = s.customers.map { it.id }.toSet()
    val suppliers = s.suppliers.map { it.id }.toSet()
    val sales = s.sales.associateBy { it.id }
    val saleItems = s.saleItems.associateBy { it.id }
    val returns = s.saleReturns.associateBy { it.id }
    val purchases = s.purchases.map { it.id }.toSet()
    require(s.products.all { (it.categoryId == null || it.categoryId in categories) &&
        it.costPaisa >= 0L && it.salePaisa >= 0L && it.stockQty.isFinite() && it.lowStockAt.isFinite() && it.lowStockAt >= 0.0 }) {
        "Invalid product price, stock or category reference"
    }
    require(s.customers.all { it.balancePaisa >= 0L && (it.creditLimitPaisa == null || it.creditLimitPaisa >= 0L) }) {
        "Invalid customer balance or credit limit"
    }
    require(s.sales.all { (it.customerId == null || it.customerId in customers) && it.subTotalPaisa >= 0L &&
        it.discountPaisa >= 0L && it.discountPaisa <= it.subTotalPaisa &&
        it.totalPaisa == it.subTotalPaisa - it.discountPaisa && it.paidPaisa in 0L..it.totalPaisa }) {
        "Invalid sale totals or customer reference"
    }
    require(s.saleItems.all { it.saleId in sales && it.productId in products && it.qty.isFinite() &&
        it.qty > 0.0 && it.returnedQty.isFinite() && it.returnedQty >= 0.0 &&
        it.returnedQty <= it.qty + 0.000001 && it.unitPricePaisa >= 0L && it.unitCostPaisa >= 0L }) {
        "Invalid sale item quantities or references"
    }
    require(s.saleReturns.all { it.saleId in sales && it.refundPaisa >= 0L &&
        (it.refundMethod == "CASH" || it.refundMethod == "KHATA") && it.customerId == sales[it.saleId]?.customerId }) {
        "Invalid return reference, customer or refund method"
    }
    require(s.saleReturnItems.all { line ->
        val parent = returns[line.returnId]
        val sold = saleItems[line.saleItemId]
        parent != null && sold != null && sold.saleId == parent.saleId &&
            line.productId == sold.productId && line.qty.isFinite() && line.qty > 0.0
    }) { "Invalid return item or cross-invoice reference" }
    val returnedByLine = s.saleReturnItems.groupBy { it.saleItemId }
    require(s.saleItems.all { item ->
        val total = returnedByLine[item.id].orEmpty().sumOf { it.qty }
        total.isFinite() && total <= item.qty + 0.000001 && kotlin.math.abs(total - item.returnedQty) <= 0.0001
    }) { "Returned quantities do not reconcile" }
    val refundsBySale = s.saleReturns.groupBy { it.saleId }
    require(s.sales.all { sale -> refundsBySale[sale.id].orEmpty().sumOf { it.refundPaisa } <= sale.totalPaisa }) {
        "Refunds exceed invoice total"
    }
    require(s.purchases.all { it.totalPaisa >= 0L && (it.supplierId == null || it.supplierId in suppliers) } &&
        s.purchaseItems.all { it.purchaseId in purchases && it.productId in products &&
            it.qty.isFinite() && it.qty > 0.0 && it.unitCostPaisa >= 0L }) {
        "Invalid purchase or purchase item"
    }
    require(s.ledgerEntries.all { it.customerId in customers && it.balanceAfterPaisa >= 0L &&
        (it.saleId == null || it.saleId in sales) } &&
        s.stockMovements.all { it.productId in products && (it.saleId == null || it.saleId in sales) &&
            it.qtyChange.isFinite() && it.qtyAfter.isFinite() }) {
        "Invalid ledger or stock movement reference"
    }
    require(s.counters.all { it.value >= 0L }) { "Invalid sequence counter" }
    require(idsUnique(s.sales.map { it.invoiceNo }) && idsUnique(s.purchases.map { it.reference }) &&
        idsUnique(s.saleReturns.map { it.reference }) && idsUnique(s.suppliers.map { it.name }) &&
        idsUnique(s.categories.map { it.name }) && idsUnique(s.products.mapNotNull { it.barcode })) {
        "Backup violates a unique index"
    }
}
