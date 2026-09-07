package com.xabier.carcareo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * N:N link between a [MaintenanceRecord] and the [MaintenanceTask]s it covers.
 * See section 3.4. One workshop visit can reset eight tasks with a single record.
 */
@Entity(
    tableName = "record_task_cross_ref",
    primaryKeys = ["recordId", "taskId"],
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MaintenanceTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recordId"), Index("taskId")],
)
data class RecordTaskCrossRef(
    val recordId: Long,
    val taskId: Long,
)
