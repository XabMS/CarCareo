package com.xabier.carcareo.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers [MIGRATION_2_3] on the JVM, so it runs in CI with the rest of the unit
 * tests (the instrumented `MigrationTest` needs a device).
 *
 * A real version-2 database file is built from the exported schema JSON, then
 * opened through the app's own Room builder. Room runs the migration and then
 * validates the result against the version-3 entities, so a migration that
 * leaves the schema even slightly different from what the entities declare
 * fails here instead of crashing on a user's phone at startup.
 */
@RunWith(RobolectricTestRunner::class)
class Migration2To3Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-2-3-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `existing vehicles keep their data and get the first reading back-filled`() = runTest {
        createVersion2Database().use { helper ->
            helper.writableDatabase.execSQL(
                "INSERT INTO vehicle (id, name, category, lastConfirmedKm, lastConfirmedKmDate, annualKmEstimate, archived) " +
                    "VALUES (1, 'Old car', 'COCHE_TERMICO', 84000, '2026-03-14', 9000, 0)",
            )
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val vehicle = db.vehicleDao().get(1)!!
            assertEquals("Old car", vehicle.name)
            assertEquals(84_000, vehicle.lastConfirmedKm)
            assertEquals(84_000, vehicle.initialKm)
            assertEquals(LocalDate.of(2026, 3, 14), vehicle.initialKmDate)
        } finally {
            db.close()
        }
    }

    /** Builds an on-disk database exactly as version 2 of the app created it. */
    private fun createVersion2Database(): SupportSQLiteOpenHelper {
        val schema = Json.parseToJsonElement(
            File("schemas/com.xabier.carcareo.data.AppDatabase/2.json").readText(),
        ).jsonObject.getValue("database").jsonObject
        val statements = schema.getValue("entities").jsonArray.flatMap { entity ->
            val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
            val create = entity.jsonObject.getValue("createSql").jsonPrimitive.content
            val indices = entity.jsonObject["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject.getValue("createSql").jsonPrimitive.content
            }
            (listOf(create) + indices).map { it.replace("\${TABLE_NAME}", table) }
        }

        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build(),
        ).also { it.writableDatabase } // force creation now
    }
}
