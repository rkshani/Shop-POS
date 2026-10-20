package com.dukan.pos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dukan.pos.core.platform.PlatformActions
import com.dukan.pos.domain.model.AppLanguage
import com.dukan.pos.domain.model.ShopSettings
import com.dukan.pos.domain.model.ThemeMode
import com.dukan.pos.domain.repository.BackupRepository
import com.dukan.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class BackupJob { NONE, EXPORTING, RESTORING }

sealed interface SettingsEffect {
    data object ExportDone : SettingsEffect
    data object ExportFailed : SettingsEffect
    data object RestoreDone : SettingsEffect
    data object RestoreFailed : SettingsEffect
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val actions: PlatformActions,
) : ViewModel() {

    val settings: StateFlow<ShopSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopSettings())

    private val _job = MutableStateFlow(BackupJob.NONE)
    val job: StateFlow<BackupJob> = _job.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onShopNameChange(value: String) = update { it.copy(shopName = value) }
    fun onOwnerChange(value: String) = update { it.copy(ownerName = value) }
    fun onPhoneChange(value: String) = update { it.copy(phone = value) }
    fun onCurrencyChange(value: String) = update { it.copy(currencySymbol = value) }
    fun onLanguageChange(value: AppLanguage) = update { it.copy(language = value) }
    fun onThemeChange(value: ThemeMode) = update { it.copy(theme = value) }
    fun onAllowNegativeStockChange(value: Boolean) = update { it.copy(allowNegativeStock = value) }
    fun onReceiptFooterChange(value: String) = update { it.copy(receiptFooter = value) }

    fun exportBackup() = runExport(BackupJob.EXPORTING) {
        actions.saveTextFile(
            fileName = "dukan-backup-${stamp()}.json",
            mimeType = "application/json",
            content = backupRepository.exportJson(),
        )
    }

    fun exportCsv() = runExport(BackupJob.EXPORTING) {
        // Three sheets is more useful to an accountant than one wide file.
        val products = actions.saveTextFile(
            "dukan-products-${stamp()}.csv", "text/csv", backupRepository.exportProductsCsv(),
        )
        val sales = actions.saveTextFile(
            "dukan-sales-${stamp()}.csv", "text/csv", backupRepository.exportSalesCsv(),
        )
        val khata = actions.saveTextFile(
            "dukan-khata-${stamp()}.csv", "text/csv", backupRepository.exportKhataCsv(),
        )
        products || sales || khata
    }

    fun restoreBackup() {
        if (_job.value != BackupJob.NONE) return
        _job.value = BackupJob.RESTORING
        viewModelScope.launch {
            val content = actions.readTextFile()
            if (content == null) {
                _job.value = BackupJob.NONE
                return@launch
            }
            val result = runCatching { backupRepository.restoreJson(content) }
            _job.value = BackupJob.NONE
            _effects.send(
                if (result.isSuccess) SettingsEffect.RestoreDone else SettingsEffect.RestoreFailed,
            )
        }
    }

    private fun runExport(job: BackupJob, block: suspend () -> Boolean) {
        if (_job.value != BackupJob.NONE) return
        _job.value = job
        viewModelScope.launch {
            val ok = runCatching { block() }.getOrDefault(false)
            _job.value = BackupJob.NONE
            if (ok) {
                _effects.send(SettingsEffect.ExportDone)
            } else {
                _effects.send(SettingsEffect.ExportFailed)
            }
        }
    }

    private fun stamp(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.date}-${now.hour.toString().padStart(2, '0')}${now.minute.toString().padStart(2, '0')}"
    }

    private fun update(transform: (ShopSettings) -> ShopSettings) {
        viewModelScope.launch { repository.update(transform(repository.get())) }
    }
}
