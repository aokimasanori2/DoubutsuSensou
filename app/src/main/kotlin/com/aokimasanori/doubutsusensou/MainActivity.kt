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
        if (android.os.Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)
        enableEdgeToEdge()
        setContent {
            DoubutsuSensouTheme { DoubutsuSensouApp() }
        }
    }
}
