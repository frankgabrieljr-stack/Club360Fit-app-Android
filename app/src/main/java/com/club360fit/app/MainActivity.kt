package com.club360fit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.club360fit.app.ui.theme.Club360FitTheme
import com.club360fit.app.ui.navigation.Club360FitNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Club360FitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Club360FitNavHost()
                }
            }
        }
    }
}
