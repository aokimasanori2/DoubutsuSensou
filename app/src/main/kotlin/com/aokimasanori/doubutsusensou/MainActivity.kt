package com.aokimasanori.doubutsusensou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aokimasanori.doubutsusensou.ui.DoubutsuSensouApp
import com.aokimasanori.doubutsusensou.ui.theme.DoubutsuSensouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoubutsuSensouTheme { DoubutsuSensouApp() }
        }
    }
}
