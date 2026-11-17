package com.xabier.carcareo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xabier.carcareo.ui.screen.ArchivedVehiclesScreen
import com.xabier.carcareo.ui.screen.GarageScreen
import com.xabier.carcareo.ui.screen.LogMaintenanceScreen
import com.xabier.carcareo.ui.screen.PlanSetupScreen
import com.xabier.carcareo.ui.screen.SettingsScreen
import com.xabier.carcareo.ui.screen.VehicleFormScreen
import com.xabier.carcareo.ui.screen.VehicleShellScreen

/**
 * Single NavHost for the whole app: the garage, the three-tab vehicle shell,
 * the add / edit vehicle form with its plan-setup step, the log / edit record
 * form, archived vehicles and settings.
 *
 * [openVehicleId] is a one-shot request to jump to a vehicle — set when the app
 * is opened from a maintenance notification — and [onVehicleOpened] clears it.
 *
 * Every navigation goes through [navigateFrom] / [popFrom]: a second tap that
 * lands while the first one's transition is still running must do nothing.
 * Unguarded, a double tap on a card opens the screen twice, and a double tap on
 * Back pops the garage as well and leaves an empty, blank NavHost.
 */
@Composable
fun CarCareoNavGraph(
    navController: NavHostController = rememberNavController(),
    openVehicleId: Long? = null,
    onVehicleOpened: () -> Unit = {},
) {
    val vehicleIdArg = listOf(
        navArgument(Destinations.VEHICLE_ID_ARG) { type = NavType.LongType },
    )

    LaunchedEffect(openVehicleId) {
        if (openVehicleId != null) {
            // Rebuild the stack as garage -> vehicle, whatever was open before.
            navController.navigate(Destinations.vehicleDetail(openVehicleId)) {
                popUpTo(Destinations.GARAGE)
                launchSingleTop = true
            }
            onVehicleOpened()
        }
    }

    NavHost(navController = navController, startDestination = Destinations.GARAGE) {

        composable(Destinations.GARAGE) { entry ->
            GarageScreen(
                onAddVehicle = { navController.navigateFrom(entry, Destinations.VEHICLE_FORM_NEW) },
                onOpenVehicle = { navController.navigateFrom(entry, Destinations.vehicleDetail(it)) },
                onOpenArchived = { navController.navigateFrom(entry, Destinations.ARCHIVED) },
                onOpenSettings = { navController.navigateFrom(entry, Destinations.SETTINGS) },
                onLogWork = { vId, taskId ->
                    navController.navigateFrom(entry, Destinations.logMaintenance(vId, taskId))
                },
            )
        }

        // The three vehicle-scoped routes all render the same shell and differ
        // only in the tab they open on.
        listOf(
            Destinations.VEHICLE_DETAIL to VehicleTab.OVERVIEW,
            Destinations.PLAN_EDITOR to VehicleTab.PLAN,
            Destinations.HISTORY to VehicleTab.HISTORY,
        ).forEach { (route, tab) ->
            composable(route, arguments = vehicleIdArg) { entry ->
                val id = entry.vehicleId()
                VehicleShellScreen(
                    vehicleId = id,
                    initialTab = tab,
                    onBack = { navController.popFrom(entry) },
                    onEditVehicle = { navController.navigateFrom(entry, Destinations.vehicleFormEdit(it)) },
                    onLogMaintenance = { vId, taskId ->
                        navController.navigateFrom(entry, Destinations.logMaintenance(vId, taskId))
                    },
                    onEditRecord = { vId, recordId ->
                        navController.navigateFrom(entry, Destinations.recordEdit(vId, recordId))
                    },
                )
            }
        }

        composable(Destinations.VEHICLE_FORM_NEW) { entry ->
            VehicleFormScreen(
                onBack = { navController.popFrom(entry) },
                onSaved = { id ->
                    // New vehicle -> plan setup step, then its detail. Drop the form
                    // and the setup screen from the back stack so Back goes to the garage.
                    navController.popBackStack()
                    navController.navigate(Destinations.planSetup(id))
                },
            )
        }

        composable(Destinations.PLAN_SETUP, arguments = vehicleIdArg) { entry ->
            val id = entry.vehicleId()
            PlanSetupScreen(
                vehicleId = id,
                onDone = {
                    if (entry.isResumed()) {
                        navController.popBackStack()
                        navController.navigate(Destinations.vehicleDetail(id))
                    }
                },
            )
        }

        composable(Destinations.VEHICLE_FORM_EDIT, arguments = vehicleIdArg) { entry ->
            VehicleFormScreen(
                onBack = { navController.popFrom(entry) },
                // Not lifecycle-guarded: a save finishes asynchronously and must
                // close the form even if the app was backgrounded meanwhile. The
                // ViewModel's own `saving` flag is what stops a double save.
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            Destinations.LOG_MAINTENANCE,
            arguments = vehicleIdArg + navArgument(Destinations.TASK_ID_ARG) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ) { entry ->
            LogMaintenanceScreen(
                vehicleId = entry.vehicleId(),
                onBack = { navController.popFrom(entry) },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            Destinations.RECORD_EDIT,
            arguments = vehicleIdArg + navArgument(Destinations.RECORD_ID_ARG) {
                type = NavType.LongType
            },
        ) { entry ->
            LogMaintenanceScreen(
                vehicleId = entry.vehicleId(),
                onBack = { navController.popFrom(entry) },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Destinations.ARCHIVED) { entry ->
            ArchivedVehiclesScreen(onBack = { navController.popFrom(entry) })
        }

        composable(Destinations.SETTINGS) { entry ->
            SettingsScreen(
                onBack = { navController.popFrom(entry) },
                onOpenArchived = { navController.navigateFrom(entry, Destinations.ARCHIVED) },
            )
        }
    }
}

private fun NavBackStackEntry.vehicleId(): Long =
    checkNotNull(arguments?.getLong(Destinations.VEHICLE_ID_ARG)) {
        "Missing ${Destinations.VEHICLE_ID_ARG} argument"
    }

/**
 * True while [this] is the settled, on-screen destination. As soon as a
 * navigation away from it starts it drops below RESUMED, which is exactly the
 * window in which a repeated tap has to be ignored.
 */
private fun NavBackStackEntry.isResumed(): Boolean =
    lifecycle.currentState == Lifecycle.State.RESUMED

private fun NavHostController.navigateFrom(from: NavBackStackEntry, route: String) {
    if (from.isResumed()) navigate(route)
}

private fun NavHostController.popFrom(from: NavBackStackEntry) {
    if (from.isResumed()) popBackStack()
}
