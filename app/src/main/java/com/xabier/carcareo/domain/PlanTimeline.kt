package com.xabier.carcareo.domain

import kotlin.math.roundToInt

/**
 * Pure derivation for the Overview tab's "Road ahead" timeline and its
 * due-now / soon / later buckets (design handoff §2). No Android, no formatting.
 *
 * The timeline is a sketch, not a full list: overdue tasks collapse to a single
 * pinned marker, forward markers are spaced out and capped, and everything else
 * is left to the buckets below.
 */

data class TimelineMarker(
    val taskId: Long,
    val label: String,
    /** Distance from "now" in km, 0..windowKm. The overdue cluster sits at 0. */
    val kmFromNow: Int,
    val status: TaskStatus,
    /** Alternate the label above / below the axis to reduce collisions. */
    val labelAbove: Boolean,
    /** True for the single overdue marker pinned to the left edge. */
    val pinnedLeft: Boolean,
    /** km past due for the most urgent overdue task, when it has a km interval. */
    val overByKm: Int?,
    /** How many overdue tasks this pinned marker stands for (0 for forward markers). */
    val clusterCount: Int,
)

data class PlanTimeline(
    val currentKm: Int,
    val windowKm: Int,
    val markers: List<TimelineMarker>,
    val dueNow: List<TaskWithStatus>,
    val soon: List<TaskWithStatus>,
    val later: List<TaskWithStatus>,
) {
    companion object {
        const val MIN_WINDOW_KM = 12_000
        const val MAX_FORWARD_MARKERS = 3
        const val WINDOW_STEP_KM = 1_000
        const val DAYS_PER_YEAR = 365.0
        /** Forward markers closer than this fraction of the window are dropped. */
        const val MIN_GAP_FRACTION = 0.18
    }
}

fun buildPlanTimeline(status: VehiclePlanStatus, annualKmEstimate: Int?): PlanTimeline {
    val active = status.activeOrdered

    val dueNow = active.filter { it.computation.status == TaskStatus.OVERDUE }
    val soon = active.filter { it.computation.status == TaskStatus.UPCOMING }
    val later = active.filter { it.computation.status == TaskStatus.OK }

    fun kmFromNow(c: TaskComputation): Int? {
        c.kmRemaining?.let { return it.coerceAtLeast(0) }
        val days = c.daysRemaining ?: return null
        val annual = annualKmEstimate?.takeIf { it > 0 } ?: return null
        return (days / PlanTimeline.DAYS_PER_YEAR * annual).roundToInt().coerceAtLeast(0)
    }

    val forward = (soon + later)
        .mapNotNull { tws -> kmFromNow(tws.computation)?.let { tws to it } }
        .sortedBy { it.second }

    val windowKm = maxOf(
        PlanTimeline.MIN_WINDOW_KM,
        roundUpTo(forward.lastOrNull()?.second ?: 0, PlanTimeline.WINDOW_STEP_KM),
    )

    val minGap = (windowKm * PlanTimeline.MIN_GAP_FRACTION).toInt()
    val spacedForward = buildList {
        var lastKm: Int? = null
        for ((tws, km) in forward) {
            if (lastKm != null && km - lastKm < minGap) continue
            add(tws to km)
            lastKm = km
            if (size >= PlanTimeline.MAX_FORWARD_MARKERS) break
        }
    }

    val markers = buildList {
        if (dueNow.isNotEmpty()) {
            val top = dueNow.first() // activeOrdered is urgency-sorted
            add(
                TimelineMarker(
                    taskId = top.task.id,
                    label = top.task.name,
                    kmFromNow = 0,
                    status = TaskStatus.OVERDUE,
                    labelAbove = true,
                    pinnedLeft = true,
                    overByKm = top.computation.kmRemaining?.takeIf { it < 0 }?.let { -it },
                    clusterCount = dueNow.size,
                )
            )
        }
        spacedForward.forEach { (tws, km) ->
            add(
                TimelineMarker(
                    taskId = tws.task.id,
                    label = tws.task.name,
                    kmFromNow = km.coerceAtMost(windowKm),
                    status = tws.computation.status,
                    labelAbove = false,
                    pinnedLeft = false,
                    overByKm = null,
                    clusterCount = 0,
                )
            )
        }
    }.let { list ->
        val anyPinned = list.any { it.pinnedLeft }
        var forwardIndex = 0
        list.map { m ->
            when {
                // Pinned overdue cluster sits below the axis (its own left column).
                m.pinnedLeft -> m.copy(labelAbove = false)
                // With a pinned cluster occupying the lower-left, keep every
                // forward label above so nothing overlaps it.
                anyPinned -> m.copy(labelAbove = true)
                else -> m.copy(labelAbove = (forwardIndex++) % 2 == 0)
            }
        }
    }

    return PlanTimeline(
        currentKm = status.currentKm,
        windowKm = windowKm,
        markers = markers,
        dueNow = dueNow,
        soon = soon,
        later = later,
    )
}

private fun roundUpTo(value: Int, step: Int): Int =
    if (value <= 0) 0 else ((value + step - 1) / step) * step
