package com.example.wedsnap20.ui.screens.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.example.wedsnap20.ui.util.navGraphViewModel
import com.example.wedsnap20.viewmodel.DeepLinkViewModel

@Composable
fun JoinRouterScreen(eventId: String, navController: NavController) {
    val vm: DeepLinkViewModel = navGraphViewModel(navController)

    LaunchedEffect(eventId) {
        vm.setPending(eventId)
        navController.navigate("welcome") {
            popUpTo("welcome") { inclusive = true }
        }
    }
}
