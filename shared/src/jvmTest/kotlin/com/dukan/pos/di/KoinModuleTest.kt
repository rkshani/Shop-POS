package com.dukan.pos.di

import com.dukan.pos.domain.repository.BackupRepository
import com.dukan.pos.presentation.settings.SettingsViewModel
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun verifyModulesCanBeInitialized() {
        val app = initKoin {}
        val koin = app.koin
        assertNotNull(koin.getOrNull<BackupRepository>())
        assertNotNull(koin.getOrNull<SettingsViewModel>())
    }
}
