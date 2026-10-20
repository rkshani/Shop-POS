package com.dukan.pos.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Two roles only. Body text uses the platform face so Urdu renders with the
 * system's own Nastaliq/Naskh fallback. Every number — price, qty, balance —
 * uses a monospaced face so digits line up in columns and the eye can compare
 * a khata down the page without reading it.
 */
val NumericFamily: FontFamily = FontFamily.Monospace

val DukanTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

object MoneyStyles {
    val counter = TextStyle(fontFamily = NumericFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-1).sp)
    val large = TextStyle(fontFamily = NumericFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.4).sp)
    val medium = TextStyle(fontFamily = NumericFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val small = TextStyle(fontFamily = NumericFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
}
