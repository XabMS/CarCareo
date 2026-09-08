package com.xabier.carcareo.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.xabier.carcareo.MainActivity
import com.xabier.carcareo.R

/**
 * Notification plumbing for the optional reminders (spec F6). Two channels so the
 * user can tune them separately in system settings; maintenance alerts are
 * grouped one-per-vehicle with a summary.
 */
object Notifications {

    const val CHANNEL_MAINTENANCE = "maintenance_alerts"
    const val CHANNEL_ODOMETER = "odometer_reminder"

    private const val GROUP_MAINTENANCE = "com.xabier.carcareo.MAINTENANCE"
    private const val SUMMARY_ID = 1
    private const val ODOMETER_ID = 2
    private const val VEHICLE_ID_OFFSET = 1_000

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MAINTENANCE,
                context.getString(R.string.notif_channel_maintenance),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_maintenance_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ODOMETER,
                context.getString(R.string.notif_channel_odometer),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.notif_channel_odometer_desc) },
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** One notification per vehicle plus a group summary. Caller checks the runtime permission. */
    fun postMaintenanceAlert(context: Context, vehicleId: Long, vehicleName: String, line: String) {
        val nm = NotificationManagerCompat.from(context)
        val perVehicle = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(R.drawable.ic_logo_mono)
            .setContentTitle(vehicleName)
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(line))
            .setGroup(GROUP_MAINTENANCE)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        val summary = NotificationCompat.Builder(context, CHANNEL_MAINTENANCE)
            .setSmallIcon(R.drawable.ic_logo_mono)
            .setContentTitle(context.getString(R.string.notif_maintenance_summary_title))
            .setGroup(GROUP_MAINTENANCE)
            .setGroupSummary(true)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(VEHICLE_ID_OFFSET + vehicleId.toInt(), perVehicle)
            nm.notify(SUMMARY_ID, summary)
        } catch (_: SecurityException) {
            // Permission revoked between the check and here; nothing to do.
        }
    }

    fun postOdometerReminder(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ODOMETER)
            .setSmallIcon(R.drawable.ic_logo_mono)
            .setContentTitle(context.getString(R.string.notif_odometer_title))
            .setContentText(context.getString(R.string.notif_odometer_body))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ODOMETER_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
