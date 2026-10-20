package com.dukan.pos.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val items: List<SaleItemEntity>,
)

data class PurchaseWithItems(
    @Embedded val purchase: PurchaseEntity,
    @Relation(parentColumn = "id", entityColumn = "purchaseId")
    val items: List<PurchaseItemEntity>,
)

data class SaleReturnWithItems(
    @Embedded val saleReturn: SaleReturnEntity,
    @Relation(parentColumn = "id", entityColumn = "returnId")
    val items: List<SaleReturnItemEntity>,
)

data class SummaryRow(
    val salesCount: Int,
    val grossPaisa: Long,
    val discountPaisa: Long,
    val paidPaisa: Long,
)

data class MoneyFlowRow(
    val cashSalesPaisa: Long,
    val digitalPaisa: Long,
    val khataCollectedPaisa: Long,
    val returnedPaisa: Long,
    val cashRefundedPaisa: Long,
    val returnedCostPaisa: Long,
)

data class DailyRow(
    val dayEpoch: Long,
    val salesPaisa: Long,
    val profitPaisa: Long,
)

data class TopProductRow(
    val productId: Long,
    val name: String,
    val qtySold: Double,
    val revenuePaisa: Long,
    val profitPaisa: Long,
)
