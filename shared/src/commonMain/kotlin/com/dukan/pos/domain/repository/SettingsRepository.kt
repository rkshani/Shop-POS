package com.dukan.pos.domain.repository

import com.dukan.pos.domain.model.ShopSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<ShopSettings>
    suspend fun get(): ShopSettings
    suspend fun update(settings: ShopSettings)
}
