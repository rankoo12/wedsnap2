package com.example.wedsnap20.ui.screens.host

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.navDeepLink
import com.example.wedsnap20.ui.ScannerScreen
import com.example.wedsnap20.ui.screens.AccountTypeChoiceScreen
import com.example.wedsnap20.ui.screens.WelcomeScreen
import com.example.wedsnap20.ui.screens.guest.GuestAlbumScreen
import com.example.wedsnap20.ui.screens.guest.GuestDecisionScreen
import com.example.wedsnap20.ui.screens.guest.GuestGalleryScreen
import com.example.wedsnap20.viewmodel.AuthViewModel
import com.example.wedsnap20.ui.screens.route.JoinRouterScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: AuthViewModel
) {
    NavHost(navController = navController, startDestination = "welcome", route = "root") {

        // 🔗 Deep-link router: catches /join/{eventId} and forwards.
        composable(
            route = "join_router/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://wedsnapv2.web.app/join/{eventId}" },
                navDeepLink { uriPattern = "wedsnap://join/{eventId}" }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            JoinRouterScreen(eventId = eventId, navController = navController)
        }

        composable("welcome") {
            WelcomeScreen(navController = navController, viewModel = viewModel)
        }

        composable("scanner") {
            ScannerScreen(navController = navController)
        }

        composable("host_panel") {
            HostPanelScreen(viewModel = viewModel, navController = navController)
        }

        // ✅ Single canonical album route (no deep links here)
        composable(
            route = "guest_album/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            GuestAlbumScreen(eventId = eventId, navController = navController, viewModel = viewModel)
        }

        composable(
            route = "guest_gallery/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            GuestGalleryScreen(eventId = eventId, navController = navController, viewModel = viewModel)
        }

        composable("guest_decision") {
            GuestDecisionScreen(navController = navController)
        }

        composable("account_type_choice") {
            AccountTypeChoiceScreen(navController = navController, viewModel = viewModel)
        }

        composable("create_event") {
            // CreateEventScreen(navController = navController)
        }
    }
}
