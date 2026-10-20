package com.dukan.pos.di

import com.dukan.pos.core.platform.IosPlatformActions
import com.dukan.pos.core.platform.PlatformActions
import com.dukan.pos.data.local.DatabaseFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseFactory() }
    single<PlatformActions> { IosPlatformActions() }
}
