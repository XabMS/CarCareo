package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.domain.KmChangeSeverity
import com.xabier.carcareo.domain.OdometerReading
import com.xabier.carcareo.domain.OdometerUpdate
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.DatePickerField
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.plan.color
import com.xabier.carcareo.ui.plan.intervalSummary
import com.xabier.carcareo.ui.plan.remainingSummary
import com.xabier.carcareo.ui.vehicle.VehicleDetailViewModel
import com.xabier.carcareo.ui.vehicle.freshnessText
import com.xabier.carcareo.ui.vehicle.icon
import com.xabier.carcareo.ui.vehicle.kmText
import com.xabier.carcareo.ui.vehicle.labelRes
import java.time.LocalDate

/**
 * P2 — Vehicle detail. F1 covers the header, the odometer card with "Update km",
 * archive, and navigation to the other screens. The task list + status arrive in
 * F2/F3, so the plan section shows an empty note for now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onEditPlan: (Long) -> Unit,
    onLogMaintenance: (vehicleId: Long, taskId: Long?) -> Unit,
    onViewHistory: (Long) -> Unit,
    viewModel: VehicleDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var showUpdateKm by rememberSaveable { mutableStateOf(false) }
    var showArchiveConfirm by rememberSaveable { mutableStateOf(false) }

    val vehicle = state.vehicle
    val odometer = state.odometer

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.name ?: stringResource(R.string.screen_vehicle_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (vehicle != null) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_action_edit_vehicle)) },
                                onClick = { menuOpen = false; onEditVehicle(vehicle.id) },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (vehicle.archived) R.string.action_unarchive
                                            else R.string.action_archive,
                                        ),
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    if (vehicle.archived) viewModel.setArchived(false)
                                    else showArchiveConfirm = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (vehicle == null || odometer == null) {
            // Loading, or the vehicle was just archived/deleted from under us.
            Spacer(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VehicleHeader(vehicle)
            OdometerCard(
                odometer = odometer,
                onUpdateClick = { showUpdateKm = true },
            )
            PlanSection(
                planStatus = state.planStatus,
                onLogTask = { taskId -> onLogMaintenance(vehicle.id, taskId) },
            )
            DetailActions(
                onLog = { onLogMaintenance(vehicle.id, null) },
                onEditPlan = { onEditPlan(vehicle.id) },
                onHistory = { onViewHistory(vehicle.id) },
            )
        }
    }

    if (showUpdateKm && vehicle != null) {
        UpdateOdometerDialog(
            previousConfirmedKm = vehicle.lastConfirmedKm,
            onDismiss = { showUpdateKm = false },
            onConfirm = { km, date ->
                viewModel.confirmOdometer(km, date)
                showUpdateKm = false
            },
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(stringResource(R.string.detail_archive_confirm_title)) },
            text = { Text(stringResource(R.string.detail_archive_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    viewModel.setArchived(true)
                    onBack()
                }) { Text(stringResource(R.string.action_archive)) }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun VehicleHeader(vehicle: Vehicle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = vehicle.category.icon,
            contentDescription = stringResource(vehicle.category.labelRes),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(vehicle.category.labelRes), style = MaterialTheme.typography.labelLarge)
            val subtitle = listOfNotNull(
                vehicle.make,
                vehicle.model,
                vehicle.year?.toString(),
            ).joinToString(" ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            vehicle.plate?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OdometerCard(odometer: OdometerReading, onUpdateClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.odometer_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(odometer.kmText(), style = MaterialTheme.typography.headlineMedium)
            Text(
                odometer.freshnessText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onUpdateClick) {
                Text(stringResource(R.string.action_update_km))
            }
        }
    }
}

@Composable
private fun PlanSection(
    planStatus: com.xabier.carcareo.domain.VehiclePlanStatus?,
    onLogTask: (Long) -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.detail_section_plan),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))

        val active = planStatus?.activeOrdered.orEmpty()
        val inactive = planStatus?.inactive.orEmpty()

        if (active.isEmpty() && inactive.isEmpty()) {
            Text(
                stringResource(R.string.detail_plan_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        // Active tasks, most urgent first (spec P2 / 4.3). Tapping a row logs just
        // that task; the "Log maintenance" button below covers multi-task work.
        active.forEach { tws ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        onClickLabel = stringResource(
                            R.string.detail_plan_row_log,
                            tws.task.name,
                        ),
                    ) { onLogTask(tws.task.id) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tws.computation.status.color()),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tws.task.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (tws.computation.hasHistory) {
                            tws.computation.remainingSummary()
                        } else {
                            stringResource(R.string.status_no_record)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp),
                )
            }
        }

        // Deactivated tasks: shown struck through, no status.
        inactive.forEach { task ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = task.intervalSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailActions(
    onLog: () -> Unit,
    onEditPlan: () -> Unit,
    onHistory: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onLog) {
            Text(stringResource(R.string.detail_action_log))
        }
        OutlinedButton(onClick = onEditPlan) {
            Text(stringResource(R.string.detail_action_edit_plan))
        }
        OutlinedButton(onClick = onHistory) {
            Text(stringResource(R.string.detail_action_history))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateOdometerDialog(
    previousConfirmedKm: Int,
    onDismiss: () -> Unit,
    onConfirm: (km: Int, date: LocalDate) -> Unit,
) {
    var kmText by rememberSaveable { mutableStateOf(previousConfirmedKm.toString()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var majorConfirmed by rememberSaveable { mutableStateOf(false) }

    val km = kmText.trim().toIntOrNull()
    val severity = km?.let { OdometerUpdate.classify(previousConfirmedKm, it) }
    val canSave = km != null && km >= 0 &&
        (severity != KmChangeSeverity.MAJOR_DECREASE || majorConfirmed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_km_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { new -> kmText = new.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.update_km_field)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = km == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField(
                    label = stringResource(R.string.update_km_date),
                    value = date,
                    onValueChange = { date = it },
                )
                when (severity) {
                    KmChangeSeverity.MINOR_DECREASE -> Text(
                        stringResource(
                            R.string.update_km_warn_minor,
                            formatNumber(previousConfirmedKm),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    KmChangeSeverity.MAJOR_DECREASE -> Column {
                        Text(
                            stringResource(
                                R.string.update_km_warn_major,
                                formatNumber(previousConfirmedKm),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = majorConfirmed,
                                onCheckedChange = { majorConfirmed = it },
                            )
                            Text(stringResource(R.string.update_km_confirm_major))
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onConfirm(km!!, date) },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
