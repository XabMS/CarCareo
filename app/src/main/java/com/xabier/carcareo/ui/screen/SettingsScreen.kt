package com.xabier.carcareo.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.xabier.carcareo.ui.settings.SettingsEvent
import com.xabier.carcareo.ui.settings.SettingsViewModel
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
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var pendingImportUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
    var showModeDialog by rememberSaveable { mutableStateOf(false) }
    var showReplaceConfirm by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val permissionDeniedMsg = stringResource(R.string.notif_permission_denied)
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The toggle stays on and the work stays scheduled either way; if the user
        // denied, tell them notifications won't actually appear.
        if (!granted) scope.launch { snackbar.showSnackbar(permissionDeniedMsg) }
    }

    // POST_NOTIFICATIONS only exists from API 33; below that, notifications are
    // granted at install time and there is nothing to ask for. The version check
    // is written inline so lint can see the constant is never touched on older
    // devices.
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

    // Resolve the snackbar text in composition (configuration-aware), then show it.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionHeader(stringResource(R.string.settings_section_data))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = {
                    Text(
                        stringResource(R.string.settings_export_desc) + " " +
                            stringResource(R.string.export_attachment_warning),
                    )
                },
                modifier = Modifier.clickable(enabled = !state.busy) {
                    exportLauncher.launch(defaultExportFilename())
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                supportingContent = { Text(stringResource(R.string.settings_import_desc)) },
                modifier = Modifier.clickable(enabled = !state.busy) {
                    importLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream"),
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_archived)) },
                modifier = Modifier.clickable(onClick = onOpenArchived),
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_notifications))
            ToggleRow(
                title = stringResource(R.string.settings_maintenance_alerts),
                desc = stringResource(R.string.settings_maintenance_alerts_desc),
                checked = notifPrefs.maintenanceAlertsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) requestNotificationPermissionIfNeeded()
                    viewModel.setMaintenanceAlerts(enabled)
                },
            )
            ToggleRow(
                title = stringResource(R.string.settings_odometer_reminder),
                desc = stringResource(R.string.settings_odometer_reminder_desc),
                checked = notifPrefs.odometerReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) requestNotificationPermissionIfNeeded()
                    viewModel.setOdometerReminder(enabled)
                },
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
                },
            )
        }
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
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(desc) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
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
    androidx.compose.foundation.layout.Row(
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
