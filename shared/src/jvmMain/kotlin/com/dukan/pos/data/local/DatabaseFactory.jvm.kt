package com.dukan.pos.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun builder(): RoomDatabase.Builder<DukanDatabase> {
        val directory = File(System.getProperty("user.home"), ".dukanpos").apply { mkdirs() }
        return Room.databaseBuilder<DukanDatabase>(
            name = File(directory, DATABASE_NAME).absolutePath,
        )
    }
}
