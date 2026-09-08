package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.domain.template.MaintenanceTemplates
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.component.LedgerAppBar
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerIconButton
import com.xabier.carcareo.ui.component.LedgerPrimaryButton
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.isSpanishUi
import com.xabier.carcareo.ui.plan.PlanSetupViewModel
import com.xabier.carcareo.ui.plan.intervalSummary
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.labelRes

/**
 * Step 2 of the create flow (design handoff §7): pick a starting point for the
 * plan. Applying the category template is the recommended path.
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
    val sep = stringResource(R.string.remaining_separator)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LedgerAppBar(
            title = stringResource(R.string.screen_plan_editor),
            context = stringResource(R.string.form_step, 2, 2) +
                (vehicle?.name?.let { sep + it } ?: ""),
            navigationIcon = {
                LedgerIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onDone,
                )
            },
            progressFilled = 2,
            progressTotal = 2,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.plan_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.extraColors.bodyOnCard,
            )

            if (vehicle != null) {
                val template = MaintenanceTemplates.forCategory(vehicle.category)
                RecommendedCard(
                    categoryLabel = stringResource(vehicle.category.labelRes),
                    total = template.size,
                    previewLines = template.take(3).map { t ->
                        t.localizedName(spanish) to t.toEntity(0, spanish, 0).intervalSummary()
                    },
                    onUse = { viewModel.applyTemplate(spanish, onDone) },
                )
            }

            if (state.otherVehicles.isNotEmpty()) {
                LedgerCard {
                    SetupRow(
                        icon = Icons.Filled.ContentCopy,
                        title = stringResource(R.string.plan_setup_duplicate),
                        subtitle = state.otherVehicles.joinToString(sep) { it.name },
                        onClick = { showSourcePicker = true },
                    )
                }
            }
            LedgerCard {
                SetupRow(
                    icon = Icons.Filled.EditNote,
                    title = stringResource(R.string.plan_setup_scratch_title),
                    subtitle = stringResource(R.string.plan_setup_scratch_sub),
                    onClick = onDone,
                )
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
                        Text(
                            source.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourcePicker = false
                                    viewModel.duplicateFrom(source.id, onDone)
                                }
                                .padding(vertical = 12.dp),
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

@Composable
private fun RecommendedCard(
    categoryLabel: String,
    total: Int,
    previewLines: List<Pair<String, String>>,
    onUse: () -> Unit,
) {
    Box(Modifier.padding(top = 8.dp)) {
        LedgerCard(borderColor = MaterialTheme.extraColors.ink) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.plan_setup_template_title, categoryLabel),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    pluralStringResource(R.plurals.plan_setup_template_sub, total, formatNumber(total)),
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                previewLines.forEach { (name, interval) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = LedgerText.rowMeta,
                            color = MaterialTheme.extraColors.bodyOnCard,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            interval.upcase(),
                            style = LedgerText.rowMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (total > previewLines.size) {
                    Text(
                        stringResource(R.string.plan_setup_preview_more, formatNumber(total - previewLines.size)),
                        style = LedgerText.rowMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                LedgerPrimaryButton(
                    text = stringResource(R.string.plan_setup_use_template),
                    onClick = onUse,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.template_disclaimer),
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(R.string.plan_setup_recommended).upcase(),
            style = LedgerText.tag,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .offset(x = 12.dp, y = (-8).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.extraColors.ink)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SetupRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = LedgerText.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = LedgerText.supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
