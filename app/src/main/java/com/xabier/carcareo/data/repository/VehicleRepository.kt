package com.xabier.carcareo.data.repository

import com.xabier.carcareo.data.dao.VehicleDao
import com.xabier.carcareo.data.entity.Vehicle
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Thin layer over [VehicleDao]. Repositories exist so ViewModels never touch Room
 * directly (spec architecture) and so cross-entity operations have one home.
 */
class VehicleRepository(private val dao: VehicleDao) {

    fun observeActive(): Flow<List<Vehicle>> = dao.observeActive()

    fun observeArchived(): Flow<List<Vehicle>> = dao.observeArchived()

    fun observeAll(): Flow<List<Vehicle>> = dao.observeAll()

    fun observe(id: Long): Flow<Vehicle?> = dao.observe(id)

    suspend fun get(id: Long): Vehicle? = dao.get(id)

    suspend fun add(vehicle: Vehicle): Long = dao.insert(vehicle)

    suspend fun update(vehicle: Vehicle) = dao.update(vehicle)

    suspend fun delete(vehicle: Vehicle) = dao.delete(vehicle)

    suspend fun setArchived(id: Long, archived: Boolean) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(archived = archived))
    }

    /**
     * Records a user-confirmed odometer value. Callers are responsible for having
     * already checked the decrease severity (see domain.OdometerUpdate).
     */
    suspend fun confirmOdometer(id: Long, km: Int, date: LocalDate) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(lastConfirmedKm = km, lastConfirmedKmDate = date))
    }
}
