package com.xabier.carcareo.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.domain.KmChangeSeverity
import com.xabier.carcareo.domain.OdometerUpdate
import com.xabier.carcareo.domain.PlanTimeline
import com.xabier.carcareo.domain.TaskStatus
import com.xabier.carcareo.domain.TaskWithStatus
import com.xabier.carcareo.domain.buildPlanTimeline
import com.xabier.carcareo.ui.component.DatePickerField
import com.xabier.carcareo.ui.component.LedgerCard
import com.xabier.carcareo.ui.component.LedgerSecondaryButton
import com.xabier.carcareo.ui.component.MiniBar
import com.xabier.carcareo.ui.component.SectionLabelRow
import com.xabier.carcareo.ui.format.formatNumber
import com.xabier.carcareo.ui.plan.color
import com.xabier.carcareo.ui.plan.intervalConsumed
import com.xabier.carcareo.ui.plan.intervalSummary
import com.xabier.carcareo.ui.plan.remainingSummary
import com.xabier.carcareo.ui.plan.textColor
import com.xabier.carcareo.ui.theme.LedgerText
import com.xabier.carcareo.ui.theme.extraColors
import com.xabier.carcareo.ui.upcase
import com.xabier.carcareo.ui.vehicle.VehicleDetailViewModel
import com.xabier.carcareo.ui.vehicle.freshnessText
import java.time.LocalDate

/**
 * Overview tab of the vehicle shell (design handoff §2): odometer block, the
 * "Road ahead" km timeline, and the due-now / soon / later buckets. The plan
 * *editor* is the Plan tab; this list is read-only and logs a single task on tap.
 */
@Composable
fun OverviewTab(
    viewModel: VehicleDetailViewModel,
    onLogTask: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showUpdateKm by rememberSaveable { mutableStateOf(false) }
    var laterExpanded by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()

    val vehicle = state.vehicle
    val odometer = state.odometer
    if (vehicle == null || odometer == null) {
        Spacer(Modifier.fillMaxSize())
        return
    }

    val planStatus = state.planStatus
    val timeline = remember(planStatus, vehicle.annualKmEstimate) {
        planStatus?.let { buildPlanTimeline(it, vehicle.annualKmEstimate) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        OdometerBlock(
            km = odometer.km,
            freshness = odometer.freshnessText(),
            onUpdate = { showUpdateKm = true },
        )

        if (timeline != null && timeline.markers.isNotEmpty()) {
            SectionLabelRow(
                label = stringResource(R.string.overview_road_ahead),
                trailing = stringResource(R.string.overview_road_window, formatNumber(timeline.windowKm)),
                modifier = Modifier.padding(top = 12.dp),
            )
            RoadAheadCard(timeline)
        }

        if (timeline != null) {
            Bucket(
                label = stringResource(R.string.bucket_due_now),
                color = MaterialTheme.colorScheme.error,
                tasks = timeline.dueNow,
                onLogTask = onLogTask,
            )
            Bucket(
                label = stringResource(R.string.bucket_soon),
                color = MaterialTheme.extraColors.soonText,
                tasks = timeline.soon,
                onLogTask = onLogTask,
            )
            LaterBucket(
                color = MaterialTheme.extraColors.ok,
                tasks = timeline.later,
                inactive = planStatus?.inactive.orEmpty(),
                expanded = laterExpanded,
                onToggle = { laterExpanded = !laterExpanded },
                onLogTask = onLogTask,
            )
        } else {
            Text(
                stringResource(R.string.detail_plan_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    if (showUpdateKm) {
        UpdateOdometerDialog(
            previousConfirmedKm = vehicle.lastConfirmedKm,
            onDismiss = { showUpdateKm = false },
            onConfirm = { km, date ->
                viewModel.confirmOdometer(km, date)
                showUpdateKm = false
            },
        )
    }
}

@Composable
private fun OdometerBlock(km: Int, freshness: String, onUpdate: () -> Unit) {
    LedgerCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.odometer_title).upcase(),
                    style = LedgerText.mono10Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatNumber(km),
                        style = LedgerText.bigNumber,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.unit_km),
                        style = LedgerText.bigNumberUnit,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    freshness,
                    style = LedgerText.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LedgerSecondaryButton(
                text = stringResource(R.string.action_update),
                onClick = onUpdate,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

private val TIMELINE_LABEL_WIDTH = 100.dp

@Composable
private fun RoadAheadCard(timeline: PlanTimeline) {
    val axisColor = MaterialTheme.colorScheme.outline
    val ink = MaterialTheme.extraColors.ink
    val onVar = MaterialTheme.colorScheme.onSurfaceVariant
    val bodyOnCard = MaterialTheme.extraColors.bodyOnCard
    val dotColors: List<Pair<Float, Color>> = timeline.markers.map { m ->
        (m.kmFromNow.toFloat() / timeline.windowKm).coerceIn(0f, 1f) to m.status.color()
    }

    LedgerCard {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 12.dp),
        ) {
            val maxX = maxWidth - TIMELINE_LABEL_WIDTH
            fun xFor(m: com.xabier.carcareo.domain.TimelineMarker) =
                maxX * (m.kmFromNow.toFloat() / timeline.windowKm).coerceIn(0f, 1f)

            // Only label markers that won't visually collide with "NOW" or each
            // other; the rest keep just a dot. A phone-width axis fits ~2 labels.
            val labelled = remember(timeline, maxX) {
                var lastX = 40f
                var kept = 0
                timeline.markers.filter { m ->
                    val x = (maxX.value) * (m.kmFromNow.toFloat() / timeline.windowKm).coerceIn(0f, 1f)
                    when {
                        m.pinnedLeft -> {
                            lastX = maxOf(lastX, TIMELINE_LABEL_WIDTH.value * 0.7f)
                            true
                        }
                        kept < 2 && x - lastX >= TIMELINE_LABEL_WIDTH.value * 0.72f -> {
                            lastX = x; kept++; true
                        }
                        else -> false
                    }
                }.map { it.taskId }.toSet()
            }

            Canvas(Modifier.fillMaxSize()) {
                val axisY = size.height * 0.5f
                drawLine(axisColor, Offset(0f, axisY), Offset(size.width, axisY), 1.dp.toPx())
                drawLine(ink, Offset(1f, axisY - 15.dp.toPx()), Offset(1f, axisY + 15.dp.toPx()), 2.dp.toPx())
                dotColors.forEach { (f, c) ->
                    val x = (f * (size.width - 8.dp.toPx())) + 2.dp.toPx()
                    drawCircle(c, 4.5.dp.toPx(), Offset(x, axisY))
                }
            }

            Column(
                Modifier.align(Alignment.CenterStart).offset(y = (-32).dp),
            ) {
                Text(
                    stringResource(R.string.overview_now).upcase(),
                    style = LedgerText.mono10.copy(fontWeight = FontWeight.SemiBold),
                    color = ink,
                )
                Text(
                    formatNumber(timeline.currentKm),
                    style = LedgerText.mono10,
                    color = onVar,
                )
            }

            timeline.markers.forEach { m ->
                if (m.taskId !in labelled) return@forEach
                val labelColor = if (m.status == TaskStatus.OK) bodyOnCard else m.status.textColor()
                Column(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = xFor(m), y = if (m.labelAbove) (-40).dp else 14.dp)
                        .width(TIMELINE_LABEL_WIDTH),
                ) {
                    Text(
                        m.label,
                        style = LedgerText.supporting,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            m.clusterCount > 1 ->
                                stringResource(R.string.timeline_over, formatNumber(m.clusterCount))
                            m.overByKm != null ->
                                stringResource(R.string.timeline_over, formatNumber(m.overByKm))
                            else -> formatNumber(timeline.currentKm + m.kmFromNow)
                        },
                        style = LedgerText.mono10,
                        color = onVar,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}


@Composable
private fun Bucket(
    label: String,
    color: Color,
    tasks: List<TaskWithStatus>,
    onLogTask: (Long) -> Unit,
) {
    if (tasks.isEmpty()) return
    SectionLabelRow(label = label, labelColor = color, modifier = Modifier.padding(top = 14.dp))
    tasks.forEach { tws ->
        BucketRow(tws, onClick = { onLogTask(tws.task.id) })
    }
}

@Composable
private fun LaterBucket(
    color: Color,
    tasks: List<TaskWithStatus>,
    inactive: List<MaintenanceTask>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLogTask: (Long) -> Unit,
) {
    if (tasks.isEmpty() && inactive.isEmpty()) return
    SectionLabelRow(
        label = stringResource(R.string.bucket_later),
        labelColor = color,
        trailing = if (!expanded) formatNumber(tasks.size + inactive.size) else null,
        modifier = Modifier
            .clickable(onClick = onToggle)
            .padding(top = 14.dp),
    )
    Column(Modifier.animateContentSize()) {
        if (expanded) {
            tasks.forEach { tws -> BucketRow(tws, onClick = { onLogTask(tws.task.id) }) }
            inactive.forEach { task -> InactiveBucketRow(task) }
        }
    }
}

@Composable
private fun BucketRow(tws: TaskWithStatus, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                tws.task.name,
                style = LedgerText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = bucketMeta(tws),
                style = LedgerText.rowMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MiniBar(
            fraction = tws.computation.intervalConsumed(),
            color = tws.computation.status.color(),
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun bucketMeta(tws: TaskWithStatus): String {
    if (!tws.computation.hasHistory) return stringResource(R.string.status_no_record)
    val sep = stringResource(R.string.remaining_separator)
    return tws.computation.remainingSummary() + sep + tws.task.intervalSummary()
}

@Composable
private fun InactiveBucketRow(task: MaintenanceTask) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(
            task.name,
            style = LedgerText.rowTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough,
        )
        Text(
            task.intervalSummary(),
            style = LedgerText.rowMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                        stringResource(R.string.update_km_warn_minor, formatNumber(previousConfirmedKm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    KmChangeSeverity.MAJOR_DECREASE -> Column {
                        Text(
                            stringResource(R.string.update_km_warn_major, formatNumber(previousConfirmedKm)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = majorConfirmed, onCheckedChange = { majorConfirmed = it })
                            Text(stringResource(R.string.update_km_confirm_major))
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onConfirm(km!!, date) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
