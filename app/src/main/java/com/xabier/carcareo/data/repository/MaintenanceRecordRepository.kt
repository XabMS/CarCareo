package com.xabier.carcareo.data.repository

import com.xabier.carcareo.data.dao.MaintenanceRecordDao
import com.xabier.carcareo.data.dao.VehicleDao
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.relation.RecordWithTasks
import kotlinx.coroutines.flow.Flow

class MaintenanceRecordRepository(
    private val recordDao: MaintenanceRecordDao,
    private val vehicleDao: VehicleDao,
) {

    fun observeForVehicle(vehicleId: Long): Flow<List<RecordWithTasks>> =
        recordDao.observeForVehicle(vehicleId)

    fun observeAllWithTasks(): Flow<List<RecordWithTasks>> =
        recordDao.observeAllWithTasks()

    suspend fun getWithTasks(id: Long): RecordWithTasks? = recordDao.getWithTasks(id)

    /**
     * Saves a record and the tasks it covers, then confirms the vehicle's
     * odometer from it — but only if this record is at least as recent as the
     * last confirmation, so back-filling old paperwork never rewinds "current km"
     * (spec 3.3 / 4.1).
     */
    suspend fun save(record: MaintenanceRecord, taskIds: List<Long>): Long {
        val id = recordDao.saveWithTasks(record, taskIds)
        val vehicle = vehicleDao.get(record.vehicleId)
        if (vehicle != null && !record.date.isBefore(vehicle.lastConfirmedKmDate)) {
            vehicleDao.update(
                vehicle.copy(
                    lastConfirmedKm = record.odometerKm,
                    lastConfirmedKmDate = record.date,
                ),
            )
        }
        return id
    }

    suspend fun update(record: MaintenanceRecord, taskIds: List<Long>): Long =
        save(record, taskIds)

    suspend fun delete(record: MaintenanceRecord) = recordDao.delete(record)
}
