package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.export.buildCsv
import com.dukan.pos.data.local.dao.BackupDao
import com.dukan.pos.data.local.dao.DatabaseSnapshot
import com.dukan.pos.domain.repository.BackupRepository
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

class BackupRepositoryImpl(
    private val dao: BackupDao,
    private val dispatchers: AppDispatchers,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : BackupRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportJson(): String = withContext(dispatchers.io) {
        json.encodeToString(DatabaseSnapshot.serializer(), dao.snapshot().withDefaultSettings())
    }

    override suspend fun restoreJson(content: String) = withContext(dispatchers.io) {
        val snapshot = json.decodeFromString(DatabaseSnapshot.serializer(), content)
        validateRestoreSnapshot(snapshot)
        dao.replaceAll(snapshot.withDefaultSettings())
    }

    override suspend fun exportProductsCsv(): String = withContext(dispatchers.io) {
        buildCsv(
            header = listOf("Name", "Urdu name", "Barcode", "Unit", "Cost", "Price", "Stock", "Low stock at"),
            rows = dao.products().map { p ->
                listOf(
                    p.name,
                    p.nameUrdu,
                    p.barcode,
                    p.unit,
                    Money(p.costPaisa).format(""),
                    Money(p.salePaisa).format(""),
                    p.stockQty.toString(),
                    p.lowStockAt.toString(),
                )
            },
        )
    }

    override suspend fun exportSalesCsv(): String = withContext(dispatchers.io) {
        val itemsBySale = dao.saleItems().groupBy { it.saleId }
        buildCsv(
            header = listOf("Invoice", "Date", "Customer", "Item", "Qty", "Unit price", "Line total", "Payment", "Invoice total", "Paid"),
            rows = dao.sales().sortedBy { it.createdAtMillis }.flatMap { sale ->
                val items = itemsBySale[sale.id].orEmpty()
                if (items.isEmpty()) {
                    listOf(
                        listOf(
                            sale.invoiceNo, sale.createdAtMillis.asDate(), sale.customerName,
                            null, null, null, null,
                            sale.paymentMethod, Money(sale.totalPaisa).format(""), Money(sale.paidPaisa).format(""),
                        ),
                    )
                } else {
                    items.map { item ->
                        listOf(
                            sale.invoiceNo,
                            sale.createdAtMillis.asDate(),
                            sale.customerName,
                            item.productName,
                            item.qty.toString(),
                            Money(item.unitPricePaisa).format(""),
                            Money(item.unitPricePaisa).times(item.qty).format(""),
                            sale.paymentMethod,
                            Money(sale.totalPaisa).format(""),
                            Money(sale.paidPaisa).format(""),
                        )
                    }
                }
            },
        )
    }

    override suspend fun exportKhataCsv(): String = withContext(dispatchers.io) {
        val customers = dao.customers().associateBy { it.id }
        buildCsv(
            header = listOf("Customer", "Phone", "Date", "Type", "Amount", "Balance after", "Note"),
            rows = dao.ledgerEntries().sortedBy { it.createdAtMillis }.map { entry ->
                val customer = customers[entry.customerId]
                listOf(
                    customer?.name,
                    customer?.phone,
                    entry.createdAtMillis.asDate(),
                    entry.type,
                    Money(entry.amountPaisa).format(""),
                    Money(entry.balanceAfterPaisa).format(""),
                    entry.note,
                )
            },
        )
    }

    private fun Long.asDate(): String {
        val dateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)
        return "${dateTime.date} ${dateTime.hour.pad()}:${dateTime.minute.pad()}"
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
