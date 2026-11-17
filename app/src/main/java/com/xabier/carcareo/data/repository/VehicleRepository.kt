package com.xabier.carcareo.data.repository

import androidx.room.withTransaction
import com.xabier.carcareo.data.AppDatabase
import com.xabier.carcareo.data.DataHooks
import com.xabier.carcareo.data.entity.Vehicle
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Thin layer over the vehicle DAO. Repositories exist so ViewModels never touch
 * Room directly (spec architecture) and so cross-entity operations have one home.
 */
class VehicleRepository(
    private val db: AppDatabase,
    private val hooks: DataHooks = DataHooks.None,
) {

    private val dao = db.vehicleDao()
    private val recordDao = db.maintenanceRecordDao()

    fun observeActive(): Flow<List<Vehicle>> = dao.observeActive()

    fun observeArchived(): Flow<List<Vehicle>> = dao.observeArchived()

    fun observeAll(): Flow<List<Vehicle>> = dao.observeAll()

    fun observe(id: Long): Flow<Vehicle?> = dao.observe(id)

    suspend fun get(id: Long): Vehicle? = dao.get(id)

    suspend fun add(vehicle: Vehicle): Long = dao.insert(vehicle)

    suspend fun update(vehicle: Vehicle) = dao.update(vehicle)

    /**
     * Permanently deletes a vehicle with its whole plan and history (cascade).
     * Attachments only its records pointed at lose their read grant.
     */
    suspend fun delete(id: Long) {
        val orphaned = db.withTransaction {
            val uris = recordDao.attachmentUris(id)
            dao.deleteById(id)
            uris.filter { recordDao.countWithAttachment(it) == 0 }
        }
        orphaned.forEach(hooks::onAttachmentUnused)
        hooks.onVehicleAlertStale(id)
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        dao.setArchived(id, archived)
        if (archived) hooks.onVehicleAlertStale(id)
    }

    /**
     * Records a user-confirmed odometer value. Callers are responsible for having
     * already checked the decrease severity (see domain.OdometerUpdate).
     */
    suspend fun confirmOdometer(id: Long, km: Int, date: LocalDate) =
        dao.confirmOdometer(id, km, date)
}
