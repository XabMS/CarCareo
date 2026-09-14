package com.xabier.carcareo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xabier.carcareo.data.entity.MaintenanceTask
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceTaskDao {

    @Query("SELECT * FROM maintenance_task ORDER BY vehicleId, sortOrder")
    fun observeAll(): Flow<List<MaintenanceTask>>

    @Query("SELECT * FROM maintenance_task ORDER BY vehicleId, sortOrder")
    suspend fun getAll(): List<MaintenanceTask>

    @Query("SELECT * FROM maintenance_task WHERE vehicleId = :vehicleId ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceTask>>

    @Query("SELECT * FROM maintenance_task WHERE vehicleId = :vehicleId AND active = 1 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeActiveForVehicle(vehicleId: Long): Flow<List<MaintenanceTask>>

    @Query("SELECT * FROM maintenance_task WHERE vehicleId = :vehicleId")
    suspend fun getForVehicle(vehicleId: Long): List<MaintenanceTask>

    @Query("SELECT * FROM maintenance_task WHERE id = :id")
    suspend fun get(id: Long): MaintenanceTask?

    @Insert
    suspend fun insert(task: MaintenanceTask): Long

    @Insert
    suspend fun insertAll(tasks: List<MaintenanceTask>): List<Long>

    @Update
    suspend fun update(task: MaintenanceTask)

    @Update
    suspend fun updateAll(tasks: List<MaintenanceTask>)

    @Delete
    suspend fun delete(task: MaintenanceTask)

    /** Appends [task] at the end of the vehicle's plan, atomically. */
    @Transaction
    suspend fun insertAppending(task: MaintenanceTask): Long {
        val nextOrder = (getForVehicle(task.vehicleId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        return insert(task.copy(sortOrder = nextOrder))
    }

    /**
     * Appends [candidates] to the vehicle's plan, dropping names it already has,
     * atomically. See [com.xabier.carcareo.data.repository.MaintenanceTaskRepository]'s
     * old `addNew` for why duplicate names must never be inserted.
     */
    @Transaction
    suspend fun insertAllAppending(vehicleId: Long, candidates: List<MaintenanceTask>): List<Long> {
        val existing = getForVehicle(vehicleId)
        val taken = existing.mapTo(HashSet()) { it.name.lowercase() }
        var order = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val toInsert = candidates.mapNotNull { task ->
            if (!taken.add(task.name.lowercase())) null else task.copy(sortOrder = order++)
        }
        return if (toInsert.isNotEmpty()) insertAll(toInsert) else emptyList()
    }
}
