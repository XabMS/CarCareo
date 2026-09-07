package com.xabier.carcareo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xabier.carcareo.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicle WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicle WHERE archived = 1 ORDER BY name COLLATE NOCASE")
    fun observeArchived(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicle ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    fun observe(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    suspend fun get(id: Long): Vehicle?

    @Query("SELECT * FROM vehicle ORDER BY id")
    suspend fun getAll(): List<Vehicle>

    /** Cascades to tasks, records and cross-refs. Used by "replace all" import. */
    @Query("DELETE FROM vehicle")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)
}
