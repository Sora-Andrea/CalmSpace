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
                    composable("welcome") {
                        welcomeScreen(
                            onLoginClick = { navController.navigate("login") },
                            onSignupClick = { navController.navigate("signup") }
                        )
                    }

                    composable("login") {
                        loginScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("signup") {
                        signupScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
