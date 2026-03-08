package com.stocktracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.ambient.AmbientLifecycleObserver
import com.stocktracker.wear.ui.AmbientAware
import com.stocktracker.wear.ui.StockTrackerNavHost
import com.stocktracker.wear.ui.theme.StockTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isAmbient by mutableStateOf(false)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
        }

        override fun onExitAmbient() {
            isAmbient = false
        }

        override fun onUpdateAmbient() {
            // Called periodically in ambient mode — compose will recompose automatically
        }
    }

    private lateinit var ambientObserver: AmbientLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ambientObserver = AmbientLifecycleObserver(this, ambientCallback)
        lifecycle.addObserver(ambientObserver)

        enableEdgeToEdge()
        setContent {
            StockTrackerTheme {
                AmbientAware(isAmbient = isAmbient) {
                    StockTrackerNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND)
@Composable
private fun MainPreview() {
    StockTrackerTheme {
        StockTrackerNavHost(modifier = Modifier.fillMaxSize())
    }
}

