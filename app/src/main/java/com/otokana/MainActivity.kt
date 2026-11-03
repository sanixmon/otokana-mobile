package com.otokana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.otokana.ui.OtoKanaApp
import com.otokana.ui.OtoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The desk is always dark: light icons on the bars.
        enableEdgeToEdge(SystemBarStyle.dark(0x00000000), SystemBarStyle.dark(0x00000000))
        super.onCreate(savedInstanceState)
        setContent { OtoTheme { OtoKanaApp() } }
    }
}
