package com.dukan.pos.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.Pill
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.core.designsystem.StatTile
import com.dukan.pos.core.i18n.fmt
import com.dukan.pos.domain.model.DailyPoint
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenPos: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenKhata: () -> Unit,
    onOpenPurchase: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.settings.shopName) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = strings.settings)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = spacing.screenPadding,
                end = spacing.screenPadding,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                // The headline number: what the shop took today.
                Column {
                    Text(
                        strings.todaysSale,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.xs))
                    MoneyText(state.snapshot.today.grossSales, style = MoneyStyles.counter)
                    Spacer(Modifier.height(spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Pill(strings.salesCount.fmt(state.snapshot.today.salesCount.toString()))
                        Pill(
                            strings.profitPill.fmt(
                                state.snapshot.today.profit.format(state.settings.currencySymbol),
                            ),
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            item { WeekBars(state.snapshot.lastSevenDays) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    QuickAction(strings.newSale, Icons.Rounded.PointOfSale, Modifier.weight(1f), onOpenPos)
                    QuickAction(strings.purchases, Icons.Rounded.LocalShipping, Modifier.weight(1f), onOpenPurchase)
                    QuickAction(strings.salesHistory, Icons.Rounded.ReceiptLong, Modifier.weight(1f), onOpenSalesHistory)
                }
            }

            item { SectionHeader(strings.shopHealth) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile(
                        label = strings.udhaarOutstanding,
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.error,
                        value = {
                            MoneyText(
                                state.snapshot.totalUdhaarOutstanding,
                                style = MoneyStyles.large,
                                emphasizeNegative = false,
                            )
                        },
                    )
                    StatTile(
                        label = strings.stockValue,
                        modifier = Modifier.weight(1f),
                        value = {
                            MoneyText(state.snapshot.stockValueAtCost, style = MoneyStyles.large)
                        },
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile(
                        label = strings.lowStock,
                        modifier = Modifier.weight(1f).clickable(onClick = onOpenProducts),
                        accent = MaterialTheme.colorScheme.secondary,
                        value = { Text("${state.snapshot.lowStockCount}", style = MoneyStyles.large) },
                    )
                    StatTile(
                        label = strings.outOfStock,
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.error,
                        value = { Text("${state.snapshot.outOfStockCount}", style = MoneyStyles.large) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Seven bars, no axes, no legend. On a phone at a counter the only question is
 * "was today better than the last few days" — the shape answers it.
 */
@Composable
private fun WeekBars(points: List<DailyPoint>) {
    if (points.isEmpty()) return
    val max = points.maxOf { it.sales.paisa }.coerceAtLeast(1L)
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current

    Column {
        SectionHeader(strings.lastSevenDays)
        Spacer(Modifier.height(spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { point ->
                val ratio = (point.sales.paisa.toFloat() / max).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(ratio)
                        .background(
                            if (point.sales == Money.ZERO) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        ),
                )
            }
        }
    }
}
