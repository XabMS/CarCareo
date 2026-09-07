package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.LedgerPrimaryButton
import com.xabier.carcareo.ui.history.HistoryViewModel
import com.xabier.carcareo.ui.navigation.VehicleTab
import com.xabier.carcareo.ui.plan.PlanEditorViewModel
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.VehicleDetailViewModel
import com.xabier.carcareo.ui.vehicle.labelRes

/**
 * The shell shared by the Overview / Plan / History tabs of one vehicle
 * (design handoff §"Navigation changes"). It owns the header, the sticky
 * "LOG MAINTENANCE" bar and the bottom nav; the tabs are plain content.
 *
 * Tab switches never touch the back stack — system Back leaves the vehicle.
 */
@Composable
fun VehicleShellScreen(
    vehicleId: Long,
    initialTab: VehicleTab,
    onBack: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    onLogMaintenance: (vehicleId: Long, taskId: Long?) -> Unit,
    onEditRecord: (vehicleId: Long, recordId: Long) -> Unit,
) {
    val detailVm: VehicleDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val planVm: PlanEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val historyVm: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val detail by detailVm.uiState.collectAsStateWithLifecycle()
    val history by historyVm.uiState.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var showArchiveConfirm by rememberSaveable { mutableStateOf(false) }

    val vehicle = detail.vehicle
    val sep = stringResource(R.string.remaining_separator)

    val context: String? = when (tab) {
        VehicleTab.OVERVIEW -> vehicle?.let { v ->
            listOfNotNull(
                stringResource(v.category.labelRes),
                v.plate?.takeIf { it.isNotBlank() },
            ).joinToString(sep)
        }
        VehicleTab.PLAN -> {
            val active = detail.planStatus?.activeOrdered?.size ?: 0
            val inactive = detail.planStatus?.inactive?.size ?: 0
            val tasks = pluralStringResource(R.plurals.shell_context_tasks, active, active)
            if (inactive > 0) {
                tasks + sep + pluralStringResource(R.plurals.shell_context_inactive, inactive, inactive)
            } else tasks
        }
        VehicleTab.HISTORY ->
            pluralStringResource(R.plurals.shell_context_records, history.totalRecords, history.totalRecords)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LedgerAppBar(
            title = vehicle?.name ?: stringResource(R.string.screen_vehicle_detail),
            context = context,
            navigationIcon = {
                LedgerIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
            actions = {
                if (vehicle != null) {
                    Box {
                        LedgerIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                            onClick = { menuOpen = true },
                        )
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
                                    if (vehicle.archived) detailVm.setArchived(false)
                                    else showArchiveConfirm = true
                                },
                            )
                        }
                    }
                }
            },
        )

        Box(Modifier.weight(1f)) {
            when (tab) {
                VehicleTab.OVERVIEW -> OverviewTab(
                    viewModel = detailVm,
                    onLogTask = { taskId -> onLogMaintenance(vehicleId, taskId) },
                )
                VehicleTab.PLAN -> PlanTab(viewModel = planVm)
                VehicleTab.HISTORY -> HistoryTab(
                    viewModel = historyVm,
                    onEditRecord = { recordId -> onEditRecord(vehicleId, recordId) },
                )
            }
        }

        ShellBottomBar(
            selected = tab,
            onSelect = { tab = it },
            onLog = { onLogMaintenance(vehicleId, null) },
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
                    detailVm.setArchived(true)
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
private fun ShellBottomBar(
    selected: VehicleTab,
    onSelect: (VehicleTab) -> Unit,
    onLog: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
        ) {
            LedgerPrimaryButton(
                text = stringResource(R.string.detail_action_log),
                onClick = onLog,
                icon = Icons.Filled.Build,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                NavItem(Icons.Filled.Dashboard, R.string.nav_overview, selected == VehicleTab.OVERVIEW,
                    Modifier.weight(1f)) { onSelect(VehicleTab.OVERVIEW) }
                NavItem(Icons.Filled.Checklist, R.string.nav_plan, selected == VehicleTab.PLAN,
                    Modifier.weight(1f)) { onSelect(VehicleTab.PLAN) }
                NavItem(Icons.Filled.History, R.string.nav_history, selected == VehicleTab.HISTORY,
                    Modifier.weight(1f)) { onSelect(VehicleTab.HISTORY) }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    labelRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(labelRes).upcase(),
            style = LedgerText.navLabel.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = color,
        )
    }
}
