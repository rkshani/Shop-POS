package com.dukan.pos.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.dukan.pos.core.i18n.Strings
import com.dukan.pos.domain.model.AppLanguage
import com.dukan.pos.domain.model.ThemeMode

val LocalCurrencySymbol = staticCompositionLocalOf { "Rs" }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalStrings = staticCompositionLocalOf { Strings.English }

private val DukanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DukanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.ENGLISH,
    currencySymbol: String = "Rs",
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val direction = if (language == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalCurrencySymbol provides currencySymbol,
        LocalAppLanguage provides language,
        LocalStrings provides if (language == AppLanguage.URDU) Strings.Urdu else Strings.English,
        LocalLayoutDirection provides direction,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DukanDarkScheme else DukanLightScheme,
            typography = DukanTypography,
            shapes = DukanShapes,
            content = content,
        )
    }
}
