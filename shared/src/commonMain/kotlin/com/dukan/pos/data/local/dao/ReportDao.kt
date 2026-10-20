package com.dukan.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.dukan.pos.data.local.entity.DailyRow
import com.dukan.pos.data.local.entity.MoneyFlowRow
import com.dukan.pos.data.local.entity.SummaryRow
import com.dukan.pos.data.local.entity.TopProductRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query(
        """
        SELECT COUNT(*) AS salesCount,
               CAST(COALESCE(SUM(totalPaisa), 0) AS INTEGER) AS grossPaisa,
               CAST(COALESCE(SUM(discountPaisa), 0) AS INTEGER) AS discountPaisa,
               CAST(COALESCE(SUM(paidPaisa), 0) AS INTEGER) AS paidPaisa
        FROM sales
        WHERE createdAtMillis >= :from AND createdAtMillis < :until
        """,
    )
    fun observeSummary(from: Long, until: Long): Flow<SummaryRow>

    @Query(
        """
        SELECT CAST(COALESCE(SUM(
                   (i.unitPricePaisa - i.unitCostPaisa) * i.qty
               ), 0) AS INTEGER)
        FROM sale_items i
        INNER JOIN sales s ON s.id = i.saleId
        WHERE s.createdAtMillis >= :from AND s.createdAtMillis < :until
        """,
    )
    fun observeGrossProfit(from: Long, until: Long): Flow<Long>

    @Query(
        """
        SELECT dayEpoch,
               CAST(SUM(totalPaisa) AS INTEGER) AS salesPaisa,
               CAST(SUM(profitPaisa) AS INTEGER) AS profitPaisa
        FROM (
            SELECT (s.createdAtMillis + :tzOffsetMillis) / 86400000 AS dayEpoch,
                   s.totalPaisa AS totalPaisa,
                   (COALESCE((
                        SELECT SUM((i.unitPricePaisa - i.unitCostPaisa) * i.qty)
                        FROM sale_items i WHERE i.saleId = s.id
                   ), 0) - s.discountPaisa) AS profitPaisa
            FROM sales s
            WHERE s.createdAtMillis >= :from AND s.createdAtMillis < :until
        )
        GROUP BY dayEpoch
        ORDER BY dayEpoch ASC
        """,
    )
    fun observeDailySeries(from: Long, until: Long, tzOffsetMillis: Long): Flow<List<DailyRow>>

    @Query(
        """
        SELECT i.productId AS productId,
               i.productName AS name,
               SUM(i.qty) AS qtySold,
               CAST(SUM(i.unitPricePaisa * i.qty) AS INTEGER) AS revenuePaisa,
               CAST(SUM((i.unitPricePaisa - i.unitCostPaisa) * i.qty) AS INTEGER) AS profitPaisa
        FROM sale_items i
        INNER JOIN sales s ON s.id = i.saleId
        WHERE s.createdAtMillis >= :from AND s.createdAtMillis < :until
        GROUP BY i.productId, i.productName
        ORDER BY revenuePaisa DESC
        LIMIT :limit
        """,
    )
    fun observeTopProducts(from: Long, until: Long, limit: Int): Flow<List<TopProductRow>>

    @Query(
        """
        SELECT CAST(COALESCE(SUM(totalPaisa - paidPaisa), 0) AS INTEGER)
        FROM sales
        WHERE createdAtMillis >= :from AND createdAtMillis < :until AND totalPaisa > paidPaisa
        """,
    )
    fun observeUdhaarAdded(from: Long, until: Long): Flow<Long>

    /**
     * Where the money went, in one read so the figures can never disagree.
     *
     * - Cash: what was paid at the counter on CASH and UDHAAR sales (a part
     *   payment on an udhaar sale is handed over in cash), plus khata payments
     *   received, minus CASH refunds. KHATA refunds never touch the drawer.
     * - Digital: what was paid on Easypaisa / JazzCash / Bank / Card sales.
     * - Returned cost: the cost snapshot of returned items, so returned profit
     *   is the refund actually given minus that cost — a discounted invoice's
     *   discount is not taken off profit a second time.
     */
    @Query(
        """
        SELECT
            (SELECT CAST(COALESCE(SUM(paidPaisa), 0) AS INTEGER) FROM sales
             WHERE createdAtMillis >= :from AND createdAtMillis < :until
               AND paymentMethod IN ('CASH', 'UDHAAR')) AS cashSalesPaisa,
            (SELECT CAST(COALESCE(SUM(paidPaisa), 0) AS INTEGER) FROM sales
             WHERE createdAtMillis >= :from AND createdAtMillis < :until
               AND paymentMethod NOT IN ('CASH', 'UDHAAR')) AS digitalPaisa,
            (SELECT CAST(COALESCE(-SUM(amountPaisa), 0) AS INTEGER) FROM ledger_entries
             WHERE createdAtMillis >= :from AND createdAtMillis < :until
               AND type = 'PAYMENT') AS khataCollectedPaisa,
            (SELECT CAST(COALESCE(SUM(refundPaisa), 0) AS INTEGER) FROM sale_returns
             WHERE createdAtMillis >= :from AND createdAtMillis < :until) AS returnedPaisa,
            (SELECT CAST(COALESCE(SUM(refundPaisa), 0) AS INTEGER) FROM sale_returns
             WHERE createdAtMillis >= :from AND createdAtMillis < :until
               AND refundMethod = 'CASH') AS cashRefundedPaisa,
            (SELECT CAST(COALESCE(SUM(i.unitCostPaisa * i.qty), 0) AS INTEGER)
             FROM sale_return_items i
             INNER JOIN sale_returns r ON r.id = i.returnId
             WHERE r.createdAtMillis >= :from AND r.createdAtMillis < :until) AS returnedCostPaisa
        """,
    )
    fun observeMoneyFlow(from: Long, until: Long): Flow<MoneyFlowRow>
}
