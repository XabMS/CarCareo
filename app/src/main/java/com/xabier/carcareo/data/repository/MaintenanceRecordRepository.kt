package com.xabier.carcareo.data.repository

import androidx.room.withTransaction
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.Vehicle
import com.xabier.carcareo.data.relation.RecordWithTasks
import com.xabier.carcareo.domain.ConfirmedOdometer
import com.xabier.carcareo.domain.OdometerConfirmation
import com.xabier.carcareo.domain.RecordReading
import kotlinx.coroutines.flow.Flow

class MaintenanceRecordRepository(private val db: AppDatabase) {

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
     * [OdometerConfirmation.shouldConfirmOnSave] owns that decision.
     */
    suspend fun save(record: MaintenanceRecord, taskIds: List<Long>): Long = db.withTransaction {
        // Read the stored version before overwriting it: knowing what this record
        // used to say is what lets an edit correct the odometer it had confirmed.
        val previous = if (record.id != 0L) recordDao.getWithTasks(record.id)?.record else null

        val id = recordDao.saveWithTasks(record, taskIds)

        val vehicle = vehicleDao.get(record.vehicleId) ?: return@withTransaction id
        val confirm = OdometerConfirmation.shouldConfirmOnSave(
            saved = record.reading(),
            previous = previous?.reading(),
            confirmed = vehicle.confirmedOdometer(),
        )
        if (confirm) {
            vehicleDao.update(
                vehicle.copy(
                    lastConfirmedKm = record.odometerKm,
                    lastConfirmedKmDate = record.date,
                ),
            )
        }
        id
    }

    suspend fun update(record: MaintenanceRecord, taskIds: List<Long>): Long =
        save(record, taskIds)

    /**
     * Deletes a record and, when the vehicle's confirmed odometer came from that
     * very record, rolls it back to the newest remaining one — otherwise the
     * vehicle would keep quoting a reading no record backs any more (spec 9.11).
     */
    suspend fun delete(record: MaintenanceRecord) = db.withTransaction {
        recordDao.delete(record)

        val vehicle = vehicleDao.get(record.vehicleId) ?: return@withTransaction
        // getForVehicle is ordered newest-first, and the record is already gone.
        val newest = recordDao.getForVehicle(record.vehicleId).firstOrNull()
        val rollback = OdometerConfirmation.rollbackAfterDelete(
            deleted = record.reading(),
            confirmed = vehicle.confirmedOdometer(),
            newestRemaining = newest?.reading(),
        ) ?: return@withTransaction

        vehicleDao.update(
            vehicle.copy(lastConfirmedKm = rollback.km, lastConfirmedKmDate = rollback.date),
        )
    }
}

// Adapters between the Room entities and the pure domain decision above.

private fun MaintenanceRecord.reading() = RecordReading(km = odometerKm, date = date)

private fun Vehicle.confirmedOdometer() =
    ConfirmedOdometer(km = lastConfirmedKm, date = lastConfirmedKmDate)
