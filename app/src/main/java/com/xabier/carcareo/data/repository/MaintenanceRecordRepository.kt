package com.xabier.carcareo.data.repository

import androidx.room.withTransaction
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.DataHooks
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.relation.RecordWithTasks
import com.xabier.carcareo.domain.ConfirmedOdometer
import com.xabier.carcareo.domain.OdometerConfirmation
import com.xabier.carcareo.domain.RecordReading
import kotlinx.coroutines.flow.Flow

class MaintenanceRecordRepository(
    private val db: AppDatabase,
    private val hooks: DataHooks = DataHooks.None,
) {

    private val recordDao = db.maintenanceRecordDao()
    private val vehicleDao = db.vehicleDao()

    fun observeForVehicle(vehicleId: Long): Flow<List<RecordWithTasks>> =
        recordDao.observeForVehicle(vehicleId)

    fun observeAllWithTasks(): Flow<List<RecordWithTasks>> =
        recordDao.observeAllWithTasks()

    suspend fun getWithTasks(id: Long): RecordWithTasks? = recordDao.getWithTasks(id)

    /**
     * Saves a record and the tasks it covers, then confirms the vehicle's odometer
     * from it when that record is the best available reading (spec 3.3 / 4.1).
     * [OdometerConfirmation] owns that decision.
     */
    suspend fun save(record: MaintenanceRecord, taskIds: List<Long>): Long {
        var orphanedAttachment: String? = null
        val id = db.withTransaction {
            orphanedAttachment = null
            // Read the stored version before overwriting it: knowing what this record
            // used to say is what lets an edit correct the odometer it had confirmed.
            val previous = if (record.id != 0L) recordDao.getWithTasks(record.id)?.record else null

            val id = recordDao.saveWithTasks(record, taskIds)

            val oldUri = previous?.attachmentUri
            if (oldUri != null && oldUri != record.attachmentUri &&
                recordDao.countWithAttachment(oldUri) == 0
            ) {
                orphanedAttachment = oldUri
            }

            val vehicle = vehicleDao.get(record.vehicleId) ?: return@withTransaction id
            val confirmed = vehicle.confirmedOdometer()
            val newReading = if (previous != null &&
                OdometerConfirmation.isSourceOf(previous.reading(), confirmed)
            ) {
                // The confirmation came from this very record, so it has to be
                // re-derived: usually the edited record itself, but if its date was
                // moved back another record may now be the newest one.
                OdometerConfirmation.newest(
                    recordDao.getForVehicle(record.vehicleId).map { it.reading() },
                )
            } else if (OdometerConfirmation.shouldConfirmOnSave(record.reading(), previous?.reading(), confirmed)) {
                record.reading()
            } else {
                null
            }
            if (newReading != null) {
                vehicleDao.confirmOdometer(vehicle.id, newReading.km, newReading.date)
            }
            id
        }
        orphanedAttachment?.let(hooks::onAttachmentUnused)
        // Work was just logged: whatever the last notification said is stale.
        hooks.onVehicleAlertStale(record.vehicleId)
        return id
    }

    suspend fun update(record: MaintenanceRecord, taskIds: List<Long>): Long =
        save(record, taskIds)

    /**
     * Deletes a record and, when the vehicle's confirmed odometer came from that
     * very record, rolls it back to the newest remaining one — otherwise the
     * vehicle would keep quoting a reading no record backs any more (spec 9.11).
     */
    suspend fun delete(record: MaintenanceRecord) {
        var orphanedAttachment: String? = null
        db.withTransaction {
            orphanedAttachment = null
            recordDao.delete(record)

            val uri = record.attachmentUri
            if (uri != null && recordDao.countWithAttachment(uri) == 0) orphanedAttachment = uri

            val vehicle = vehicleDao.get(record.vehicleId) ?: return@withTransaction
            val newest = OdometerConfirmation.newest(
                recordDao.getForVehicle(record.vehicleId).map { it.reading() },
            )
            val rollback = OdometerConfirmation.rollbackAfterDelete(
                deleted = record.reading(),
                confirmed = vehicle.confirmedOdometer(),
                newestRemaining = newest,
            ) ?: return@withTransaction

            vehicleDao.confirmOdometer(vehicle.id, rollback.km, rollback.date)
        }
        orphanedAttachment?.let(hooks::onAttachmentUnused)
    }
}

// Adapters between the Room entities and the pure domain decision above.

private fun MaintenanceRecord.reading() = RecordReading(km = odometerKm, date = date)

private fun Vehicle.confirmedOdometer() =
    ConfirmedOdometer(km = lastConfirmedKm, date = lastConfirmedKmDate)
