package com.calmspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.LoginScreen
import com.calmspace.ui.authentication.SignupScreen
import com.calmspace.ui.authentication.WelcomeScreen
import com.calmspace.ui.onboarding.QuestionnaireScreen
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
                        WelcomeScreen(
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
                        LoginScreen(
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
                        SignupScreen(
                            onBackToLogin = {
                                navController.popBackStack()
                            },
                            onSignup = { username, email, password ->
                                // TODO:
                                // - Create account
                                // - Handle success / error

                                // IMPORTANT:
                                // Signup now goes to onboarding questionnaire
                                navController.navigate("questionnaire")
                            }
                        )
                    }

                    // ───────── Questionnaire Screen ─────────
                    composable("questionnaire") {
                        QuestionnaireScreen(
                            onFinish = {
                                // TODO:
                                // - Save onboarding completion
                                // - Navigate to dashboard
                                navController.navigate("home") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }

                    // ───────── Home Screen (placeholder) ─────────
                    composable("home") {
                        // TODO:
                        // Replace with real dashboard / bottom navigation
                    }
                }
            }
        }
    }
}
