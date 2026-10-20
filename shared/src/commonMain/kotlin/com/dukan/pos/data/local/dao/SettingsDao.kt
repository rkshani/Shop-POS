package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dukan.pos.data.local.entity.ShopSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM shop_settings WHERE id = 1")
    fun observeSettings(): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE id = 1")
    suspend fun get(): ShopSettingsEntity?

    @Upsert
    suspend fun upsert(settings: ShopSettingsEntity)
}
