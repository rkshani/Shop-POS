package com.dukan.pos.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun builder(): RoomDatabase.Builder<DukanDatabase> {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Could not resolve the app documents directory")

        val path = requireNotNull(documents.URLByAppendingPathComponent(DATABASE_NAME)?.path) {
            "Could not build the database path"
        }
        return Room.databaseBuilder<DukanDatabase>(name = path)
    }
}
