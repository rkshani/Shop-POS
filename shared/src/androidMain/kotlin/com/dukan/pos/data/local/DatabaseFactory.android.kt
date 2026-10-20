package com.dukan.pos.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun builder(): RoomDatabase.Builder<DukanDatabase> {
        val file = context.getDatabasePath(DATABASE_NAME)
        return Room.databaseBuilder<DukanDatabase>(
            context = context.applicationContext,
            name = file.absolutePath,
        )
    }
}
