package com.example.wedsnap20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.wedsnap20.ui.screens.host.AppNavHost
import com.example.wedsnap20.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val viewModel: AuthViewModel = viewModel()
                val navController = rememberNavController()
                AppNavHost(navController = navController, viewModel = viewModel)
            }
        }
    }
}
