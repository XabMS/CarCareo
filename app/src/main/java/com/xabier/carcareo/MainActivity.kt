package com.xabier.carcareo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.xabier.carcareo.notifications.Notifications
import com.xabier.carcareo.ui.navigation.CarCareoNavGraph
import com.xabier.carcareo.ui.theme.CarCareoTheme

/**
 * Single-activity app. Compose owns everything from here; navigation is handled
 * by [CarCareoNavGraph].
 */
class MainActivity : ComponentActivity() {

    /**
     * Vehicle to jump to, set when the activity is opened from a maintenance
     * notification. Compose state, so both a cold start and a tap while the app
     * is already running ([onNewIntent]) reach the nav graph the same way.
     */
    private var openVehicleId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after a rotation or process restore the saved
        // back stack already reflects where the notification took the user.
        if (savedInstanceState == null) {
            openVehicleId = Notifications.vehicleIdFrom(intent)
        }
        setContent {
            CarCareoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CarCareoNavGraph(
                        openVehicleId = openVehicleId,
                        onVehicleOpened = { openVehicleId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Notifications.vehicleIdFrom(intent)?.let { openVehicleId = it }
    }
}
