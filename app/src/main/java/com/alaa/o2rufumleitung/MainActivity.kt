package com.alaa.o2rufumleitung

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alaa.o2rufumleitung.ui.ForwardingScreen
import com.alaa.o2rufumleitung.ui.theme.O2RufumleitungTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@Composable
private fun App() {
    O2RufumleitungTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ForwardingScreen()
        }
    }
}
