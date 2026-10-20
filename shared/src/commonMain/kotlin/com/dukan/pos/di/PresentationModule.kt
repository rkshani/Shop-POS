package com.dukan.pos.di

import com.dukan.pos.presentation.dashboard.DashboardViewModel
import com.dukan.pos.presentation.khata.CustomerDetailViewModel
import com.dukan.pos.presentation.khata.KhataViewModel
import com.dukan.pos.presentation.pos.PosViewModel
import com.dukan.pos.presentation.products.ProductEditViewModel
import com.dukan.pos.presentation.products.ProductsViewModel
import com.dukan.pos.presentation.purchase.PurchaseViewModel
import com.dukan.pos.presentation.reports.ReportsViewModel
import com.dukan.pos.presentation.sales.SaleDetailViewModel
import com.dukan.pos.presentation.sales.SalesHistoryViewModel
import com.dukan.pos.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { PosViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProductsViewModel(get(), get()) }
    viewModel { params -> ProductEditViewModel(productId = params.get(), get(), get(), get()) }
    viewModel { KhataViewModel(get()) }
    viewModel { params -> CustomerDetailViewModel(customerId = params.get(), get(), get(), get(), get()) }
    viewModel { ReportsViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { PurchaseViewModel(get(), get(), get()) }
    viewModel { SalesHistoryViewModel(get()) }
    viewModel { params -> SaleDetailViewModel(saleId = params.get(), get(), get(), get()) }
}
