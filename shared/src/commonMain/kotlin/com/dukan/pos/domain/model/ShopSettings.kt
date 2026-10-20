package com.dukan.pos.domain.model

data class ShopSettings(
    val shopName: String = "My Dukan",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val currencySymbol: String = "Rs",
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val allowNegativeStock: Boolean = false,
    val receiptFooter: String = "Shukriya! Dobara tashreef laiye.",
    val invoicePrefix: String = "INV",
)

enum class AppLanguage(val tag: String) { ENGLISH("en"), URDU("ur") }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
