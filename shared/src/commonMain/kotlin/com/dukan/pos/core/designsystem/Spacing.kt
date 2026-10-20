package com.dukan.pos.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Minimum tap target for a hand holding a shopping bag. */
    val tapTarget: Dp = 48.dp,
    val screenPadding: Dp = 16.dp,
    val cardRadius: Dp = 14.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
