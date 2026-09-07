package com.xabier.carcareo.ui.navigation

import androidx.compose.runtime.Composable
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
 * Single NavHost for the whole app. All six screens plus the add/edit-vehicle
 * and archived sub-screens are wired here. Screens still in placeholder state
 * (plan editor, log, history) get filled in by their phases.
 */
@Composable
fun CarCareoNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val vehicleIdArg = listOf(
        navArgument(Destinations.VEHICLE_ID_ARG) { type = NavType.LongType },
    )

    NavHost(navController = navController, startDestination = Destinations.GARAGE) {

        composable(Destinations.GARAGE) {
            GarageScreen(
                onAddVehicle = { navController.navigate(Destinations.VEHICLE_FORM_NEW) },
                onOpenVehicle = { navController.navigate(Destinations.vehicleDetail(it)) },
                onOpenArchived = { navController.navigate(Destinations.ARCHIVED) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onLogWork = { vId, taskId ->
                    navController.navigate(Destinations.logMaintenance(vId, taskId))
                },
            )
        }

        // The three vehicle-scoped routes all render the same shell — kept as
        // separate routes so notifications and deep links still resolve.
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
                    onBack = navController::popBackStack,
                    onEditVehicle = { navController.navigate(Destinations.vehicleFormEdit(it)) },
                    onLogMaintenance = { vId, taskId ->
                        navController.navigate(Destinations.logMaintenance(vId, taskId))
                    },
                    onEditRecord = { vId, recordId ->
                        navController.navigate(Destinations.recordEdit(vId, recordId))
                    },
                )
            }
        }

        composable(Destinations.VEHICLE_FORM_NEW) {
            VehicleFormScreen(
                onBack = navController::popBackStack,
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
                    navController.popBackStack()
                    navController.navigate(Destinations.vehicleDetail(id))
                },
            )
        }

        composable(Destinations.VEHICLE_FORM_EDIT, arguments = vehicleIdArg) {
            VehicleFormScreen(
                onBack = navController::popBackStack,
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
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack,
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
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack,
            )
        }

        composable(Destinations.ARCHIVED) {
            ArchivedVehiclesScreen(onBack = navController::popBackStack)
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack = navController::popBackStack,
                onOpenArchived = { navController.navigate(Destinations.ARCHIVED) },
            )
        }
    }
}

private fun NavBackStackEntry.vehicleId(): Long =
    checkNotNull(arguments?.getLong(Destinations.VEHICLE_ID_ARG)) {
        "Missing ${Destinations.VEHICLE_ID_ARG} argument"
    }
