package com.bplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bplayer.ui.BPlayerNavHost
import com.bplayer.ui.theme.BPlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BPlayerTheme {
                BPlayerNavHost()
            }
        }
    }
}
