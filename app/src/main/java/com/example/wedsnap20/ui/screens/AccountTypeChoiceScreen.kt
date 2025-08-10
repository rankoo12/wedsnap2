package com.example.wedsnap20.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wedsnap20.services.AuthService
import com.example.wedsnap20.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AccountTypeChoiceScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val authService = AuthService(context, viewModel)
    val user = FirebaseAuth.getInstance().currentUser
    val nameOverride = viewModel.nameOverride.collectAsState().value

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "What Would You Like To Do?",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    user?.let { u ->
                        val resolvedName = when {
                            !nameOverride.isNullOrBlank() -> nameOverride.trim()
                            !u.displayName.isNullOrBlank() -> u.displayName!!.trim()
                            else -> "Anonymous Host"
                        }
                        authService.createOrUpdateUser(
                            user = u,
                            name = resolvedName,
                            type = "host",
                            onSuccess = {
                                navController.navigate("host_panel") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onFailure = { error ->
                                Log.e("AccountTypeChoice", "Failed to save host user", error)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(text = "I'm the Host")
            }

            Button(
                onClick = {
                    user?.let { u ->
                        val resolvedName = when {
                            !nameOverride.isNullOrBlank() -> nameOverride.trim()
                            !u.displayName.isNullOrBlank() -> u.displayName!!.trim()
                            else -> "Anonymous Guest"
                        }
                        authService.createOrUpdateUser(
                            user = u,
                            name = resolvedName,
                            type = "guest",
                            onSuccess = {
                                navController.navigate("scanner") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onFailure = { error ->
                                Log.e("AccountTypeChoice", "Failed to save guest user", error)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(text = "📷 Scan QR Code to Join Event")
            }
        }
    }
}
