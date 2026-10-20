package com.dukan.pos.di

import com.dukan.pos.domain.usecase.AddScannedProductUseCase
import com.dukan.pos.domain.usecase.RecordPurchaseUseCase
import com.dukan.pos.domain.usecase.RecordSaleReturnUseCase
import com.dukan.pos.domain.usecase.SendUdhaarReminderUseCase
import com.dukan.pos.domain.usecase.CheckoutUseCase
import com.dukan.pos.domain.usecase.ObserveDashboardUseCase
import com.dukan.pos.domain.usecase.ObserveProductsUseCase
import com.dukan.pos.domain.usecase.RecordCustomerPaymentUseCase
import com.dukan.pos.domain.usecase.SaveProductUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { CheckoutUseCase(get(), get(), get()) }
    factory { SaveProductUseCase(get(), get()) }
    factory { AddScannedProductUseCase(get()) }
    factory { RecordCustomerPaymentUseCase(get()) }
    factory { ObserveDashboardUseCase(get()) }
    factory { ObserveProductsUseCase(get()) }
    factory { RecordPurchaseUseCase(get(), get()) }
    factory { RecordSaleReturnUseCase(get(), get()) }
    factory { SendUdhaarReminderUseCase(get(), get()) }
}
