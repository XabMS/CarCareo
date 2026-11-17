package com.xabier.carcareo.data

/**
 * Side effects the data layer triggers but cannot perform itself, because they
 * need Android services (ContentResolver, NotificationManager). The repositories
 * call these *after* their transaction has committed; the app wires the real
 * implementation in [com.xabier.carcareo.di.AppContainer], tests use [None].
 */
interface DataHooks {

    /** No record references this attachment any more: its SAF read grant can go. */
    fun onAttachmentUnused(uri: String) {}

    /**
     * Whatever was last notified about this vehicle is out of date (work was
     * logged, or the vehicle was archived / deleted): drop the notification and
     * its dedup memory.
     */
    fun onVehicleAlertStale(vehicleId: Long) {}

    object None : DataHooks
}
