package com.xabier.carcareo.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.RecordTaskCrossRef

/** A record together with the tasks it covers, resolved through the cross-ref table. */
data class RecordWithTasks(
    @Embedded val record: MaintenanceRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordTaskCrossRef::class,
            parentColumn = "recordId",
            entityColumn = "taskId",
        ),
    )
    val tasks: List<MaintenanceTask>,
)
