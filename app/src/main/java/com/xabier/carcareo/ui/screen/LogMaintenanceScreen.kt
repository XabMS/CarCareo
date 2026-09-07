package com.xabier.carcareo.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.DatePickerField
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.log.LogMaintenanceViewModel
import com.xabier.carcareo.ui.log.LogTaskRow
import com.xabier.carcareo.ui.plan.color
import com.xabier.carcareo.ui.plan.labelRes
import com.xabier.carcareo.ui.plan.remainingSummary

/**
 * P3 — the most important screen. Must be closeable in under 30 seconds:
 * everything is pre-filled, overdue/upcoming tasks are pre-checked, and no task
 * selection is required.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMaintenanceScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LogMaintenanceViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    // SAF picker for an invoice photo / PDF, with a persistable read grant so the
    // URI stays valid across reboots (spec F4).
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setAttachment(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEdit) R.string.screen_edit_record
                            else R.string.screen_log_maintenance,
                        ),
                    )
                },
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
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = { viewModel.save(onSaved) }) {
                        Text(stringResource(R.string.log_save))
                    }
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DatePickerField(
                label = stringResource(R.string.log_date),
                value = state.date,
                onValueChange = viewModel::setDate,
            )

            OutlinedTextField(
                value = state.odometer,
                onValueChange = viewModel::setOdometer,
                label = { Text(stringResource(R.string.log_odometer)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.odometerError,
                supportingText = when {
                    state.odometerError -> {
                        { Text(stringResource(R.string.error_odometer_required)) }
                    }
                    state.odometer.toIntOrNull()?.let { it < state.lastConfirmedKm } == true -> {
                        {
                            Text(
                                stringResource(
                                    R.string.log_km_warn_low,
                                    formatNumber(state.lastConfirmedKm),
                                ),
                            )
                        }
                    }
                    else -> null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.tasks.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.log_tasks_header),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.tasks.any { it.computation?.status == TaskStatus.OVERDUE }) {
                        TextButton(onClick = viewModel::selectAllOverdue) {
                            Text(stringResource(R.string.log_select_all_overdue))
                        }
                    }
                }

                state.tasks.forEach { row ->
                    TaskCheckRow(
                        row = row,
                        checked = row.taskId in state.selectedTaskIds,
                        onToggle = { viewModel.toggleTask(row.taskId) },
                    )
                    HorizontalDivider()
                }
            }

            Text(
                stringResource(R.string.log_free_record_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TextButton(onClick = viewModel::toggleExtras) {
                Text(
                    stringResource(
                        if (state.extrasExpanded) R.string.log_extras_hide
                        else R.string.log_extras_show,
                    ),
                )
            }

            if (state.extrasExpanded) {
                OutlinedTextField(
                    value = state.workshop,
                    onValueChange = viewModel::setWorkshop,
                    label = { Text(stringResource(R.string.log_workshop)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.cost,
                    onValueChange = viewModel::setCost,
                    label = { Text(stringResource(R.string.log_cost)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    label = { Text(stringResource(R.string.log_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.attachmentUri == null) {
                    OutlinedButton(
                        onClick = {
                            attachmentPicker.launch(arrayOf("image/*", "application/pdf"))
                        },
                    ) { Text(stringResource(R.string.log_attach)) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.log_attachment_attached),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.setAttachment(null) }) {
                            Text(stringResource(R.string.log_attachment_remove))
                        }
                    }
                }
                Text(
                    stringResource(R.string.log_attachment_export_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun TaskCheckRow(
    row: LogTaskRow,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = { onToggle() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            val computation = row.computation
            if (computation != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(computation.status.color()),
                    )
                    Spacer(Modifier.size(6.dp))
                    val label = stringResource(computation.status.labelRes)
                    val detail = if (computation.hasHistory) {
                        computation.remainingSummary()
                    } else {
                        stringResource(R.string.status_no_record)
                    }
                    Text(
                        text = "$label${stringResource(R.string.remaining_separator)}$detail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
