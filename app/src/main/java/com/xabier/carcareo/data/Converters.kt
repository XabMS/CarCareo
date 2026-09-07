package com.xabier.carcareo.data

import androidx.room.TypeConverter
import com.xabier.carcareo.data.entity.VehicleCategory
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Room only stores primitives. These converters keep the entity types honest:
 *  - LocalDate  <-> ISO-8601 text ("2026-03-14"), sorts correctly as text
 *  - BigDecimal <-> plain string, so money keeps its exact scale
 *  - VehicleCategory <-> its constant name, stable for JSON export too
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun fromVehicleCategory(value: VehicleCategory?): String? = value?.name

    @TypeConverter
    fun toVehicleCategory(value: String?): VehicleCategory? =
        value?.let(VehicleCategory::valueOf)
}
