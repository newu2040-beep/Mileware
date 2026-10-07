package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MilesAreTheme
import com.example.ui.viewmodel.ConfessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val auth = Firebase.auth
            var currentUser by remember { mutableStateOf(auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    currentUser = firebaseAuth.currentUser
                }
                auth.addAuthStateListener(listener)
                onDispose {
                    auth.removeAuthStateListener(listener)
                }
            }

            // We can resolve ViewModel inside the authenticated state
            if (currentUser != null) {
                val confessionViewModel: ConfessionViewModel = viewModel()
                val isDarkMode by confessionViewModel.isDarkMode.collectAsState()

                MilesAreTheme(darkTheme = isDarkMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            onSignOutSuccess = {
                                currentUser = null
                            },
                            viewModel = confessionViewModel
                        )
                    }
                }
            } else {
                MilesAreTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                            }
                        )
                    }
                }
            }
        }
    }
}
