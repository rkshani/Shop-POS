package com.dukan.pos.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.presentation.dashboard.DashboardScreen
import com.dukan.pos.presentation.khata.CustomerDetailScreen
import com.dukan.pos.presentation.khata.KhataScreen
import com.dukan.pos.presentation.pos.PosScreen
import com.dukan.pos.presentation.products.ProductEditScreen
import com.dukan.pos.presentation.products.ProductsScreen
import com.dukan.pos.presentation.purchase.PurchaseScreen
import com.dukan.pos.presentation.sales.SaleDetailScreen
import com.dukan.pos.presentation.sales.SalesHistoryScreen
import com.dukan.pos.presentation.reports.ReportsScreen
import com.dukan.pos.presentation.settings.SettingsScreen

@Composable
fun DukanNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val strings = LocalStrings.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { destination ->
        currentDestination?.hierarchyMatches(destination) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchyMatches(destination) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Route.Dashboard) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = {
                                Text(
                                    when (destination) {
                                        TopLevelDestination.DASHBOARD -> strings.navShop
                                        TopLevelDestination.POS -> strings.navSell
                                        TopLevelDestination.PRODUCTS -> strings.navStock
                                        TopLevelDestination.KHATA -> strings.navKhata
                                        TopLevelDestination.REPORTS -> strings.navReports
                                    },
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable<Route.Dashboard> {
                DashboardScreen(
                    onOpenPos = { navController.navigate(Route.Pos) },
                    onOpenProducts = { navController.navigate(Route.Products) },
                    onOpenKhata = { navController.navigate(Route.Khata) },
                    onOpenPurchase = { navController.navigate(Route.Purchase) },
                    onOpenSalesHistory = { navController.navigate(Route.SalesHistory) },
                    onOpenSettings = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Pos> {
                PosScreen(
                    onAddProduct = { navController.navigate(Route.ProductEdit()) },
                )
            }
            composable<Route.Products> {
                ProductsScreen(
                    onAddProduct = { navController.navigate(Route.ProductEdit()) },
                    onEditProduct = { id -> navController.navigate(Route.ProductEdit(id)) },
                )
            }
            composable<Route.ProductEdit> { entry ->
                val route: Route.ProductEdit = entry.toRoute()
                ProductEditScreen(
                    productId = route.productId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable<Route.Khata> {
                KhataScreen(
                    onOpenCustomer = { id -> navController.navigate(Route.CustomerDetail(id)) },
                )
            }
            composable<Route.CustomerDetail> { entry ->
                val route: Route.CustomerDetail = entry.toRoute()
                CustomerDetailScreen(
                    customerId = route.customerId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.SalesHistory> {
                SalesHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSale = { id -> navController.navigate(Route.SaleDetail(id)) },
                )
            }
            composable<Route.SaleDetail> { entry ->
                val route: Route.SaleDetail = entry.toRoute()
                SaleDetailScreen(
                    saleId = route.saleId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.Purchase> {
                PurchaseScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.Reports> {
                ReportsScreen(onOpenSalesHistory = { navController.navigate(Route.SalesHistory) })
            }
            composable<Route.Settings> { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun androidx.navigation.NavDestination.hierarchyMatches(destination: TopLevelDestination): Boolean =
    when (destination) {
        TopLevelDestination.DASHBOARD -> hasRoute(Route.Dashboard::class)
        TopLevelDestination.POS -> hasRoute(Route.Pos::class)
        TopLevelDestination.PRODUCTS -> hasRoute(Route.Products::class)
        TopLevelDestination.KHATA -> hasRoute(Route.Khata::class)
        TopLevelDestination.REPORTS -> hasRoute(Route.Reports::class)
    }
