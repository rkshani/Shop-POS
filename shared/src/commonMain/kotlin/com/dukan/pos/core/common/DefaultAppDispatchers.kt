package com.dukan.pos.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = platformIoDispatcher
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
