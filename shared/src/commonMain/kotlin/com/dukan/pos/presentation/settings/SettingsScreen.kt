package com.dukan.pos.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dukan.pos.core.designsystem.LocalSpacing
import com.dukan.pos.core.designsystem.LocalStrings
import com.dukan.pos.core.designsystem.SectionHeader
import com.dukan.pos.domain.model.AppLanguage
import com.dukan.pos.domain.model.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val job by viewModel.job.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                SettingsEffect.ExportDone -> strings.done
                SettingsEffect.ExportFailed -> strings.exportFailed
                SettingsEffect.RestoreDone -> strings.restoreDone
                SettingsEffect.RestoreFailed -> strings.restoreFailed
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(strings.importBackup) },
            text = { Text(strings.restoreWarning) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    viewModel.restoreBackup()
                }) { Text(strings.importBackup) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(strings.cancel) }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            // Backup sits first, not buried at the bottom. On an offline app it
            // is the single most important thing on this screen.
            SectionHeader(strings.backupSection)
            Text(
                strings.backupBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = viewModel::exportBackup,
                enabled = job == BackupJob.NONE,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (job == BackupJob.EXPORTING) strings.exporting else strings.exportBackup)
            }
            OutlinedButton(
                onClick = viewModel::exportCsv,
                enabled = job == BackupJob.NONE,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.exportCsv) }
            OutlinedButton(
                onClick = { confirmRestore = true },
                enabled = job == BackupJob.NONE,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.importBackup) }

            SectionHeader(strings.shopSection)
            OutlinedTextField(
                value = settings.shopName,
                onValueChange = viewModel::onShopNameChange,
                label = { Text(strings.shopName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.ownerName,
                onValueChange = viewModel::onOwnerChange,
                label = { Text(strings.ownerName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text(strings.phone) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.currencySymbol,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text(strings.currencySymbol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(strings.language)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = settings.language == language,
                        onClick = { viewModel.onLanguageChange(language) },
                        label = { Text(if (language == AppLanguage.URDU) "اردو" else "English") },
                    )
                }
            }

            SectionHeader(strings.theme)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.theme == mode,
                        onClick = { viewModel.onThemeChange(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            SectionHeader(strings.saleSection)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.allowNegativeStock, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        strings.allowNegativeStockBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.allowNegativeStock,
                    onCheckedChange = viewModel::onAllowNegativeStockChange,
                )
            }

            OutlinedTextField(
                value = settings.receiptFooter,
                onValueChange = viewModel::onReceiptFooterChange,
                label = { Text(strings.receiptFooter) },
                modifier = Modifier.fillMaxWidth().padding(bottom = spacing.xxl),
            )
        }
    }
}
