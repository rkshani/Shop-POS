package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dukan.pos.data.local.entity.CategoryEntity
import com.dukan.pos.data.local.entity.CounterEntity
import com.dukan.pos.data.local.entity.CustomerEntity
import com.dukan.pos.data.local.entity.LedgerEntryEntity
import com.dukan.pos.data.local.entity.ProductEntity
import com.dukan.pos.data.local.entity.PurchaseEntity
import com.dukan.pos.data.local.entity.PurchaseItemEntity
import com.dukan.pos.data.local.entity.SaleEntity
import com.dukan.pos.data.local.entity.SaleItemEntity
import com.dukan.pos.data.local.entity.SaleReturnEntity
import com.dukan.pos.data.local.entity.SaleReturnItemEntity
import com.dukan.pos.data.local.entity.ShopSettingsEntity
import com.dukan.pos.data.local.entity.StockMovementEntity
import com.dukan.pos.data.local.entity.SupplierEntity
import kotlinx.serialization.Serializable

/**
 * Whole-database read and replace. Restore runs as one transaction: a backup
 * that half-applies is worse than no backup at all.
 */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM categories") abstract suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM products") abstract suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM customers") abstract suspend fun customers(): List<CustomerEntity>
    @Query("SELECT * FROM suppliers") abstract suspend fun suppliers(): List<SupplierEntity>
    @Query("SELECT * FROM sales") abstract suspend fun sales(): List<SaleEntity>
    @Query("SELECT * FROM sale_items") abstract suspend fun saleItems(): List<SaleItemEntity>
    @Query("SELECT * FROM sale_returns") abstract suspend fun saleReturns(): List<SaleReturnEntity>
    @Query("SELECT * FROM sale_return_items") abstract suspend fun saleReturnItems(): List<SaleReturnItemEntity>
    @Query("SELECT * FROM purchases") abstract suspend fun purchases(): List<PurchaseEntity>
    @Query("SELECT * FROM purchase_items") abstract suspend fun purchaseItems(): List<PurchaseItemEntity>
    @Query("SELECT * FROM ledger_entries") abstract suspend fun ledgerEntries(): List<LedgerEntryEntity>
    @Query("SELECT * FROM stock_movements") abstract suspend fun stockMovements(): List<StockMovementEntity>
    @Query("SELECT * FROM counters") abstract suspend fun counters(): List<CounterEntity>
    @Query("SELECT * FROM shop_settings") abstract suspend fun settings(): List<ShopSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putCategories(rows: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putProducts(rows: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putCustomers(rows: List<CustomerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSuppliers(rows: List<SupplierEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSales(rows: List<SaleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSaleItems(rows: List<SaleItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSaleReturns(rows: List<SaleReturnEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSaleReturnItems(rows: List<SaleReturnItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putPurchases(rows: List<PurchaseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putPurchaseItems(rows: List<PurchaseItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putLedgerEntries(rows: List<LedgerEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putStockMovements(rows: List<StockMovementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putCounters(rows: List<CounterEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putSettings(rows: List<ShopSettingsEntity>)

    @Query("DELETE FROM stock_movements") abstract suspend fun clearStockMovements()
    @Query("DELETE FROM ledger_entries") abstract suspend fun clearLedgerEntries()
    @Query("DELETE FROM sale_return_items") abstract suspend fun clearSaleReturnItems()
    @Query("DELETE FROM sale_returns") abstract suspend fun clearSaleReturns()
    @Query("DELETE FROM sale_items") abstract suspend fun clearSaleItems()
    @Query("DELETE FROM sales") abstract suspend fun clearSales()
    @Query("DELETE FROM purchase_items") abstract suspend fun clearPurchaseItems()
    @Query("DELETE FROM purchases") abstract suspend fun clearPurchases()
    @Query("DELETE FROM products") abstract suspend fun clearProducts()
    @Query("DELETE FROM categories") abstract suspend fun clearCategories()
    @Query("DELETE FROM customers") abstract suspend fun clearCustomers()
    @Query("DELETE FROM suppliers") abstract suspend fun clearSuppliers()
    @Query("DELETE FROM counters") abstract suspend fun clearCounters()
    @Query("DELETE FROM shop_settings") abstract suspend fun clearSettings()

    @Transaction
    open suspend fun replaceAll(snapshot: DatabaseSnapshot) {
        // Children first, then parents: foreign keys are on.
        clearStockMovements()
        clearLedgerEntries()
        clearSaleReturnItems()
        clearSaleReturns()
        clearSaleItems()
        clearSales()
        clearPurchaseItems()
        clearPurchases()
        clearProducts()
        clearCategories()
        clearCustomers()
        clearSuppliers()
        clearCounters()
        clearSettings()

        putCategories(snapshot.categories)
        putCustomers(snapshot.customers)
        putSuppliers(snapshot.suppliers)
        putProducts(snapshot.products)
        putSales(snapshot.sales)
        putSaleItems(snapshot.saleItems)
        putSaleReturns(snapshot.saleReturns)
        putSaleReturnItems(snapshot.saleReturnItems)
        putPurchases(snapshot.purchases)
        putPurchaseItems(snapshot.purchaseItems)
        putLedgerEntries(snapshot.ledgerEntries)
        putStockMovements(snapshot.stockMovements)
        putCounters(snapshot.counters)
        putSettings(snapshot.settings)
    }

    @Transaction
    open suspend fun snapshot() = DatabaseSnapshot(
        categories = categories(),
        products = products(),
        customers = customers(),
        suppliers = suppliers(),
        sales = sales(),
        saleItems = saleItems(),
        saleReturns = saleReturns(),
        saleReturnItems = saleReturnItems(),
        purchases = purchases(),
        purchaseItems = purchaseItems(),
        ledgerEntries = ledgerEntries(),
        stockMovements = stockMovements(),
        counters = counters(),
        settings = settings(),
    )
}

@Serializable
data class DatabaseSnapshot(
    /** Bumped whenever the shape changes so a restore can refuse a future file. */
    val formatVersion: Int = 1,
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val saleReturns: List<SaleReturnEntity> = emptyList(),
    val saleReturnItems: List<SaleReturnItemEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val counters: List<CounterEntity> = emptyList(),
    val settings: List<ShopSettingsEntity> = emptyList(),
)
