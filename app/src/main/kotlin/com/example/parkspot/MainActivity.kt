package com.example.parkspot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.parkspot.ui.ParkSpotApp
import com.example.parkspot.ui.ParkingViewModel
import com.example.parkspot.ui.theme.ParkSpotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ParkingViewModel by viewModels()
    private var latestIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latestIntent = intent

        enableEdgeToEdge()
        setContent {
            ParkSpotTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1216)
                ) {
                    ParkSpotApp(
                        viewModel = viewModel,
                        initialIntent = latestIntent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent = intent
    }
}
