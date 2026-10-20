package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.data.local.dao.SettingsDao
import com.dukan.pos.data.mapper.toDomain
import com.dukan.pos.data.mapper.toEntity
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val dao: SettingsDao,
    private val dispatchers: AppDispatchers,
) : SettingsRepository {

    override fun observeSettings(): Flow<ShopSettings> =
        dao.observeSettings().map { it?.toDomain() ?: ShopSettings() }.flowOn(dispatchers.io)

    override suspend fun get(): ShopSettings = withContext(dispatchers.io) {
        dao.get()?.toDomain() ?: ShopSettings()
    }

    override suspend fun update(settings: ShopSettings) = withContext(dispatchers.io) {
        dao.upsert(settings.toEntity())
    }
}
