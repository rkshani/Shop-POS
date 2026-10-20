package com.dukan.pos.data.repository

import com.dukan.pos.core.common.AppDispatchers
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.Money
import com.dukan.pos.core.common.endOfDayExclusive
import com.dukan.pos.core.common.plusDays
import com.dukan.pos.core.common.startOfDay
import com.dukan.pos.core.common.toLocalDate
import com.dukan.pos.data.local.dao.CustomerDao
import com.dukan.pos.data.local.dao.ProductDao
import com.dukan.pos.data.local.dao.ReportDao
import com.dukan.pos.domain.model.DailyPoint
import com.dukan.pos.domain.model.DashboardSnapshot
import com.dukan.pos.domain.model.SalesSummary
import com.dukan.pos.domain.model.TopProduct
import com.dukan.pos.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

class ReportRepositoryImpl(
    private val reportDao: ReportDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ReportRepository {

    override fun observeDashboard(): Flow<DashboardSnapshot> {
        val zone = clock.timeZone()
        val today = clock.now().toLocalDate(zone)
        val weekStart = today.plusDays(-6)

        return combine(
            observeSummary(today, today),
            observeDailySeries(weekStart, today),
            productDao.observeLowStockCount(),
            productDao.observeOutOfStockCount(),
            combine(customerDao.observeTotalOutstanding(), productDao.observeStockValueAtCost()) { a, b -> a to b },
        ) { todaySummary, series, lowStock, outOfStock, (outstanding, stockValue) ->
            DashboardSnapshot(
                today = todaySummary,
                lowStockCount = lowStock,
                outOfStockCount = outOfStock,
                totalUdhaarOutstanding = Money(outstanding),
                stockValueAtCost = Money(stockValue),
                lastSevenDays = series,
            )
        }.flowOn(dispatchers.io)
    }

    override fun observeSummary(from: LocalDate, until: LocalDate): Flow<SalesSummary> {
        val zone = clock.timeZone()
        val start = from.startOfDay(zone).toEpochMilliseconds()
        val end = until.endOfDayExclusive(zone).toEpochMilliseconds()

        return combine(
            reportDao.observeSummary(start, end),
            reportDao.observeGrossProfit(start, end),
            reportDao.observeCashCollected(start, end),
            reportDao.observeUdhaarAdded(start, end),
            combine(
                reportDao.observeReturnedAmount(start, end),
                reportDao.observeReturnedProfit(start, end),
            ) { amount, profit -> amount to profit },
        ) { row, grossProfit, cash, udhaar, (returnedAmount, returnedProfit) ->
            // Returns are netted off here rather than in a separate figure:
            // a shopkeeper asking "aaj kitna hua" means after returns.
            SalesSummary(
                salesCount = row.salesCount,
                grossSales = Money(row.grossPaisa - returnedAmount),
                discountGiven = Money(row.discountPaisa),
                cashCollected = Money(cash - returnedAmount),
                udhaarAdded = Money(udhaar),
                profit = Money(grossProfit - row.discountPaisa - returnedProfit),
                returnedAmount = Money(returnedAmount),
            )
        }.flowOn(dispatchers.io)
    }

    override fun observeDailySeries(from: LocalDate, until: LocalDate): Flow<List<DailyPoint>> {
        val zone = clock.timeZone()
        val startInstant = from.startOfDay(zone)
        val endInstant = until.endOfDayExclusive(zone)
        // Day bucketing happens in SQL, so SQLite needs the local offset.
        val offsetMillis = zone.offsetAt(startInstant).totalSeconds * 1000L

        return reportDao.observeDailySeries(
            from = startInstant.toEpochMilliseconds(),
            until = endInstant.toEpochMilliseconds(),
            tzOffsetMillis = offsetMillis,
        ).map { rows ->
            val byDate = rows.associateBy { LocalDate.fromEpochDays(it.dayEpoch.toInt()) }
            buildList {
                var cursor = from
                while (cursor <= until) {
                    val row = byDate[cursor]
                    add(
                        DailyPoint(
                            date = cursor,
                            sales = Money(row?.salesPaisa ?: 0L),
                            profit = Money(row?.profitPaisa ?: 0L),
                        ),
                    )
                    cursor = cursor.plusDays(1)
                }
            }
        }.flowOn(dispatchers.io)
    }

    override fun observeTopProducts(from: LocalDate, until: LocalDate, limit: Int): Flow<List<TopProduct>> {
        val zone = clock.timeZone()
        return reportDao.observeTopProducts(
            from = from.startOfDay(zone).toEpochMilliseconds(),
            until = until.endOfDayExclusive(zone).toEpochMilliseconds(),
            limit = limit,
        ).map { rows ->
            rows.map {
                TopProduct(
                    productId = it.productId,
                    name = it.name,
                    qtySold = it.qtySold,
                    revenue = Money(it.revenuePaisa),
                    profit = Money(it.profitPaisa),
                )
            }
        }.flowOn(dispatchers.io)
    }
}
