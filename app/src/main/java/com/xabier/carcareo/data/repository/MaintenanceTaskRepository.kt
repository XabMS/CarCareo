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

    /**
     * Applies the built-in template for [category]. Does not clear existing tasks,
     * and skips any template task whose name the vehicle already uses — applying a
     * template twice must not leave two "Engine oil" lines behind (see [addNew]).
     */
    suspend fun applyTemplate(vehicleId: Long, category: VehicleCategory, spanish: Boolean) {
        addNew(vehicleId, MaintenanceTemplates.instantiate(category, vehicleId, spanish))
    }

    /** Copies every task from [sourceVehicleId] into [targetVehicleId]. */
    suspend fun duplicatePlan(sourceVehicleId: Long, targetVehicleId: Long) {
        val copies = dao.getForVehicle(sourceVehicleId)
            .sortedBy { it.sortOrder }
            .map { it.copy(id = 0, vehicleId = targetVehicleId) }
        addNew(targetVehicleId, copies)
    }

    /**
     * Appends [candidates] to the vehicle's plan, dropping names it already has.
     *
     * Task names must stay unique per vehicle: backups link records to tasks by
     * name within the vehicle (spec 7), so a duplicate would make the file
     * ambiguous and merge two tasks' history on import.
     */
    private suspend fun addNew(vehicleId: Long, candidates: List<MaintenanceTask>) {
        val existing = dao.getForVehicle(vehicleId)
        val taken = existing.mapTo(HashSet()) { it.name.lowercase() }
        var order = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val toInsert = candidates.mapNotNull { task ->
            if (!taken.add(task.name.lowercase())) null else task.copy(sortOrder = order++)
        }
        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
    }
}
