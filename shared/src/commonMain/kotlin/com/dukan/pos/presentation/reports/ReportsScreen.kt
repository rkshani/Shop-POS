package com.dukan.pos.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.formatQty
import com.dukan.pos.core.designsystem.EmptyState
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.core.designsystem.StatTile
import com.dukan.pos.domain.model.DailyPoint
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onOpenSalesHistory: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel(),
) {
    val range by viewModel.range.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()
    val topProducts by viewModel.topProducts.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navReports) },
                actions = {
                    TextButton(onClick = onOpenSalesHistory) { Text(strings.salesHistory) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = spacing.screenPadding, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ReportRange.entries.forEach { option ->
                        FilterChip(
                            selected = range == option,
                            onClick = { viewModel.onRangeSelected(option) },
                            label = {
                                Text(
                                    when (option) {
                                        ReportRange.TODAY -> strings.rangeToday
                                        ReportRange.WEEK -> strings.rangeWeek
                                        ReportRange.MONTH -> strings.rangeMonth
                                    },
                                )
                            },
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        strings.netSales,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyText(summary.grossSales, style = MoneyStyles.counter)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile(strings.profitPill.replace(" %s", ""), Modifier.weight(1f)) {
                        MoneyText(summary.profit, style = MoneyStyles.large)
                    }
                    StatTile(strings.salesCount.replace(" %s", "").replace("%s ", ""), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary) {
                        Text("${summary.salesCount}", style = MoneyStyles.large)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile(strings.cash, Modifier.weight(1f)) {
                        MoneyText(summary.cashCollected, style = MoneyStyles.large)
                    }
                    StatTile(strings.udhaar, Modifier.weight(1f), accent = MaterialTheme.colorScheme.error) {
                        MoneyText(summary.udhaarAdded, style = MoneyStyles.large, emphasizeNegative = false)
                    }
                }
            }

            if (summary.returnedAmount.isPositive) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.returnedInRange, style = MaterialTheme.typography.bodyMedium)
                        MoneyText(
                            summary.returnedAmount,
                            style = MoneyStyles.medium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (series.size > 1) {
                item {
                    Column {
                        SectionHeader(strings.lastSevenDays)
                        Spacer(Modifier.height(spacing.md))
                        DailyBars(series)
                    }
                }
            }

            item { SectionHeader(strings.topSelling) }

            if (topProducts.isEmpty()) {
                item {
                    EmptyState(
                        title = strings.noSalesYet,
                        message = strings.noSalesYetBody,
                    )
                }
            } else {
                items(topProducts, key = { it.productId }) { product ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${product.qtySold.formatQty()} · ${product.profit.format("")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(product.revenue, style = MoneyStyles.medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBars(points: List<DailyPoint>) {
    val max = points.maxOf { it.sales.paisa }.coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val ratio = (point.sales.paisa.toFloat() / max).coerceIn(0.03f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(ratio)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                    ),
            )
        }
    }
}
