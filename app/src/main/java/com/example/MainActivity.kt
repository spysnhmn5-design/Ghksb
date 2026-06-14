package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Instantiate MainViewModel with a standard Factory matching our constructor guidelines
    val viewModel = ViewModelProvider(
      this, 
      MainViewModelFactory(application)
    )[MainViewModel::class.java]

    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}
