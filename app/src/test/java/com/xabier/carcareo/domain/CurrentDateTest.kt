package com.xabier.carcareo.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [todayFlow] is what stops a long-lived screen from freezing on the date it was
 * opened, so the midnight roll-over is worth pinning down. The test runs on
 * virtual time: no real waiting, and the clock below advances in lockstep with
 * the coroutine scheduler so `delay` and `Clock.instant()` never disagree.
 */
@OptIn(ExperimentalCoroutinesApi::class)   // TestCoroutineScheduler / advanceTimeBy
class CurrentDateTest {

    private val madrid: ZoneId = ZoneId.of("Europe/Madrid")

    /** A [Clock] that reads the test scheduler's virtual time. */
    private class VirtualClock(
        private val scheduler: TestCoroutineScheduler,
        private val origin: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId) = VirtualClock(scheduler, origin, zone)
        override fun instant(): Instant = origin.plusMillis(scheduler.currentTime)
    }

    @Test
    fun `emits the current date and a new one after each midnight`() = runTest {
        val start = LocalDate.of(2026, 9, 7).atTime(22, 0).atZone(madrid).toInstant()
        val clock = VirtualClock(testScheduler, start, madrid)

        val seen = mutableListOf<LocalDate>()
        val job = launch { todayFlow(clock).collect { seen += it } }

        runCurrent()
        assertEquals(listOf(LocalDate.of(2026, 9, 7)), seen)

        // Two hours later it is the 8th; the flow must have woken up on its own.
        advanceTimeBy(3.hours)
        assertEquals(listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)), seen)

        // And it keeps going, rather than firing once and stopping.
        advanceTimeBy(24.hours)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)),
            seen,
        )

        job.cancel()
    }

    @Test
    fun `does not re-emit while the day has not changed`() = runTest {
        val start = LocalDate.of(2026, 9, 7).atTime(9, 0).atZone(madrid).toInstant()
        val clock = VirtualClock(testScheduler, start, madrid)

        val seen = mutableListOf<LocalDate>()
        val job = launch { todayFlow(clock).collect { seen += it } }

        runCurrent()
        advanceTimeBy(10.hours)   // 19:00, same day

        assertEquals(listOf(LocalDate.of(2026, 9, 7)), seen)

        job.cancel()
    }
}
