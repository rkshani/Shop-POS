package com.dukan.pos.data.local

import androidx.room.RoomDatabase

/**
 * Each platform knows where its app-private storage lives; the shared code
 * only needs a configured builder back.
 */
expect class DatabaseFactory {
    fun builder(): RoomDatabase.Builder<DukanDatabase>
}
