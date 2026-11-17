package com.xabier.carcareo.data.backup

import androidx.room.withTransaction
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.DataHooks
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

class BackupRepository(
    private val db: AppDatabase,
    private val hooks: DataHooks = DataHooks.None,
) {

    private val vehicleDao = db.vehicleDao()
    private val taskDao = db.maintenanceTaskDao()
    private val recordDao = db.maintenanceRecordDao()

    // --- Export ---

    suspend fun exportJson(now: java.time.Instant = java.time.Instant.now()): String {
        // One transaction = one consistent snapshot: a save landing between two of
        // these per-vehicle reads must not produce a file that never existed.
        val vehicles = db.withTransaction { readVehicles() }
        val file = BackupFile(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = DateTimeFormatter.ISO_INSTANT.format(now.atZone(ZoneOffset.UTC)),
            vehicles = vehicles,
        )
        return BackupCodec.encode(file)
    }

    private suspend fun readVehicles(): List<BackupVehicle> {
        val tasksByVehicle = taskDao.getAll().groupBy { it.vehicleId }
        // Oldest first, so a file reads top-to-bottom like a service book.
        val recordsByVehicle = recordDao.getAllWithTasks().asReversed()
            .groupBy { it.record.vehicleId }
        return vehicleDao.getAll().map { v ->
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
                initialKm = v.initialKm,
                initialKmDate = v.initialKmDate?.toString(),
                notes = v.notes,
                archived = v.archived,
                tasks = tasksByVehicle[v.id].orEmpty()
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
                records = recordsByVehicle[v.id].orEmpty().map { rwt ->
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
    }

    // --- Import ---

    /**
     * Parses + validates entirely in memory (throws [BackupException], DB
     * untouched), then applies the whole file in a single transaction.
     */
    suspend fun importJson(text: String, mode: ImportMode): ImportResult {
        val backup = BackupCodec.decode(text)   // throws before any write
        var unresolvedLinks = 0
        var replacedVehicleIds = emptyList<Long>()
        var orphanedAttachments = emptyList<String>()
        db.withTransaction {
            // Reset per attempt: Room may re-run this block.
            unresolvedLinks = 0
            if (mode == ImportMode.REPLACE) {
                replacedVehicleIds = vehicleDao.getAll().map { it.id }
                // Imported records never carry attachments, so every URI goes.
                orphanedAttachments = recordDao.attachmentUris(null)
                vehicleDao.deleteAll()
            }
            for (bv in backup.vehicles) {
                unresolvedLinks += writeVehicle(bv)
            }
        }
        orphanedAttachments.forEach(hooks::onAttachmentUnused)
        replacedVehicleIds.forEach(hooks::onVehicleAlertStale)
        return ImportResult(vehicles = backup.vehicles.size, unresolvedLinks = unresolvedLinks)
    }

    /**
     * Names in the file that an existing vehicle already uses (case-insensitive),
     * in file order. An ADD import of such a file is most likely the same backup
     * imported twice, so the UI asks before duplicating. Throws [BackupException]
     * for an invalid file, exactly like [importJson].
     */
    suspend fun namesAlreadyPresent(text: String): List<String> {
        val backup = BackupCodec.decode(text)
        val existing = vehicleDao.getNames().mapTo(HashSet()) { it.trim().lowercase() }
        return backup.vehicles.map { it.name.trim() }
            .filter { it.lowercase() in existing }
            .distinct()
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
                // Files written before these fields existed: fall back to the
                // confirmed reading, as the 2 -> 3 database migration does.
                initialKm = bv.initialKm ?: bv.lastConfirmedKm,
                initialKmDate = LocalDate.parse(bv.initialKmDate ?: bv.lastConfirmedKmDate),
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
