package com.dukan.pos.domain.repository

interface BackupRepository {
    /** Full JSON snapshot — the thing that must be exported regularly. */
    suspend fun exportJson(): String

    /** Human-readable exports for an accountant, one file per sheet. */
    suspend fun exportProductsCsv(): String
    suspend fun exportSalesCsv(): String
    suspend fun exportKhataCsv(): String

    /** Replaces everything on this device. Throws if the file is not a valid backup. */
    suspend fun restoreJson(json: String)
}
