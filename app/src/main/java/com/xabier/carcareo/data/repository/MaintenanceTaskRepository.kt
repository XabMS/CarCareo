package com.xabier.carcareo.data.repository

import com.xabier.carcareo.data.dao.MaintenanceTaskDao
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.VehicleCategory
import com.xabier.carcareo.domain.template.MaintenanceTemplates
import kotlinx.coroutines.flow.Flow

class MaintenanceTaskRepository(private val dao: MaintenanceTaskDao) {

    fun observeAll(): Flow<List<MaintenanceTask>> = dao.observeAll()

    fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceTask>> =
        dao.observeForVehicle(vehicleId)

    fun observeActiveForVehicle(vehicleId: Long): Flow<List<MaintenanceTask>> =
        dao.observeActiveForVehicle(vehicleId)

    suspend fun getForVehicle(vehicleId: Long): List<MaintenanceTask> =
        dao.getForVehicle(vehicleId)

    /** Appends a task at the end of the plan. */
    suspend fun add(task: MaintenanceTask): Long {
        val nextOrder = (dao.getForVehicle(task.vehicleId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        return dao.insert(task.copy(sortOrder = nextOrder))
    }

    suspend fun update(task: MaintenanceTask) = dao.update(task)

    suspend fun delete(task: MaintenanceTask) = dao.delete(task)

    suspend fun setActive(task: MaintenanceTask, active: Boolean) =
        dao.update(task.copy(active = active))

    /**
     * Swaps the [sortOrder] of two tasks (used by the move up / move down buttons
     * in the plan editor).
     */
    suspend fun swapOrder(a: MaintenanceTask, b: MaintenanceTask) {
        dao.updateAll(listOf(a.copy(sortOrder = b.sortOrder), b.copy(sortOrder = a.sortOrder)))
    }

    /** Applies the built-in template for [category]. Does not clear existing tasks. */
    suspend fun applyTemplate(vehicleId: Long, category: VehicleCategory, spanish: Boolean) {
        val base = (dao.getForVehicle(vehicleId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val tasks = MaintenanceTemplates.instantiate(category, vehicleId, spanish)
            .mapIndexed { i, t -> t.copy(sortOrder = base + i) }
        dao.insertAll(tasks)
    }

    /** Copies every task from [sourceVehicleId] into [targetVehicleId]. */
    suspend fun duplicatePlan(sourceVehicleId: Long, targetVehicleId: Long) {
        val base = (dao.getForVehicle(targetVehicleId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val copies = dao.getForVehicle(sourceVehicleId)
            .sortedBy { it.sortOrder }
            .mapIndexed { i, t ->
                t.copy(id = 0, vehicleId = targetVehicleId, sortOrder = base + i)
            }
        dao.insertAll(copies)
    }
}
