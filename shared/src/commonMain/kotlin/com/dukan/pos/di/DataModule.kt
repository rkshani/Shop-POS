package com.dukan.pos.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.DefaultAppDispatchers
import com.dukan.pos.core.common.SystemClock
import com.dukan.pos.data.local.DatabaseFactory
import com.dukan.pos.data.local.DUKAN_MIGRATIONS
import com.dukan.pos.data.local.DukanDatabase
import com.dukan.pos.data.repository.CustomerRepositoryImpl
import com.dukan.pos.data.repository.ProductRepositoryImpl
import com.dukan.pos.data.repository.ReportRepositoryImpl
import com.dukan.pos.data.repository.SaleRepositoryImpl
import com.dukan.pos.data.repository.BackupRepositoryImpl
import com.dukan.pos.data.repository.PurchaseRepositoryImpl
import com.dukan.pos.data.repository.ReturnRepositoryImpl
import com.dukan.pos.data.repository.SettingsRepositoryImpl
import com.dukan.pos.domain.repository.CustomerRepository
import com.dukan.pos.domain.repository.ProductRepository
import com.dukan.pos.domain.repository.ReportRepository
import com.dukan.pos.domain.repository.SaleRepository
import com.dukan.pos.domain.repository.BackupRepository
import com.dukan.pos.domain.repository.PurchaseRepository
import com.dukan.pos.domain.repository.ReturnRepository
import com.dukan.pos.domain.repository.SettingsRepository
import org.koin.dsl.module

val dataModule = module {
    single<AppDispatchers> { DefaultAppDispatchers() }
    single<Clock> { SystemClock() }

    single<DukanDatabase> {
        get<DatabaseFactory>()
            .builder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<AppDispatchers>().io)
            .addMigrations(*DUKAN_MIGRATIONS)
            .build()
    }

    single { get<DukanDatabase>().productDao() }
    single { get<DukanDatabase>().customerDao() }
    single { get<DukanDatabase>().saleDao() }
    single { get<DukanDatabase>().purchaseDao() }
    single { get<DukanDatabase>().returnDao() }
    single { get<DukanDatabase>().backupDao() }
    single { get<DukanDatabase>().reportDao() }
    single { get<DukanDatabase>().settingsDao() }

    single<ProductRepository> { ProductRepositoryImpl(get(), get(), get()) }
    single<SaleRepository> { SaleRepositoryImpl(get(), get()) }
    single<CustomerRepository> { CustomerRepositoryImpl(get(), get(), get()) }
    single<ReportRepository> { ReportRepositoryImpl(get(), get(), get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
    single<PurchaseRepository> { PurchaseRepositoryImpl(get(), get(), get()) }
    single<ReturnRepository> { ReturnRepositoryImpl(get(), get(), get()) }
    single<BackupRepository> { BackupRepositoryImpl(get(), get()) }
}
