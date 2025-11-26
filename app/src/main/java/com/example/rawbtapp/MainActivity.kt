package com.example.rawbtapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.rawbtapp.ui.PrinterScreen
import com.example.rawbtapp.ui.PrinterViewModel
import com.example.rawbtapp.ui.theme.RawBTAppTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: PrinterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RawBTAppTheme {
                PrinterScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}