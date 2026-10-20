package com.dukan.pos.core.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatchers.IO does not exist in commonMain, so each platform supplies it.
 * Injected everywhere rather than referenced statically, so tests can swap it.
 */
expect val platformIoDispatcher: CoroutineDispatcher

interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
