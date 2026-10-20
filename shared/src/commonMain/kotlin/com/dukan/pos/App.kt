package com.dukan.pos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.designsystem.DukanTheme
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.SettingsRepository
import com.dukan.pos.presentation.navigation.DukanNavHost
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun DukanApp() {
    KoinContext {
        val settingsRepository: SettingsRepository = koinInject()
        val settings by settingsRepository.observeSettings()
            .collectAsStateWithLifecycle(initialValue = ShopSettings())

        DukanTheme(
            themeMode = settings.theme,
            language = settings.language,
            currencySymbol = settings.currencySymbol,
        ) {
            DukanNavHost()
        }
    }
}
