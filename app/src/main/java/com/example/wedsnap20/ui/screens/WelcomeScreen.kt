package com.example.wedsnap20.ui.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.wedsnap20.services.AuthService
import com.example.wedsnap20.ui.util.navGraphViewModel
import com.example.wedsnap20.viewmodel.AuthViewModel
import com.example.wedsnap20.viewmodel.DeepLinkViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun WelcomeScreen(navController: NavController, viewModel: AuthViewModel) {
    val vm: DeepLinkViewModel = navGraphViewModel(navController)
    val pendingId = vm.pendingEventId.collectAsState().value
    Log.d("WelcomeScreen", "pendingId: $pendingId")

    val context = LocalContext.current
    val authService = remember { AuthService(context, viewModel) }

    var nameInput by remember { mutableStateOf(TextFieldValue("")) }
    var guestLoginInProgress by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authService.handleSignInResult(result.data) { resultUser ->
            resultUser.onSuccess { user ->
                handlePostGoogleLogin(
                    user = user,
                    navController = navController,
                    context = context,
                    viewModel = viewModel,
                    overrideName = nameInput.text
                )
            }.onFailure {
                Log.e("WelcomeScreen", "Google Sign-In Failed", it)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to WedSnap!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Your Name (Optional)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (currentUser?.isAnonymous == true) {
                    FirebaseAuth.getInstance().signOut()
                }
                val signInIntent = authService.getSignInIntent()
                launcher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!guestLoginInProgress) {
                    guestLoginInProgress = true
                    val name = if (nameInput.text.isNotBlank()) {
                        nameInput.text.trim()
                    } else {
                        generateRandomGuestName()
                    }

                    if (currentUser != null && !currentUser.isAnonymous) {
                        FirebaseAuth.getInstance().signOut()
                    }

                    authService.signInAnonymously(
                        onSuccess = { user ->
                            authService.createOrUpdateUser(
                                user = user,
                                name = name,
                                type = "guest",
                                onSuccess = {
                                    guestLoginInProgress = false
                                    navController.navigate("scanner") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onFailure = { error ->
                                    guestLoginInProgress = false
                                    Log.e("WelcomeScreen", "Failed to save guest to Firestore", error)
                                }
                            )
                        },
                        onFailure = { error ->
                            guestLoginInProgress = false
                            Log.e("WelcomeScreen", "Anonymous login failed: ${error.message}", error)
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue as Guest")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun handlePostGoogleLogin(
    user: FirebaseUser,
    navController: NavController,
    context: Context,
    viewModel: AuthViewModel,
    overrideName: String? = null
) {
    val authService = AuthService(context, viewModel)
    val uid = user.uid

    viewModel.setUser(user)
    viewModel.setNameOverride(overrideName)

    // ✅ Ensure a name is stored (typed -> Google -> fallback) in Auth + Firestore (merge)
    authService.ensureGoogleProfileName(overrideName) {
        // Continue your decision logic after name persistence
        authService.getUserType(uid) { type ->
            when (type) {
                "host" -> {
                    navController.navigate("host_panel") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
                "guest" -> {
                    navController.navigate("guest_decision") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
                else -> {
                    navController.navigate("account_type_choice") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            }
        }
    }
}

fun generateRandomGuestName(): String {
    val adjectives = listOf("Happy", "Witty", "Cool", "Silly", "Brave", "Sunny")
    val animals = listOf("Penguin", "Tiger", "Koala", "Panda", "Otter", "Giraffe")
    val adj = adjectives.random()
    val animal = animals.random()
    val number = (100..999).random()
    return "$adj$animal$number"
}
