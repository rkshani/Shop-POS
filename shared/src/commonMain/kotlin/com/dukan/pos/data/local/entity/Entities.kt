package com.dukan.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["searchText"]),
        Index(value = ["categoryId"]),
        Index(value = ["isActive"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
@Serializable
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val nameUrdu: String? = null,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val unit: String,
    val costPaisa: Long,
    val salePaisa: Long,
    val stockQty: Double,
    val lowStockAt: Double,
    val isActive: Boolean,
    val updatedAtMillis: Long,
    /**
     * Lower-cased "name | urdu name | barcode", written on every upsert.
     * One indexed column to match against instead of three LIKEs across three.
     */
    val searchText: String = "",
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"]), Index(value = ["phone"])],
)
@Serializable
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val balancePaisa: Long = 0L,
    val creditLimitPaisa: Long? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["createdAtMillis"]), Index(value = ["customerId"]), Index(value = ["invoiceNo"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
@Serializable
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNo: String,
    val customerId: Long? = null,
    val customerName: String? = null,
    val subTotalPaisa: Long,
    val discountPaisa: Long,
    val totalPaisa: Long,
    val paidPaisa: Long,
    val paymentMethod: String,
    val note: String? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "sale_items",
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])],
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val qty: Double,
    val unitPricePaisa: Long,
    val unitCostPaisa: Long,
    val returnedQty: Double = 0.0,
)

@Entity(
    tableName = "ledger_entries",
    indices = [Index(value = ["customerId"]), Index(value = ["createdAtMillis"])],
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerId: Long,
    val type: String,
    val amountPaisa: Long,
    val balanceAfterPaisa: Long,
    val saleId: Long? = null,
    val note: String? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["createdAtMillis"])],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val type: String,
    val qtyChange: Double,
    val qtyAfter: Double,
    val saleId: Long? = null,
    val note: String? = null,
    val createdAtMillis: Long,
)

@Entity(tableName = "shop_settings")
@Serializable
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val currencySymbol: String,
    val language: String,
    val theme: String,
    val allowNegativeStock: Boolean,
    val receiptFooter: String,
    val invoicePrefix: String,
) {
    companion object { const val SINGLETON_ID = 1 }
}

@Entity(tableName = "counters")
@Serializable
data class CounterEntity(
    @PrimaryKey val name: String,
    val value: Long,
) {
    companion object {
        const val INVOICE = "invoice"
        const val PURCHASE = "purchase"
        const val RETURN = "sale_return"
    }
}

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"], unique = true)],
)
@Serializable
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["supplierId"]),
        Index(value = ["reference"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
@Serializable
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reference: String,
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val billNo: String? = null,
    val totalPaisa: Long,
    val note: String? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "purchase_items",
    indices = [Index(value = ["purchaseId"]), Index(value = ["productId"])],
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double,
    val unitCostPaisa: Long,
    /** When true this bill's cost becomes the product's new cost price. */
    val updatesCostPrice: Boolean,
)

@Entity(
    tableName = "sale_returns",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["createdAtMillis"]),
        Index(value = ["reference"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reference: String,
    val saleId: Long,
    val customerId: Long? = null,
    val refundPaisa: Long,
    /** CASH or KHATA — cash leaves the till, khata reduces what the customer owes. */
    val refundMethod: String,
    val note: String? = null,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "sale_return_items",
    indices = [Index(value = ["returnId"]), Index(value = ["saleItemId"])],
    foreignKeys = [
        ForeignKey(
            entity = SaleReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val returnId: Long,
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double,
    val unitPricePaisa: Long,
    val unitCostPaisa: Long,
)
