package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.isSpanishUi
import com.xabier.carcareo.ui.plan.PlanSetupViewModel
import com.xabier.carcareo.ui.vehicle.labelRes

/**
 * P4's onboarding step, shown once after a vehicle is created. Applying the
 * category template is the default; the other two paths are lighter-weight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    vehicleId: Long,
    onDone: () -> Unit,
    viewModel: PlanSetupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spanish = isSpanishUi()
    var showSourcePicker by rememberSaveable { mutableStateOf(false) }

    val vehicle = state.vehicle

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.plan_setup_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.plan_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (vehicle != null) {
                Button(
                    onClick = { viewModel.applyTemplate(spanish, onDone) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.plan_setup_apply_template,
                            stringResource(vehicle.category.labelRes),
                        ),
                    )
                }
                Text(
                    stringResource(R.string.template_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.otherVehicles.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showSourcePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.plan_setup_duplicate))
                }
            }

            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.plan_setup_empty))
            }
        }
    }

    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text(stringResource(R.string.plan_setup_pick_source)) },
            text = {
                Column {
                    state.otherVehicles.forEach { source ->
                        ListItem(
                            headlineContent = { Text(source.name) },
                            supportingContent = { Text(stringResource(source.category.labelRes)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourcePicker = false
                                    viewModel.duplicateFrom(source.id, onDone)
                                },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourcePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
