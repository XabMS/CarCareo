package com.xabier.carcareo.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Small key/value store for the notification toggles (spec F6). SharedPreferences
 * rather than DataStore: two booleans, read synchronously at startup to decide
 * whether to schedule WorkManager jobs — not worth an extra dependency and a
 * suspend read.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("carcareo_prefs", Context.MODE_PRIVATE)

    var maintenanceAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_MAINTENANCE_ALERTS, false)
        set(value) = prefs.edit { putBoolean(KEY_MAINTENANCE_ALERTS, value) }

    var odometerReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_ODOMETER_REMINDER, false)
        set(value) = prefs.edit { putBoolean(KEY_ODOMETER_REMINDER, value) }

    /** Emits the current toggles and every later change. */
    fun observe(): Flow<NotificationPrefs> = callbackFlow {
        trySend(current())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(current())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun snapshot() = current()

    private fun current() = NotificationPrefs(
        maintenanceAlertsEnabled = maintenanceAlertsEnabled,
        odometerReminderEnabled = odometerReminderEnabled,
    )

    // --- per-vehicle dedup for maintenance alerts (avoid daily repeats) ---

    fun lastAlert(vehicleId: Long): AlertMemory = AlertMemory(
        epochDay = prefs.getLong(alertDayKey(vehicleId), Long.MIN_VALUE),
        status = prefs.getString(alertStatusKey(vehicleId), null),
    )

    fun rememberAlert(vehicleId: Long, epochDay: Long, status: String) {
        prefs.edit {
            putLong(alertDayKey(vehicleId), epochDay)
            putString(alertStatusKey(vehicleId), status)
        }
    }

    private fun alertDayKey(id: Long) = "alert_day_$id"
    private fun alertStatusKey(id: Long) = "alert_status_$id"

    companion object {
        private const val KEY_MAINTENANCE_ALERTS = "notif_maintenance_alerts"
        private const val KEY_ODOMETER_REMINDER = "notif_odometer_reminder"
    }
}

data class NotificationPrefs(
    val maintenanceAlertsEnabled: Boolean = false,
    val odometerReminderEnabled: Boolean = false,
)

data class AlertMemory(val epochDay: Long, val status: String?)
