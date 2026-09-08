package com.xabier.carcareo.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.FieldHelper
import com.xabier.carcareo.ui.component.FieldLabel
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LabeledField
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerCardShape
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.LedgerPrimaryButton
import com.xabier.carcareo.ui.component.LedgerSecondaryButton
import com.xabier.carcareo.ui.component.LedgerTextAction
import com.xabier.carcareo.ui.format.formatDate
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.log.LogMaintenanceViewModel
import com.xabier.carcareo.ui.log.LogTaskRow
import com.xabier.carcareo.ui.plan.footerLabelRes
import com.xabier.carcareo.ui.plan.remainingSummary
import com.xabier.carcareo.ui.plan.textColor
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * P3 — the most important screen. Must be closeable in under 30 seconds:
 * everything is pre-filled, overdue/upcoming tasks are pre-checked, and no task
 * selection is required. "Workshop ledger" redesign (design handoff frame 1b).
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

    val sep = stringResource(R.string.remaining_separator)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LedgerAppBar(
                title = stringResource(
                    if (state.isEdit) R.string.screen_edit_record
                    else R.string.screen_log_maintenance,
                ),
                context = state.vehicleName.ifBlank { null },
                navigationIcon = {
                    LedgerIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                },
            )
        },
        bottomBar = { LogFooter(state.odometer, onSave = { viewModel.save(onSaved) }) },
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
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateBox(
                    label = stringResource(R.string.log_date),
                    value = state.date,
                    onValueChange = viewModel::setDate,
                    modifier = Modifier.weight(1f),
                )
                val lowKm = state.odometer.toIntOrNull()
                    ?.let { it in 1 until state.lastConfirmedKm } == true
                LabeledField(
                    label = stringResource(R.string.log_odometer_field) + sep +
                        stringResource(R.string.unit_km),
                    value = state.odometer,
                    onValueChange = viewModel::setOdometer,
                    isError = state.odometerError,
                    errorText = stringResource(R.string.error_odometer_required),
                    helperText = if (lowKm) {
                        stringResource(R.string.log_km_warn_low, formatNumber(state.lastConfirmedKm))
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LedgerText.rowMeta.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.tasks.isNotEmpty()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.log_tasks_header).upcase() + sep +
                                formatNumber(state.selectedTaskIds.size),
                            style = LedgerText.sectionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.tasks.any { it.computation?.status == TaskStatus.OVERDUE }) {
                            LedgerTextAction(
                                text = stringResource(R.string.log_select_all_overdue),
                                onClick = viewModel::selectAllOverdue,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LedgerCard {
                        state.tasks.forEachIndexed { i, row ->
                            if (i > 0) HairlineDivider()
                            TaskCheckRow(
                                row = row,
                                checked = row.taskId in state.selectedTaskIds,
                                onToggle = { viewModel.toggleTask(row.taskId) },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FieldHelper(stringResource(R.string.log_free_record_hint))
                }
            }

            LedgerCard(modifier = Modifier.animateContentSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleExtras() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (state.extrasExpanded) Icons.Filled.Remove else Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.log_extras_header),
                        style = LedgerText.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.field_optional).upcase(),
                        style = LedgerText.rowMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.extrasExpanded) {
                    HairlineDivider()
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LabeledField(
                            label = stringResource(R.string.log_workshop),
                            value = state.workshop,
                            onValueChange = viewModel::setWorkshop,
                        )
                        LabeledField(
                            label = stringResource(R.string.log_cost),
                            value = state.cost,
                            onValueChange = viewModel::setCost,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        LabeledField(
                            label = stringResource(R.string.log_notes),
                            value = state.notes,
                            onValueChange = viewModel::setNotes,
                            singleLine = false,
                            minHeight = 72.dp,
                        )
                        if (state.attachmentUri == null) {
                            LedgerSecondaryButton(
                                text = stringResource(R.string.log_attach),
                                onClick = {
                                    attachmentPicker.launch(arrayOf("image/*", "application/pdf"))
                                },
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.log_attachment_attached),
                                    style = LedgerText.rowTitle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                LedgerTextAction(
                                    text = stringResource(R.string.log_attachment_remove),
                                    onClick = { viewModel.setAttachment(null) },
                                )
                            }
                        }
                        FieldHelper(stringResource(R.string.log_attachment_export_warning))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFooter(odometer: String, onSave: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        HairlineDivider(Modifier.background(MaterialTheme.colorScheme.outline))
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LedgerPrimaryButton(
                text = stringResource(R.string.log_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
            val km = odometer.toIntOrNull()
            if (km != null && km > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.log_confirms_odometer, formatNumber(km)),
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateBox(
    label: String,
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(LedgerCardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outline, LedgerCardShape)
                .heightIn(min = 48.dp)
                .clickable { showDialog = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatDate(value),
                style = LedgerText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (showDialog) {
        val initialMillis = value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun TaskCheckRow(
    row: LogTaskRow,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val computation = row.computation
    val accent = computation?.status?.textColor() ?: MaterialTheme.colorScheme.outline
    val band = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) band else Color.Transparent)
            .drawBehind {
                if (checked) drawRect(accent, size = Size(3.dp.toPx(), size.height))
            }
            .toggleable(value = checked, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.extraColors.ink,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = MaterialTheme.extraColors.decorativeOutline,
            ),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.name,
                style = LedgerText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (computation != null) {
                val label = stringResource(computation.status.footerLabelRes)
                val detail = if (computation.hasHistory) {
                    computation.remainingSummary()
                } else {
                    stringResource(R.string.status_no_record)
                }
                Text(
                    text = label.upcase() + stringResource(R.string.remaining_separator) + detail,
                    style = LedgerText.rowMeta,
                    color = accent,
                )
            }
        }
    }
}
