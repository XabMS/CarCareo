package com.xabier.carcareo.domain

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * "Today", as a value that keeps up with the calendar.
 *
 * Every computed status in this app is a function of the current date, so a
 * ViewModel that reads `LocalDate.now()` once at construction freezes the whole
 * screen in the past: estimated km stops growing and tasks never roll over into
 * UPCOMING / OVERDUE. This app is opened rarely and its process can easily
 * survive several days in the background, so that is not a theoretical problem.
 *
 * Emits the current date immediately, then a fresh value just after each local
 * midnight. Two things keep it honest:
 *  - while a screen is visible, the delay below fires at midnight;
 *  - when a screen is backgrounded its flow is cancelled (`WhileSubscribed`) and
 *    restarted on return, which re-reads the clock — this is what covers the
 *    common case, since a sleeping device does not run timers on schedule.
 */
fun todayFlow(clock: Clock = Clock.systemDefaultZone()): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(clock)
        emit(now.toLocalDate())

        // A second past midnight, so we never wake up a hair early and re-emit
        // the same date in a tight loop.
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone).plusSeconds(1)
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
    }
}
