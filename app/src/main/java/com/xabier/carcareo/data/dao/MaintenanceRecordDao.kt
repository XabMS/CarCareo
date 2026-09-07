package com.xabier.carcareo.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.RecordTaskCrossRef
import com.xabier.carcareo.data.relation.RecordWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {

    @Transaction
    @Query("SELECT * FROM maintenance_record ORDER BY date DESC, id DESC")
    fun observeAllWithTasks(): Flow<List<RecordWithTasks>>

    @Transaction
    @Query("SELECT * FROM maintenance_record ORDER BY date DESC, id DESC")
    suspend fun getAllWithTasks(): List<RecordWithTasks>

    @Transaction
    @Query("SELECT * FROM maintenance_record WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<RecordWithTasks>>

    @Transaction
    @Query("SELECT * FROM maintenance_record WHERE id = :id")
    suspend fun getWithTasks(id: Long): RecordWithTasks?

    @Transaction
    @Query("SELECT * FROM maintenance_record WHERE vehicleId = :vehicleId ORDER BY date, id")
    suspend fun getWithTasksForVehicle(vehicleId: Long): List<RecordWithTasks>

    @Query("SELECT * FROM maintenance_record WHERE vehicleId = :vehicleId ORDER BY date DESC, id DESC")
    suspend fun getForVehicle(vehicleId: Long): List<MaintenanceRecord>

    @Insert
    suspend fun insert(record: MaintenanceRecord): Long

    @Update
    suspend fun update(record: MaintenanceRecord)

    @Delete
    suspend fun delete(record: MaintenanceRecord)

    @Insert
    suspend fun insertCrossRefs(refs: List<RecordTaskCrossRef>)

    @Query("DELETE FROM record_task_cross_ref WHERE recordId = :recordId")
    suspend fun clearCrossRefs(recordId: Long)

    /** Replace a record and its task links atomically. */
    @Transaction
    suspend fun saveWithTasks(record: MaintenanceRecord, taskIds: List<Long>): Long {
        val recordId = if (record.id == 0L) {
            insert(record)
        } else {
            update(record)
            clearCrossRefs(record.id)
            record.id
        }
        if (taskIds.isNotEmpty()) {
            insertCrossRefs(taskIds.map { RecordTaskCrossRef(recordId = recordId, taskId = it) })
        }
        return recordId
    }
}
