package com.xabier.carcareo.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.domain.template.MaintenanceTemplates
import com.xabier.carcareo.ui.component.FloatingActionBar
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerPrimaryButton
import com.xabier.carcareo.ui.component.LedgerSecondaryButton
import com.xabier.carcareo.ui.component.FieldLabel
import com.xabier.carcareo.ui.component.LabeledField
import com.xabier.carcareo.ui.component.NumberUnitBox
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.isSpanishUi
import com.xabier.carcareo.ui.plan.PlanEditorViewModel
import com.xabier.carcareo.ui.plan.TaskDraft
import com.xabier.carcareo.ui.plan.intervalSummary
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.labelRes
import sh.calvin.reorderable.ReorderableColumn

/**
 * Plan tab of the vehicle shell (design handoff §4): one hairline-ruled table of
 * tasks with drag-to-reorder, a template top-up row, and an ADD TASK bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTab(
    viewModel: PlanEditorViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spanish = isSpanishUi()
    val haptic = LocalHapticFeedback.current

    var editingDraft by rememberSaveable { mutableStateOf<TaskDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<MaintenanceTask?>(null) }
    var showTemplateConfirm by rememberSaveable { mutableStateOf(false) }

    // Local copy so the list follows the finger during a drag; the DB flow
    // re-syncs it after the reorder is persisted.
    var items by remember { mutableStateOf(state.tasks) }
    LaunchedEffect(state.tasks) { items = state.tasks }

    val missingTemplateCount = remember(state.tasks, state.vehicle, spanish) {
        val category = state.vehicle?.category ?: return@remember 0
        val have = state.tasks.mapTo(HashSet()) { it.name.lowercase() }
        MaintenanceTemplates.forCategory(category)
            .count { it.localizedName(spanish).lowercase() !in have }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> Unit
            items.isEmpty() -> EmptyPlan(
                modifier = Modifier.fillMaxSize(),
                onApplyTemplate = { showTemplateConfirm = true },
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 96.dp),
            ) {
                SectionLabelRow(stringResource(R.string.plan_order_label))
                Spacer(Modifier.height(4.dp))
                LedgerCard {
                    ReorderableColumn(
                        list = items,
                        onSettle = { from, to ->
                            items = items.toMutableList().apply { add(to, removeAt(from)) }
                            viewModel.persistOrder(items)
                        },
                        onMove = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    ) { index, task, _ ->
                        key(task.id) {
                            if (index > 0) HairlineDivider()
                            PlanTaskRow(
                                task = task,
                                dragHandle = {
                                    Icon(
                                        Icons.Filled.DragIndicator,
                                        contentDescription = null,
                                        tint = if (task.active) MaterialTheme.extraColors.decorativeOutline
                                        else MaterialTheme.extraColors.chipOutline,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                            ),
                                    )
                                },
                                onEdit = { editingDraft = TaskDraft.from(task) },
                                onToggleActive = { viewModel.toggleActive(task) },
                                onDelete = { pendingDelete = task },
                            )
                        }
                    }
                }

                if (missingTemplateCount > 0 && state.vehicle != null) {
                    Spacer(Modifier.height(12.dp))
                    TemplateTopUpRow(
                        categoryLabel = stringResource(state.vehicle!!.category.labelRes),
                        count = missingTemplateCount,
                        onClick = { showTemplateConfirm = true },
                    )
                }
            }
        }

        FloatingActionBar(
            text = stringResource(R.string.plan_add_task),
            icon = Icons.Filled.Add,
            onClick = { editingDraft = TaskDraft() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    editingDraft?.let { draft ->
        TaskEditorSheet(
            draft = draft,
            vehicleName = state.vehicle?.name.orEmpty(),
            onDismiss = { editingDraft = null },
            onChange = { editingDraft = it },
            onSave = {
                viewModel.saveTask(draft) { result ->
                    editingDraft = if (result.hasError) result else null
                }
            },
        )
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_task_confirm_title)) },
            text = { Text(stringResource(R.string.delete_task_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showTemplateConfirm) {
        AlertDialog(
            onDismissRequest = { showTemplateConfirm = false },
            title = { Text(stringResource(R.string.plan_apply_template)) },
            text = { Text(stringResource(R.string.template_disclaimer)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyTemplate(spanish)
                    showTemplateConfirm = false
                }) { Text(stringResource(R.string.plan_apply_template)) }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlanTaskRow(
    task: MaintenanceTask,
    dragHandle: @Composable () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val onCard = MaterialTheme.colorScheme.onSurface
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (task.active) Modifier
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            .padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 2.dp)) { dragHandle() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.name,
                style = LedgerText.rowTitleLg,
                color = if (task.active) onCard else onVar,
                textDecoration = if (task.active) null else TextDecoration.LineThrough,
            )
            Text(
                task.intervalSummary().upcase(),
                style = LedgerText.rowMeta,
                color = onVar,
            )
            val warn = warnLine(task)
            if (warn != null) {
                Text(warn, style = LedgerText.mono10, color = onVar)
            }
            if (!task.active) {
                Spacer(Modifier.height(6.dp))
                InactiveChip(onClick = onToggleActive)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.action_more_options),
                    tint = onVar,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit)) },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (task.active) R.string.action_deactivate else R.string.action_activate,
                            ),
                        )
                    },
                    onClick = { menuOpen = false; onToggleActive() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun warnLine(task: MaintenanceTask): String? {
    // The model always carries a warn margin; only surface it when the user has
    // moved it off the defaults, so the row isn't the same line 14 times.
    if (task.warnKmBefore == TaskDraft.DEFAULT_WARN_KM &&
        task.warnDaysBefore == TaskDraft.DEFAULT_WARN_DAYS
    ) return null

    val km = task.warnKmBefore.takeIf { it > 0 }
    val days = task.warnDaysBefore.takeIf { it > 0 }
    return when {
        km != null && days != null ->
            stringResource(R.string.plan_warn_before_both, formatNumber(km), formatNumber(days))
        km != null -> stringResource(R.string.plan_warn_before_km, formatNumber(km))
        days != null -> stringResource(R.string.plan_warn_before_days, formatNumber(days))
        else -> null
    }?.upcase()
}

@Composable
private fun InactiveChip(onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        stringResource(R.string.plan_inactive_chip).upcase(),
        style = LedgerText.tag,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.extraColors.chipOutline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun TemplateTopUpRow(categoryLabel: String, count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .dashedBorder(MaterialTheme.extraColors.chipOutline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.LibraryAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.plan_template_topup, categoryLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extraColors.bodyOnCard,
            modifier = Modifier.weight(1f),
        )
        Text(
            pluralStringResource(R.plurals.plan_template_topup_count, count, count.toString()),
            style = LedgerText.rowMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A 1dp dashed border (Compose has no built-in one). */
private fun Modifier.dashedBorder(color: Color, shape: Shape) = drawWithContent {
    drawContent()
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
    )
    drawOutline(shape.createOutline(size, layoutDirection, this), color, style = stroke)
}

@Composable
private fun EmptyPlan(modifier: Modifier = Modifier, onApplyTemplate: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.plan_empty_title),
            style = LedgerText.screenTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.plan_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LedgerPrimaryButton(
            text = stringResource(R.string.plan_apply_template),
            onClick = onApplyTemplate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    draft: TaskDraft,
    vehicleName: String,
    onDismiss: () -> Unit,
    onChange: (TaskDraft) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (draft.isEdit) R.string.task_sheet_edit else R.string.task_sheet_new),
                    style = LedgerText.screenTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (vehicleName.isNotBlank()) {
                    Text(
                        vehicleName.upcase(),
                        style = LedgerText.contextLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val nameError = draft.nameError || draft.duplicateNameError
            LabeledField(
                label = stringResource(R.string.field_task_name),
                value = draft.name,
                onValueChange = {
                    onChange(draft.copy(name = it, nameError = false, duplicateNameError = false))
                },
                isError = nameError,
                errorText = when {
                    draft.nameError -> stringResource(R.string.error_task_name_required)
                    draft.duplicateNameError -> stringResource(R.string.error_task_name_duplicate)
                    else -> null
                },
                primary = true,
            )

            Column {
                FieldLabel(stringResource(R.string.task_field_interval_group), isError = draft.intervalError)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberUnitBox(
                        value = draft.intervalKm,
                        onValueChange = { onChange(draft.copy(intervalKm = it.filter(Char::isDigit), intervalError = false)) },
                        unit = stringResource(R.string.unit_km),
                        isError = draft.intervalError,
                        modifier = Modifier.weight(1f),
                    )
                    NumberUnitBox(
                        value = draft.intervalMonths,
                        onValueChange = { onChange(draft.copy(intervalMonths = it.filter(Char::isDigit), intervalError = false)) },
                        unit = stringResource(R.string.unit_months),
                        isError = draft.intervalError,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        if (draft.intervalError) R.string.error_task_interval_required
                        else R.string.task_field_interval_help,
                    ),
                    style = LedgerText.supporting,
                    color = if (draft.intervalError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column {
                FieldLabel(stringResource(R.string.task_field_warn_group))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberUnitBox(
                        value = draft.warnKmBefore,
                        onValueChange = { onChange(draft.copy(warnKmBefore = it.filter(Char::isDigit))) },
                        unit = stringResource(R.string.unit_km),
                        modifier = Modifier.weight(1f),
                    )
                    NumberUnitBox(
                        value = draft.warnDaysBefore,
                        onValueChange = { onChange(draft.copy(warnDaysBefore = it.filter(Char::isDigit))) },
                        unit = stringResource(R.string.unit_days),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            LabeledField(
                label = stringResource(R.string.field_notes),
                value = draft.notes,
                onValueChange = { onChange(draft.copy(notes = it)) },
                singleLine = false,
                minHeight = 72.dp,
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.field_task_active),
                        style = LedgerText.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.task_field_active_desc),
                        style = LedgerText.supporting,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draft.active,
                    onCheckedChange = { onChange(draft.copy(active = it)) },
                    colors = ledgerSwitchColors(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LedgerSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                )
                LedgerPrimaryButton(
                    text = stringResource(R.string.task_sheet_save),
                    onClick = onSave,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

@Composable
fun ledgerSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.extraColors.ink,
    checkedBorderColor = MaterialTheme.extraColors.ink,
    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)
