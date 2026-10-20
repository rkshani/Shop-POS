package com.dukan.pos.presentation.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.common.SystemClock
import com.dukan.pos.core.common.formatShort
import com.dukan.pos.core.designsystem.EmptyState
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.MoneyStyles
import com.dukan.pos.core.designsystem.MoneyText
import com.dukan.pos.core.designsystem.Pill
import com.dukan.pos.domain.model.Sale
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    onBack: () -> Unit,
    onOpenSale: (Long) -> Unit,
    viewModel: SalesHistoryViewModel = koinViewModel(),
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current
    val zone = remember { SystemClock().timeZone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.salesHistory) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        if (sales.isEmpty()) {
            EmptyState(
                title = strings.noSalesYet,
                message = strings.noSalesYetBody,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = spacing.xxl),
        ) {
            items(sales, key = { it.id }) { sale ->
                SaleRow(sale, zone) { onOpenSale(sale.id) }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, zone: kotlinx.datetime.TimeZone, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(sale.invoiceNo, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(sale.createdAt.toLocalDateTime(zone).formatShort())
                    sale.customerName?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MoneyText(sale.total, style = MoneyStyles.medium)
            if (sale.due.isPositive) {
                Pill(
                    "${strings.due} ${sale.due.format("")}",
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else if (sale.hasReturns) {
                Pill(strings.returnTitle)
            }
        }
    }
}
