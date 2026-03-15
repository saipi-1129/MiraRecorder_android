package com.synex.mirarecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.synex.mirarecorder.ui.navigation.AppNavigation
import com.synex.mirarecorder.ui.theme.MiraRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiraRecorderTheme {
                AppNavigation()
            }
        }
    }
}
