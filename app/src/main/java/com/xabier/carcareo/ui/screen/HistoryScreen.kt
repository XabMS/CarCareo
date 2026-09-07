package com.xabier.carcareo.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xabier.carcareo.R
import com.xabier.carcareo.data.relation.RecordWithTasks
import com.xabier.carcareo.ui.component.HairlineDivider
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.format.formatCost
import com.xabier.carcareo.ui.format.formatDayMonth
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.history.HistoryViewModel
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import java.math.BigDecimal

/**
 * History tab of the vehicle shell (design handoff §3): a cost pair, task filter
 * chips, then one hairline-ruled table per calendar year.
 */
@Composable
fun HistoryTab(
    viewModel: HistoryViewModel,
    onEditRecord: (recordId: Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RecordWithTasks?>(null) }

    if (state.loading) {
        Spacer(Modifier.fillMaxSize())
        return
    }

    val byYear = remember(state.entries) {
        state.entries
            .groupBy { it.record.date.year }
            .toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.showCosts) {
            item { CostPair(state.totalCost, state.last12MonthsCost) }
        }

        if (state.filterableTasks.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChipLedger(
                        label = stringResource(R.string.history_filter_all),
                        selected = state.activeFilterTaskId == null,
                        onClick = { viewModel.setFilter(null) },
                    )
                    state.filterableTasks.forEach { task ->
                        FilterChipLedger(
                            label = task.name,
                            selected = state.activeFilterTaskId == task.id,
                            onClick = { viewModel.setFilter(task.id) },
                        )
                    }
                }
            }
        }

        if (state.entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            byYear.forEach { (year, entries) ->
                val yearTotal = entries.fold(BigDecimal.ZERO) { acc, e ->
                    acc + (e.record.cost ?: BigDecimal.ZERO)
                }
                item(key = "year-$year") {
                    SectionLabelRow(
                        label = year.toString(),
                        trailing = if (yearTotal.signum() > 0) formatCost(yearTotal) else null,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                item(key = "table-$year") {
                    LedgerCard {
                        entries.forEachIndexed { i, entry ->
                            if (i > 0) HairlineDivider()
                            RecordRow(
                                entry = entry,
                                onClick = { onEditRecord(entry.record.id) },
                                onDelete = { pendingDelete = entry },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_record_confirm_title)) },
            text = { Text(stringResource(R.string.delete_record_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(entry.record)
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
}

@Composable
private fun CostPair(total: BigDecimal, last12: BigDecimal) {
    LedgerCard {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            CostCell(stringResource(R.string.history_total_cost), total, Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline),
            )
            CostCell(stringResource(R.string.history_last12_cost), last12, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CostCell(label: String, amount: BigDecimal, modifier: Modifier = Modifier) {
    Column(modifier.padding(14.dp)) {
        Text(
            label.upcase(),
            style = LedgerText.mono10Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            formatCost(amount),
            style = LedgerText.cardNumberLg,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FilterChipLedger(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .height(30.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(MaterialTheme.extraColors.ink)
                else Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.upcase(),
            style = LedgerText.rowMeta,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.extraColors.bodyOnCard,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecordRow(
    entry: RecordWithTasks,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val record = entry.record
    val sep = stringResource(R.string.remaining_separator)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(56.dp)) {
            Text(
                formatDayMonth(record.date).upcase(),
                style = LedgerText.rowMeta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                formatNumber(record.odometerKm),
                style = LedgerText.mono10,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (entry.tasks.isEmpty()) stringResource(R.string.history_unplanned)
                else entry.tasks.joinToString(sep) { it.name },
                style = LedgerText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val meta = listOfNotNull(
                record.workshop?.takeIf { it.isNotBlank() },
                record.notes?.takeIf { it.isNotBlank() },
            ).joinToString(sep)
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            record.attachmentUri?.let { uri ->
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                            )
                        }
                    },
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.history_invoice).upcase(),
                        style = LedgerText.rowMeta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            record.cost?.let {
                Text(
                    formatCost(it),
                    style = LedgerText.rowMeta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = LedgerText.rowTitle.fontSize),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
