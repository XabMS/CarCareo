package com.xabier.carcareo.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.LedgerTextAction
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.vehicle.ArchivedVehiclesViewModel
import com.xabier.carcareo.ui.vehicle.icon
import com.xabier.carcareo.ui.vehicle.labelRes

@Composable
fun ArchivedVehiclesScreen(
    onBack: () -> Unit,
    viewModel: ArchivedVehiclesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    // Id only: it survives rotation, and the dialog simply disappears if the
    // vehicle does.
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LedgerAppBar(
            title = stringResource(R.string.screen_archived),
            navigationIcon = {
                LedgerIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
        )

        if (archived.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.archived_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                LedgerCard {
                    archived.forEachIndexed { i, vehicle ->
                        if (i > 0) HairlineDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                vehicle.category.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    vehicle.name,
                                    style = LedgerText.rowTitleLg,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    stringResource(vehicle.category.labelRes),
                                    style = LedgerText.rowMeta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LedgerTextAction(
                                text = stringResource(R.string.action_unarchive),
                                onClick = { viewModel.unarchive(vehicle.id) },
                            )
                            Spacer(Modifier.width(12.dp))
                            LedgerTextAction(
                                text = stringResource(R.string.action_delete),
                                onClick = { pendingDeleteId = vehicle.id },
                            )
                        }
                    }
                }
            }
        }
    }

    archived.firstOrNull { it.id == pendingDeleteId }?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_vehicle_confirm_title, vehicle.name)) },
            text = { Text(stringResource(R.string.delete_vehicle_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(vehicle.id)
                    pendingDeleteId = null
                }) {
                    Text(
                        stringResource(R.string.delete_vehicle_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
