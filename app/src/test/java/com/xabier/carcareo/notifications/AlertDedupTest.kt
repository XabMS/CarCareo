package com.xabier.carcareo.notifications

import com.xabier.carcareo.domain.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDedupTest {

    @Test
    fun `OK status never notifies`() {
        assertFalse(AlertDedup.shouldNotify(TaskStatus.OK, null, 0, 100))
    }

    @Test
    fun `first time a vehicle is due it notifies`() {
        assertTrue(AlertDedup.shouldNotify(TaskStatus.UPCOMING, lastStatusName = null, lastEpochDay = Long.MIN_VALUE, todayEpochDay = 100))
    }

    @Test
    fun `same status the next day does not re-notify`() {
        assertFalse(AlertDedup.shouldNotify(TaskStatus.UPCOMING, "UPCOMING", lastEpochDay = 100, todayEpochDay = 101))
    }

    @Test
    fun `upcoming escalating to overdue notifies immediately`() {
        assertTrue(AlertDedup.shouldNotify(TaskStatus.OVERDUE, "UPCOMING", lastEpochDay = 100, todayEpochDay = 101))
    }

    @Test
    fun `overdue relaxing to upcoming does not notify`() {
        assertFalse(AlertDedup.shouldNotify(TaskStatus.UPCOMING, "OVERDUE", lastEpochDay = 100, todayEpochDay = 101))
    }

    @Test
    fun `same status re-notifies after a week`() {
        assertFalse(AlertDedup.shouldNotify(TaskStatus.OVERDUE, "OVERDUE", lastEpochDay = 100, todayEpochDay = 106))
        assertTrue(AlertDedup.shouldNotify(TaskStatus.OVERDUE, "OVERDUE", lastEpochDay = 100, todayEpochDay = 107))
    }
}
