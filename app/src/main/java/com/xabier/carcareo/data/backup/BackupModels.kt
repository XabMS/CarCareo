package com.xabier.carcareo.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The export/import file format (spec section 7). Deliberately language-neutral:
 *  - enums travel as constant names (MOTO_TERMICA)
 *  - dates as ISO-8601 strings ("2026-03-14")
 *  - money as a plain string with a '.' decimal separator ("189.50")
 *  - record<->task links by task name within the vehicle, never by id, so the
 *    file survives a re-import.
 */
const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupFile(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: String,
    val vehicles: List<BackupVehicle> = emptyList(),
)

@Serializable
data class BackupVehicle(
    val name: String,
    val category: String,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val plate: String? = null,
    val lastConfirmedKm: Int,
    val lastConfirmedKmDate: String,
    val annualKmEstimate: Int,
    val purchaseDate: String? = null,
    val notes: String? = null,
    val archived: Boolean = false,
    val tasks: List<BackupTask> = emptyList(),
    val records: List<BackupRecord> = emptyList(),
)

@Serializable
data class BackupTask(
    val name: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val warnKmBefore: Int = 1_000,
    val warnDaysBefore: Int = 30,
    val notes: String? = null,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

@Serializable
data class BackupRecord(
    val date: String,
    val odometerKm: Int,
    val workshop: String? = null,
    /** Plain decimal string, '.' separator. Null = no cost recorded. */
    val cost: String? = null,
    val notes: String? = null,
    @SerialName("taskNames") val taskNames: List<String> = emptyList(),
    // attachmentUri is intentionally NOT exported (spec 7): it points at a file
    // on the origin device and would not resolve elsewhere.
)
