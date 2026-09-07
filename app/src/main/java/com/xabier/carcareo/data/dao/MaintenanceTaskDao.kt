package com.xabier.carcareo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
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
}
