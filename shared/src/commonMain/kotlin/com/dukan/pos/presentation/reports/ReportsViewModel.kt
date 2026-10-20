package com.dukan.pos.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.common.Clock
import com.dukan.pos.core.common.plusDays
import com.dukan.pos.core.common.toLocalDate
import com.dukan.pos.domain.model.DailyPoint
import com.dukan.pos.domain.model.SalesSummary
import com.dukan.pos.domain.model.TopProduct
import com.dukan.pos.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

enum class ReportRange(val days: Int) {
    TODAY(0),
    WEEK(6),
    MONTH(29),
}

class ReportsViewModel(
    private val repository: ReportRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _range = MutableStateFlow(ReportRange.WEEK)
    val range: StateFlow<ReportRange> = _range.asStateFlow()

    private fun bounds(range: ReportRange): Pair<LocalDate, LocalDate> {
        val today = clock.now().toLocalDate(clock.timeZone())
        return today.plusDays(-range.days) to today
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<SalesSummary> = _range
        .flatMapLatest { r -> bounds(r).let { repository.observeSummary(it.first, it.second) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SalesSummary())

    @OptIn(ExperimentalCoroutinesApi::class)
    val series: StateFlow<List<DailyPoint>> = _range
        .flatMapLatest { r -> bounds(r).let { repository.observeDailySeries(it.first, it.second) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val topProducts: StateFlow<List<TopProduct>> = _range
        .flatMapLatest { r -> bounds(r).let { repository.observeTopProducts(it.first, it.second) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onRangeSelected(range: ReportRange) { _range.value = range }
}
