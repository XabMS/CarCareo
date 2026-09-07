package com.xabier.carcareo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.ui.AppViewModelProvider
import com.xabier.carcareo.ui.isSpanishUi
import com.xabier.carcareo.ui.plan.PlanEditorViewModel
import com.xabier.carcareo.ui.plan.TaskDraft
import com.xabier.carcareo.ui.plan.intervalSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    viewModel: PlanEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spanish = isSpanishUi()

    var editingDraft by rememberSaveable { mutableStateOf<TaskDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<MaintenanceTask?>(null) }
    var showTemplateConfirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_plan_editor)) },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingDraft = TaskDraft() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.plan_add_task)) },
            )
        },
    ) { padding ->
        when {
            state.loading -> Unit
            state.tasks.isEmpty() -> EmptyPlan(
                modifier = Modifier.fillMaxSize().padding(padding),
                onApplyTemplate = { showTemplateConfirm = true },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                itemsIndexed(state.tasks, key = { _, t -> t.id }) { index, task ->
                    TaskRow(
                        task = task,
                        isFirst = index == 0,
                        isLast = index == state.tasks.lastIndex,
                        onEdit = { editingDraft = TaskDraft.from(task) },
                        onToggleActive = { viewModel.toggleActive(task) },
                        onMoveUp = { viewModel.move(task, up = true) },
                        onMoveDown = { viewModel.move(task, up = false) },
                        onDelete = { pendingDelete = task },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editingDraft?.let { draft ->
        TaskEditorSheet(
            draft = draft,
            onDismiss = { editingDraft = null },
            onChange = { editingDraft = it },
            onSave = {
                viewModel.saveTask(draft) { result ->
                    if (result.nameError || result.intervalError) {
                        editingDraft = result
                    } else {
                        editingDraft = null
                    }
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
private fun TaskRow(
    task: MaintenanceTask,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.active) null else TextDecoration.LineThrough,
            )
            Text(
                text = task.intervalSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!task.active) {
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = onToggleActive, label = { Text(stringResource(R.string.task_inactive)) })
            }
        }
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.action_more_options),
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
                            if (task.active) R.string.field_task_active else R.string.task_inactive,
                        ),
                    )
                },
                onClick = { menuOpen = false; onToggleActive() },
            )
            if (!isFirst) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_up)) },
                    onClick = { menuOpen = false; onMoveUp() },
                )
            }
            if (!isLast) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_down)) },
                    onClick = { menuOpen = false; onMoveDown() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

@Composable
private fun EmptyPlan(modifier: Modifier = Modifier, onApplyTemplate: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.plan_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.plan_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onApplyTemplate) {
            Text(stringResource(R.string.plan_apply_template))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    draft: TaskDraft,
    onDismiss: () -> Unit,
    onChange: (TaskDraft) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (draft.isEdit) R.string.task_sheet_edit else R.string.task_sheet_new),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it, nameError = false)) },
                label = { Text(stringResource(R.string.field_task_name)) },
                isError = draft.nameError,
                supportingText = if (draft.nameError) {
                    { Text(stringResource(R.string.error_task_name_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.intervalKm,
                    onValueChange = {
                        onChange(draft.copy(intervalKm = it.filter(Char::isDigit), intervalError = false))
                    },
                    label = { Text(stringResource(R.string.field_interval_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = draft.intervalError,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.intervalMonths,
                    onValueChange = {
                        onChange(draft.copy(intervalMonths = it.filter(Char::isDigit), intervalError = false))
                    },
                    label = { Text(stringResource(R.string.field_interval_months)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = draft.intervalError,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(
                    if (draft.intervalError) R.string.error_task_interval_required
                    else R.string.field_interval_help,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (draft.intervalError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.warnKmBefore,
                    onValueChange = { onChange(draft.copy(warnKmBefore = it.filter(Char::isDigit))) },
                    label = { Text(stringResource(R.string.field_warn_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.warnDaysBefore,
                    onValueChange = { onChange(draft.copy(warnDaysBefore = it.filter(Char::isDigit))) },
                    label = { Text(stringResource(R.string.field_warn_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onChange(draft.copy(notes = it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.field_task_active), Modifier.weight(1f))
                Switch(
                    checked = draft.active,
                    onCheckedChange = { onChange(draft.copy(active = it)) },
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = onSave) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
