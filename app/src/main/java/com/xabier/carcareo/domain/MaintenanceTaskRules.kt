package com.xabier.carcareo.domain

/**
 * Validation for a plan task (spec 3.2): at least one of the two intervals must
 * be present, and any interval that IS present must be positive.
 */
object MaintenanceTaskRules {

    fun intervalsValid(intervalKm: Int?, intervalMonths: Int?): Boolean {
        val kmOk = intervalKm == null || intervalKm > 0
        val monthsOk = intervalMonths == null || intervalMonths > 0
        val atLeastOne = intervalKm != null || intervalMonths != null
        return kmOk && monthsOk && atLeastOne
    }
}
