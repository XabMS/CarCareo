package com.xabier.carcareo.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.BuildConfig
import com.xabier.carcareo.R
import com.xabier.carcareo.data.backup.BackupError
import com.xabier.carcareo.data.backup.ImportMode
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.settings.SettingsEvent
import com.xabier.carcareo.ui.settings.SettingsViewModel
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenArchived: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notifPrefs by viewModel.notificationPrefs.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    var pendingImportUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
    var showModeDialog by rememberSaveable { mutableStateOf(false) }
    var showReplaceConfirm by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val permissionDeniedMsg = stringResource(R.string.notif_permission_denied)
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) scope.launch { snackbar.showSnackbar(permissionDeniedMsg) }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.export { bytes ->
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("no output stream")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showModeDialog = true
        }
    }

    fun runImport(mode: ImportMode) {
        val uri = pendingImportUri ?: return
        viewModel.import(mode) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("no input stream")
            }
        }
        pendingImportUri = null
    }

    val event = state.event
    val eventText: String? = when (event) {
        null -> null
        SettingsEvent.Exported -> stringResource(R.string.msg_export_ok)
        SettingsEvent.ExportFailed -> stringResource(R.string.msg_export_failed)
        is SettingsEvent.Imported ->
            pluralStringResource(R.plurals.msg_import_ok, event.vehicles, event.vehicles)
        is SettingsEvent.ImportFailed -> when (event.error) {
            BackupError.MALFORMED -> stringResource(R.string.msg_import_failed_malformed)
            BackupError.UNSUPPORTED_VERSION -> stringResource(R.string.msg_import_failed_version)
            null -> stringResource(R.string.msg_import_failed_generic)
        }
    }
    LaunchedEffect(event) {
        if (eventText != null) {
            snackbar.showSnackbar(eventText)
            viewModel.consumeEvent()
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LedgerAppBar(
            title = stringResource(R.string.settings_title),
            navigationIcon = {
                LedgerIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
        )
        if (state.busy) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.extraColors.ink,
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            SectionLabelRow(stringResource(R.string.settings_section_your_data))
            LedgerCard {
                SettingsRow(
                    icon = Icons.Filled.UploadFile,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_export_title),
                    body = stringResource(R.string.settings_export_body),
                    enabled = !state.busy,
                    onClick = { exportLauncher.launch(defaultExportFilename()) },
                )
                HairlineDivider()
                SettingsRow(
                    icon = Icons.Filled.Download,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_import_title),
                    body = stringResource(R.string.settings_import_body),
                    enabled = !state.busy,
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                )
                HairlineDivider()
                SettingsRow(
                    icon = Icons.Filled.Inventory2,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.settings_archived),
                    onClick = onOpenArchived,
                    trailing = {
                        Text(
                            formatNumber(archivedCount),
                            style = LedgerText.rowMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            BackupNotice(state.lastExportAt)

            Spacer(Modifier.height(20.dp))
            SectionLabelRow(stringResource(R.string.settings_section_notifications))
            LedgerCard {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_maintenance_alerts),
                    body = stringResource(R.string.settings_maintenance_alerts_desc),
                    checked = notifPrefs.maintenanceAlertsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        viewModel.setMaintenanceAlerts(enabled)
                    },
                )
                HairlineDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.settings_odometer_reminder),
                    body = stringResource(R.string.settings_odometer_reminder_desc),
                    checked = notifPrefs.odometerReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        viewModel.setOdometerReminder(enabled)
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabelRow(stringResource(R.string.settings_section_about))
            LedgerCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = LedgerText.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = LedgerText.rowMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
        androidx.compose.material3.SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }

    if (showModeDialog) {
        ImportModeDialog(
            onDismiss = { showModeDialog = false; pendingImportUri = null },
            onAdd = { showModeDialog = false; runImport(ImportMode.ADD) },
            onReplace = { showModeDialog = false; showReplaceConfirm = true },
        )
    }

    if (showReplaceConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm = false; pendingImportUri = null },
            title = { Text(stringResource(R.string.import_replace_confirm_title)) },
            text = { Text(stringResource(R.string.import_replace_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceConfirm = false
                    runImport(ImportMode.REPLACE)
                }) { Text(stringResource(R.string.import_mode_replace)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReplaceConfirm = false
                    pendingImportUri = null
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = LedgerText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (body != null) {
                Text(
                    body,
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LedgerText.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Text(
                body,
                style = LedgerText.supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = ledgerSwitchColors())
    }
}

@Composable
private fun BackupNotice(lastExportAt: Long) {
    val accent = MaterialTheme.extraColors.infoAccent
    val months = if (lastExportAt <= 0L) -1
    else ((System.currentTimeMillis() - lastExportAt) / (30L * 24 * 60 * 60 * 1000)).toInt()

    val text = buildString {
        append(stringResource(R.string.settings_backup_notice))
        if (months >= 1) {
            append(' ')
            append(pluralStringResource(R.plurals.settings_backup_last, months, formatNumber(months)))
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.extraColors.infoGround)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .drawBehind { drawRect(accent, size = Size(4.dp.toPx(), size.height)) }
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = LedgerText.supporting,
            color = MaterialTheme.extraColors.bodyOnCard,
        )
    }
}

@Composable
private fun ImportModeDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_mode_title)) },
        text = {
            Column {
                ModeRow(
                    title = stringResource(R.string.import_mode_add),
                    desc = stringResource(R.string.import_mode_add_desc),
                    onClick = onAdd,
                )
                ModeRow(
                    title = stringResource(R.string.import_mode_replace),
                    desc = stringResource(R.string.import_mode_replace_desc),
                    onClick = onReplace,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ModeRow(title: String, desc: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = false, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun defaultExportFilename(): String {
    val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    return "mantenimiento-vehiculos-$stamp.json"
}
