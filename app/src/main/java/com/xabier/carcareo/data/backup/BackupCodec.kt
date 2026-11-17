package com.xabier.carcareo.data.backup

import com.xabier.carcareo.data.entity.VehicleCategory
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** Why an import was rejected. The DB is never touched when any of these occurs. */
enum class BackupError {
    /** Not valid JSON, or missing required fields / wrong types. */
    MALFORMED,

    /** schemaVersion is newer than this app understands. */
    UNSUPPORTED_VERSION,
}

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(cause)

/**
 * Pure (no Android, no Room) serialization + validation for the backup file.
 * Parsing and validation happen entirely in memory so a bad file can be
 * rejected before any write transaction is opened (spec 7).
 */
object BackupCodec {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /**
     * Parses and fully validates. Throws [BackupException] on any problem;
     * returns a structurally-sound [BackupFile] otherwise.
     */
    fun decode(text: String, today: LocalDate = LocalDate.now()): BackupFile {
        val parsed = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupException(BackupError.MALFORMED, e)
        }

        if (parsed.schemaVersion > BACKUP_SCHEMA_VERSION) {
            throw BackupException(BackupError.UNSUPPORTED_VERSION)
        }
        if (parsed.schemaVersion < 1) {
            throw BackupException(BackupError.MALFORMED)
        }

        try {
            validate(parsed, today)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupError.MALFORMED, e)
        }
        return parsed
    }

    private fun validate(backup: BackupFile, today: LocalDate) {
        // The app never stores a future date, so one in a file means the file is
        // wrong. One day of slack: a backup written late in the evening and read
        // in a time zone further west is legitimately "tomorrow" there.
        val latest = today.plusDays(1)
        fun pastDate(text: String): LocalDate =
            LocalDate.parse(text).also { require(!it.isAfter(latest)) { "date $text is in the future" } }

        for (v in backup.vehicles) {
            require(v.name.isNotBlank())
            VehicleCategory.valueOf(v.category)          // throws IllegalArgumentException if unknown
            val confirmedOn = pastDate(v.lastConfirmedKmDate)
            v.purchaseDate?.let { require(!pastDate(it).isAfter(confirmedOn)) }
            v.initialKmDate?.let(::pastDate)
            v.initialKm?.let { require(it >= 0) }
            require(v.lastConfirmedKm >= 0)
            require(v.annualKmEstimate >= 0)

            // Records reference tasks by name within the vehicle (spec 7), so a
            // repeated name makes those links ambiguous. Rejecting the file is the
            // honest outcome: importing it would silently merge two tasks' history.
            val taskNames = HashSet<String>()
            for (t in v.tasks) {
                require(t.name.isNotBlank())
                require(taskNames.add(t.name.lowercase())) {
                    "vehicle '${v.name}' has more than one task named '${t.name}'"
                }
                require(t.intervalKm != null || t.intervalMonths != null) {
                    "task '${t.name}' has no interval"
                }
                t.intervalKm?.let { require(it > 0) }
                t.intervalMonths?.let { require(it > 0) }
                require(t.warnKmBefore >= 0)
                require(t.warnDaysBefore >= 0)
            }
            for (r in v.records) {
                pastDate(r.date)
                require(r.odometerKm >= 0)
                // NumberFormatException if not a decimal.
                r.cost?.let { require(BigDecimal(it) >= BigDecimal.ZERO) }
            }
        }
    }
}
