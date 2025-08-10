package com.example.wedsnap20.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
inline fun <reified T : ViewModel> navGraphViewModel(
    navController: NavController,
    graphRoute: String = "root"
): T {
    // ✅ Key remember with the currentBackStackEntry to avoid the warning
    val owner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(graphRoute)
    }
    return viewModel(owner)
}
