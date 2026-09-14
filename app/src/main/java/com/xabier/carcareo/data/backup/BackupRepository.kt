package com.xabier.carcareo.data.backup

import androidx.room.withTransaction
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.RecordTaskCrossRef
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.entity.VehicleCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class ImportMode {
    /** Wipe everything, then insert the file's contents. */
    REPLACE,

    /** Keep current data; add the file's vehicles as new ones. */
    ADD,
}

/**
 * Outcome of [BackupRepository.importJson]. [unresolvedLinks] counts record
 * task-name references that didn't match any task in that record's vehicle
 * (typo, removed task, stale reference) — those links are simply dropped.
 */
data class ImportResult(val vehicles: Int, val unresolvedLinks: Int)

class BackupRepository(private val db: AppDatabase) {

    private val vehicleDao = db.vehicleDao()
    private val taskDao = db.maintenanceTaskDao()
    private val recordDao = db.maintenanceRecordDao()

    // --- Export ---

    suspend fun exportJson(now: java.time.Instant = java.time.Instant.now()): String {
        val vehicles = vehicleDao.getAll().map { v ->
            BackupVehicle(
                name = v.name,
                category = v.category.name,
                make = v.make,
                model = v.model,
                year = v.year,
                plate = v.plate,
                lastConfirmedKm = v.lastConfirmedKm,
                lastConfirmedKmDate = v.lastConfirmedKmDate.toString(),
                annualKmEstimate = v.annualKmEstimate,
                purchaseDate = v.purchaseDate?.toString(),
                notes = v.notes,
                archived = v.archived,
                tasks = taskDao.getForVehicle(v.id)
                    .sortedBy { it.sortOrder }
                    .map { t ->
                        BackupTask(
                            name = t.name,
                            intervalKm = t.intervalKm,
                            intervalMonths = t.intervalMonths,
                            warnKmBefore = t.warnKmBefore,
                            warnDaysBefore = t.warnDaysBefore,
                            notes = t.notes,
                            active = t.active,
                            sortOrder = t.sortOrder,
                        )
                    },
                records = recordDao.getWithTasksForVehicle(v.id).map { rwt ->
                    BackupRecord(
                        date = rwt.record.date.toString(),
                        odometerKm = rwt.record.odometerKm,
                        workshop = rwt.record.workshop,
                        cost = rwt.record.cost?.toPlainString(),
                        notes = rwt.record.notes,
                        taskNames = rwt.tasks.map { it.name },
                    )
                },
            )
        }
        val file = BackupFile(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = DateTimeFormatter.ISO_INSTANT.format(now.atZone(ZoneOffset.UTC)),
            vehicles = vehicles,
        )
        return BackupCodec.encode(file)
    }

    // --- Import ---

    /**
     * Parses + validates entirely in memory (throws [BackupException], DB
     * untouched), then applies the whole file in a single transaction.
     */
    suspend fun importJson(text: String, mode: ImportMode): ImportResult {
        val backup = BackupCodec.decode(text)   // throws before any write
        var unresolvedLinks = 0
        db.withTransaction {
            if (mode == ImportMode.REPLACE) {
                vehicleDao.deleteAll()
            }
            for (bv in backup.vehicles) {
                unresolvedLinks += writeVehicle(bv)
            }
        }
        return ImportResult(vehicles = backup.vehicles.size, unresolvedLinks = unresolvedLinks)
    }

    /** Returns the number of record task-name references that couldn't be resolved. */
    private suspend fun writeVehicle(bv: BackupVehicle): Int {
        val vehicleId = vehicleDao.insert(
            Vehicle(
                name = bv.name,
                category = VehicleCategory.valueOf(bv.category),
                make = bv.make,
                model = bv.model,
                year = bv.year,
                plate = bv.plate,
                lastConfirmedKm = bv.lastConfirmedKm,
                lastConfirmedKmDate = LocalDate.parse(bv.lastConfirmedKmDate),
                annualKmEstimate = bv.annualKmEstimate,
                purchaseDate = bv.purchaseDate?.let(LocalDate::parse),
                notes = bv.notes,
                archived = bv.archived,
            ),
        )

        // Task name -> new id, for resolving record links (spec 7: links by name).
        val taskIdByName = HashMap<String, Long>()
        for (bt in bv.tasks) {
            val id = taskDao.insert(
                MaintenanceTask(
                    vehicleId = vehicleId,
                    name = bt.name,
                    intervalKm = bt.intervalKm,
                    intervalMonths = bt.intervalMonths,
                    warnKmBefore = bt.warnKmBefore,
                    warnDaysBefore = bt.warnDaysBefore,
                    notes = bt.notes,
                    active = bt.active,
                    sortOrder = bt.sortOrder,
                ),
            )
            taskIdByName[bt.name.lowercase()] = id
        }

        var unresolvedLinks = 0
        for (br in bv.records) {
            val recordId = recordDao.insert(
                MaintenanceRecord(
                    vehicleId = vehicleId,
                    date = LocalDate.parse(br.date),
                    odometerKm = br.odometerKm,
                    workshop = br.workshop,
                    cost = br.cost?.let(::BigDecimal),
                    notes = br.notes,
                    attachmentUri = null,
                ),
            )
            val distinctNames = br.taskNames.map { it.lowercase() }.distinct()
            val refs = distinctNames.mapNotNull { name ->
                taskIdByName[name]?.let { RecordTaskCrossRef(recordId = recordId, taskId = it) }
            }
            unresolvedLinks += distinctNames.size - refs.size
            if (refs.isNotEmpty()) {
                recordDao.insertCrossRefs(refs)
            }
        }
        return unresolvedLinks
    }
}
