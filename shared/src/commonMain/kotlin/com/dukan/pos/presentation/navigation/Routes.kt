package com.dukan.pos.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Dashboard : Route
    @Serializable data object Pos : Route
    @Serializable data object Products : Route
    @Serializable data class ProductEdit(val productId: Long = 0L) : Route
    @Serializable data object Khata : Route
    @Serializable data class CustomerDetail(val customerId: Long) : Route
    @Serializable data object Reports : Route
    @Serializable data object Settings : Route
    @Serializable data object SalesHistory : Route
    @Serializable data class SaleDetail(val saleId: Long) : Route
    @Serializable data object Purchase : Route
}

/** Labels live in [com.dukan.pos.core.i18n.Strings], not here — this is structure only. */
enum class TopLevelDestination(
    val route: Route,
    val icon: ImageVector,
) {
    DASHBOARD(Route.Dashboard, Icons.Rounded.Storefront),
    POS(Route.Pos, Icons.Rounded.PointOfSale),
    PRODUCTS(Route.Products, Icons.Rounded.Inventory2),
    KHATA(Route.Khata, Icons.Rounded.ReceiptLong),
    REPORTS(Route.Reports, Icons.Rounded.QueryStats),
}
