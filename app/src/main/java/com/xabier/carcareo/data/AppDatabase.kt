package com.xabier.carcareo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xabier.carcareo.data.dao.MaintenanceRecordDao
import com.xabier.carcareo.data.dao.MaintenanceTaskDao
import com.xabier.carcareo.data.dao.VehicleDao
import com.xabier.carcareo.data.entity.MaintenanceRecord
import com.xabier.carcareo.data.entity.MaintenanceTask
import com.xabier.carcareo.data.entity.RecordTaskCrossRef
import com.xabier.carcareo.data.entity.Vehicle

@Database(
    entities = [
        Vehicle::class,
        MaintenanceTask::class,
        MaintenanceRecord::class,
        RecordTaskCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceTaskDao(): MaintenanceTaskDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "carcareo.db",
            )
                // Room turns SQLite foreign keys on for us, so the ON DELETE CASCADE
                // declared on the entities is what wipes a vehicle's tasks, records
                // and cross-refs (relied on by the "replace all" import).
                .build()
    }
}
