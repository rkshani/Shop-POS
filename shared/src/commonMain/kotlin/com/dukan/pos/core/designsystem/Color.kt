package com.dukan.pos.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palette taken from the counter itself: the painted green of a shop board,
 * the brass of a taraazu, the grey of a thermal receipt.
 * Purple-tinted Material defaults would read as "some app"; this reads as a shop.
 */
object DukanColors {
    val BazaarGreen = Color(0xFF1F6B4A)
    val BazaarGreenDark = Color(0xFF125136)
    val BazaarGreenSoft = Color(0xFFDCEDE3)

    val Brass = Color(0xFFA96A00)
    val BrassSoft = Color(0xFFF7E9CF)

    val Ink = Color(0xFF16181A)
    val Slate = Color(0xFF5E6468)
    val Outline = Color(0xFFDBD9D2)

    val Paper = Color(0xFFFBFBF9)
    val PaperMuted = Color(0xFFF0EFEA)

    val Due = Color(0xFFB3261E)
    val DueSoft = Color(0xFFFBE4E2)

    val NightSurface = Color(0xFF14161A)
    val NightElevated = Color(0xFF1E2126)
    val NightOutline = Color(0xFF343A40)
    val NightText = Color(0xFFECEDEE)
    val MintOnNight = Color(0xFF7FD1A8)
    val BrassOnNight = Color(0xFFE0B461)
}

internal val DukanLightScheme = lightColorScheme(
    primary = DukanColors.BazaarGreen,
    onPrimary = Color.White,
    primaryContainer = DukanColors.BazaarGreenSoft,
    onPrimaryContainer = DukanColors.BazaarGreenDark,
    secondary = DukanColors.Brass,
    onSecondary = Color.White,
    secondaryContainer = DukanColors.BrassSoft,
    onSecondaryContainer = Color(0xFF4A2F00),
    error = DukanColors.Due,
    onError = Color.White,
    errorContainer = DukanColors.DueSoft,
    onErrorContainer = Color(0xFF5C0F0B),
    background = DukanColors.Paper,
    onBackground = DukanColors.Ink,
    surface = DukanColors.Paper,
    onSurface = DukanColors.Ink,
    surfaceVariant = DukanColors.PaperMuted,
    onSurfaceVariant = DukanColors.Slate,
    outline = DukanColors.Outline,
    outlineVariant = Color(0xFFE8E6DF),
)

internal val DukanDarkScheme = darkColorScheme(
    primary = DukanColors.MintOnNight,
    onPrimary = Color(0xFF00301C),
    primaryContainer = Color(0xFF16452F),
    onPrimaryContainer = Color(0xFFB6EBCE),
    secondary = DukanColors.BrassOnNight,
    onSecondary = Color(0xFF3A2500),
    secondaryContainer = Color(0xFF4E3512),
    onSecondaryContainer = Color(0xFFF7DCA9),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0F0B),
    background = DukanColors.NightSurface,
    onBackground = DukanColors.NightText,
    surface = DukanColors.NightSurface,
    onSurface = DukanColors.NightText,
    surfaceVariant = DukanColors.NightElevated,
    onSurfaceVariant = Color(0xFFB6BCC1),
    outline = DukanColors.NightOutline,
    outlineVariant = Color(0xFF2A2F35),
)
