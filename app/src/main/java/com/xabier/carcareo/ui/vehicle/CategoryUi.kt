package com.xabier.carcareo.ui.vehicle

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.xabier.carcareo.R
import com.xabier.carcareo.data.entity.VehicleCategory

@get:StringRes
val VehicleCategory.labelRes: Int
    get() = when (this) {
        VehicleCategory.MOTO_TERMICA -> R.string.category_moto_termica
        VehicleCategory.COCHE_TERMICO -> R.string.category_coche_termico
        VehicleCategory.COCHE_ELECTRICO -> R.string.category_coche_electrico
    }

val VehicleCategory.icon: ImageVector
    get() = when (this) {
        VehicleCategory.MOTO_TERMICA -> Icons.Filled.TwoWheeler
        VehicleCategory.COCHE_TERMICO -> Icons.Filled.DirectionsCar
        VehicleCategory.COCHE_ELECTRICO -> Icons.Filled.ElectricCar
    }
