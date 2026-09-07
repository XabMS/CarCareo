package com.xabier.carcareo.ui.navigation

/**
 * Navigation routes. Kept as plain strings (not type-safe nav) to stay close to
 * the framework and easy to reason about; the spec asks for no framework magic.
 *
 * Screens that act on one vehicle take its id as a path argument.
 */
object Destinations {
    const val GARAGE = "garage"
    const val SETTINGS = "settings"
    const val ARCHIVED = "archived"

    const val VEHICLE_ID_ARG = "vehicleId"
    const val RECORD_ID_ARG = "recordId"
    const val TASK_ID_ARG = "taskId"

    private const val VEHICLE_DETAIL_BASE = "vehicle"
    private const val PLAN_EDITOR_BASE = "plan"
    private const val PLAN_SETUP_BASE = "planSetup"
    private const val LOG_MAINTENANCE_BASE = "log"
    private const val HISTORY_BASE = "history"
    const val VEHICLE_FORM_BASE = "vehicleForm"

    const val VEHICLE_DETAIL = "$VEHICLE_DETAIL_BASE/{$VEHICLE_ID_ARG}"
    const val PLAN_EDITOR = "$PLAN_EDITOR_BASE/{$VEHICLE_ID_ARG}"
    const val PLAN_SETUP = "$PLAN_SETUP_BASE/{$VEHICLE_ID_ARG}"
    const val LOG_MAINTENANCE = "$LOG_MAINTENANCE_BASE/{$VEHICLE_ID_ARG}?$TASK_ID_ARG={$TASK_ID_ARG}"
    const val RECORD_EDIT = "$LOG_MAINTENANCE_BASE/{$VEHICLE_ID_ARG}/{$RECORD_ID_ARG}"
    const val HISTORY = "$HISTORY_BASE/{$VEHICLE_ID_ARG}"

    /** New vehicle: no id. Edit vehicle: existing id. */
    const val VEHICLE_FORM_NEW = VEHICLE_FORM_BASE
    const val VEHICLE_FORM_EDIT = "$VEHICLE_FORM_BASE/{$VEHICLE_ID_ARG}"

    fun vehicleDetail(id: Long) = "$VEHICLE_DETAIL_BASE/$id"
    fun planEditor(id: Long) = "$PLAN_EDITOR_BASE/$id"
    fun planSetup(id: Long) = "$PLAN_SETUP_BASE/$id"
    /** [taskId] pre-selects a single task (row tap); null leaves the default pre-check. */
    fun logMaintenance(id: Long, taskId: Long? = null) =
        if (taskId == null) "$LOG_MAINTENANCE_BASE/$id"
        else "$LOG_MAINTENANCE_BASE/$id?$TASK_ID_ARG=$taskId"
    fun recordEdit(vehicleId: Long, recordId: Long) = "$LOG_MAINTENANCE_BASE/$vehicleId/$recordId"
    fun history(id: Long) = "$HISTORY_BASE/$id"
    fun vehicleFormEdit(id: Long) = "$VEHICLE_FORM_BASE/$id"
}
