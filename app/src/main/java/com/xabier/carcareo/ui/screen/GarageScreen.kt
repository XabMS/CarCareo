package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.component.StatusStripe
import com.xabier.carcareo.ui.component.FloatingActionBar
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.plan.footerLabelRes
import com.xabier.carcareo.ui.plan.garageTail
import com.xabier.carcareo.ui.plan.overdueFraction
import com.xabier.carcareo.ui.plan.textColor
import com.xabier.carcareo.ui.plan.upcomingFraction
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.GarageViewModel
import com.xabier.carcareo.ui.vehicle.OverdueSummary
import com.xabier.carcareo.ui.vehicle.VehicleCard
import com.xabier.carcareo.ui.vehicle.icon
import com.xabier.carcareo.ui.vehicle.labelRes

/**
 * P1 — Garage (home), "Workshop ledger" redesign: paper cards with a status
 * stripe, an attention banner for overdue tasks, and a global "LOG WORK" FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogWork: (vehicleId: Long, taskId: Long?) -> Unit,
    viewModel: GarageViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        GarageHeader(
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onAddVehicle = onAddVehicle,
            onOpenArchived = onOpenArchived,
            onOpenSettings = onOpenSettings,
        )

        Box(Modifier.weight(1f)) {
            when {
                state.loading -> Unit
                state.vehicles.isEmpty() -> EmptyGarage(Modifier.fillMaxSize())
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 96.dp +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.overdueSummary?.let { summary ->
                        item {
                            AttentionBanner(
                                summary = summary,
                                onLog = { onLogWork(summary.topVehicleId, summary.topTaskId) },
                            )
                        }
                    }
                    items(state.vehicles, key = { it.vehicle.id }) { card ->
                        VehicleGarageCard(card = card, onClick = { onOpenVehicle(card.vehicle.id) })
                    }
                }
            }

            if (state.vehicles.isNotEmpty()) {
                FloatingActionBar(
                    text = stringResource(R.string.action_log_work),
                    icon = Icons.Filled.Build,
                    onClick = {
                        val active = state.vehicles
                        if (active.size == 1) onLogWork(active.first().vehicle.id, null)
                        else showPicker = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(20.dp),
                )
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                SectionLabelRow(
                    label = stringResource(R.string.garage_pick_vehicle),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                state.vehicles.forEach { card ->
                    VehiclePickerRow(card) {
                        showPicker = false
                        onLogWork(card.vehicle.id, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageHeader(
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenArchived: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.app_name).upcase(),
                style = LedgerText.contextLine,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.screen_garage),
                style = LedgerText.garageTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LedgerIconButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.action_add_vehicle),
            onClick = onAddVehicle,
            bordered = true,
        )
        Spacer(Modifier.width(4.dp))
        Box {
            LedgerIconButton(
                icon = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.action_more_options),
                onClick = { onMenuOpenChange(true) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.garage_menu_archived)) },
                    onClick = { onMenuOpenChange(false); onOpenArchived() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.garage_menu_settings)) },
                    onClick = { onMenuOpenChange(false); onOpenSettings() },
                )
            }
        }
    }
}

@Composable
private fun AttentionBanner(summary: OverdueSummary, onLog: () -> Unit) {
    val err = MaterialTheme.colorScheme.error
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.extraColors.alertGround)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .drawBehind { drawRect(err, size = Size(4.dp.toPx(), size.height)) }
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = err, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                pluralStringResource(
                    R.plurals.garage_overdue_count,
                    summary.taskCount,
                    formatNumber(summary.taskCount),
                ).upcase(),
                style = LedgerText.rowMeta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = err,
            )
            val sep = stringResource(R.string.remaining_separator)
            Text(
                text = if (summary.vehicleCount > 1) {
                    pluralStringResource(
                        R.plurals.garage_overdue_vehicles,
                        summary.vehicleCount,
                        formatNumber(summary.vehicleCount),
                    )
                } else {
                    summary.topVehicleName + sep + summary.topTaskName
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.extraColors.bodyOnCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            stringResource(R.string.garage_banner_log).upcase(),
            style = LedgerText.buttonLabelSm,
            color = err,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onLog)
                .padding(8.dp),
        )
    }
}

@Composable
private fun VehicleGarageCard(card: VehicleCard, onClick: () -> Unit) {
    LedgerCard(Modifier.clickable(onClick = onClick)) {
        // Head
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = card.vehicle.category.icon,
                contentDescription = stringResource(card.vehicle.category.labelRes),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        card.vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    card.vehicle.plate?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            it,
                            style = LedgerText.rowMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val sub = listOfNotNull(
                    card.vehicle.make?.takeIf { it.isNotBlank() },
                    card.vehicle.model?.takeIf { it.isNotBlank() },
                    card.vehicle.year?.toString(),
                ).joinToString(stringResource(R.string.remaining_separator))
                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatNumber(card.odometer.km),
                    style = LedgerText.cardNumber,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(
                        if (card.odometer.isEstimate) R.string.garage_odo_estimate
                        else R.string.garage_odo_confirmed,
                    ).upcase(),
                    style = LedgerText.mono10,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val status = card.planStatus
        if (status != null) {
            StatusStripe(
                overdueFraction = status.overdueFraction(),
                upcomingFraction = status.upcomingFraction(),
            )
        } else {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }

        // Footer band
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val worst = status?.vehicleStatus
            Text(
                text = if (worst != null) stringResource(worst.footerLabelRes).upcase()
                else stringResource(R.string.garage_no_plan).upcase(),
                style = LedgerText.mono10Label,
                color = worst?.textColor() ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = status?.garageTail() ?: stringResource(R.string.garage_line_no_plan),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extraColors.bodyOnCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VehiclePickerRow(card: VehicleCard, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            card.vehicle.category.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(card.vehicle.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyGarage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.garage_empty_title),
            style = LedgerText.screenTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.garage_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
