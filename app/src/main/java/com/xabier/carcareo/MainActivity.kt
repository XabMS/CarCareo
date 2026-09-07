package com.xabier.carcareo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.xabier.carcareo.ui.navigation.CarCareoNavGraph
import com.xabier.carcareo.ui.theme.CarCareoTheme

/**
 * Single-activity app. Compose owns everything from here; navigation is handled
 * by [CarCareoNavGraph].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CarCareoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CarCareoNavGraph()
                }
            }
        }
    }
}
