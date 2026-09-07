package com.xabier.carcareo.notifications

import com.xabier.carcareo.domain.TaskStatus

/**
 * Decides whether to (re)notify about a vehicle, given what was last notified.
 * Pure, so it is unit-tested. Rule: notify if the status got worse, or if it has
 * been at least [repeatAfterDays] since the last notification for this vehicle.
 */
object AlertDedup {

    fun shouldNotify(
        currentStatus: TaskStatus,
        lastStatusName: String?,
        lastEpochDay: Long,
        todayEpochDay: Long,
        repeatAfterDays: Int = 7,
    ): Boolean {
        if (currentStatus == TaskStatus.OK) return false

        val gotWorse = lastStatusName != currentStatus.name &&
            !(lastStatusName == TaskStatus.OVERDUE.name && currentStatus == TaskStatus.UPCOMING)
        val stale = todayEpochDay - lastEpochDay >= repeatAfterDays
        return gotWorse || stale
    }
}
