package com.dukan.pos.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import com.dukan.pos.data.local.dao.BackupDao
import com.dukan.pos.data.local.dao.CustomerDao
import com.dukan.pos.data.local.dao.ProductDao
import com.dukan.pos.data.local.dao.PurchaseDao
import com.dukan.pos.data.local.dao.ReportDao
import com.dukan.pos.data.local.dao.ReturnDao
import com.dukan.pos.data.local.dao.SaleDao
import com.dukan.pos.data.local.dao.SettingsDao
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

const val DATABASE_NAME = "dukan.db"
const val DATABASE_VERSION = 1

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SaleReturnEntity::class,
        SaleReturnItemEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        LedgerEntryEntity::class,
        StockMovementEntity::class,
        CounterEntity::class,
        ShopSettingsEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
@ConstructedBy(DukanDatabaseConstructor::class)
abstract class DukanDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun returnDao(): ReturnDao
    abstract fun reportDao(): ReportDao
    abstract fun settingsDao(): SettingsDao
    abstract fun backupDao(): BackupDao
}

/**
 * Empty until version 2 — but the list is wired up now so that adding a
 * migration is a one-line change instead of an emergency. Destructive fallback
 * is deliberately NOT enabled: on a shop's only copy of its khata, wiping the
 * database on a schema mistake is not an acceptable failure mode. A missing
 * migration should crash in testing, loudly, before it ever ships.
 */
val DUKAN_MIGRATIONS: Array<Migration> = arrayOf()

// Generated per target by the Room compiler.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object DukanDatabaseConstructor : RoomDatabaseConstructor<DukanDatabase> {
    override fun initialize(): DukanDatabase
}
