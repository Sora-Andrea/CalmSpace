package com.calmspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.calmspace.ui.authentication.loginScreen
import com.calmspace.ui.authentication.signupScreen
import com.calmspace.ui.authentication.welcomeScreen
import com.calmspace.ui.theme.CalmSpaceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CalmSpaceTheme {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "welcome"
                ) {

                    // ───────── Welcome Screen ─────────
                    composable("welcome") {
                        welcomeScreen(
                            onLoginClick = {
                                navController.navigate("login")
                            },
                            onSignupClick = {
                                navController.navigate("signup")
                            }
                        )
                    }

                    // ───────── Login Screen ─────────
                    composable("login") {
                        loginScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            onLogin = { email, password ->
                                // TODO:
                                // - Authenticate user
                                // - Handle success / error
                                navController.navigate("home")
                            }
                        )
                    }

                    // ───────── Signup Screen ─────────
                    composable("signup") {
                        signupScreen(
                            onBackToLogin = {
                                navController.popBackStack()
                            },
                                onSignup = { username, email, password ->
                                // TODO:
                                // - Create account
                                // - Handle success / error
                                navController.navigate("home")
                            }
                        )
                    }

                    // ───────── Home Screen (placeholder) ─────────
                    //composable("home") {
                        // TODO: Replace with real home / bottom navigation
                    //}
                }
            }
        }
    }
}
