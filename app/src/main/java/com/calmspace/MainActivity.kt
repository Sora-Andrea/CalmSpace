package com.calmspace

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.LoginScreen
import com.calmspace.ui.authentication.SignupScreen
import com.calmspace.ui.authentication.WelcomeScreen
import com.calmspace.ui.components.BottomNavigationBar
import com.calmspace.ui.onboarding.QuestionnaireScreen
import com.calmspace.ui.screens.HomeScreen
import com.calmspace.ui.screens.MonitorScreen
import com.calmspace.ui.screens.ProfileScreen
import com.calmspace.ui.screens.SettingsScreen
import com.calmspace.ui.authentication.loginScreen
import com.calmspace.ui.authentication.signupScreen
import com.calmspace.ui.authentication.welcomeScreen
import com.calmspace.ui.player.mediaPlayerScreen
import com.calmspace.ui.theme.CalmSpaceTheme
import androidx.core.net.toUri

// ─────────────────────────────────────────────
// Route Constants
// TODO: Move to a dedicated Routes.kt file
// ─────────────────────────────────────────────
object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val QUESTIONNAIRE = "questionnaire"
    const val HOME = "home"
    const val MONITOR = "monitor"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

// ─────────────────────────────────────────────
// Screens where the bottom nav bar should show
// ─────────────────────────────────────────────
val bottomNavRoutes = setOf(
    Routes.HOME,
    Routes.MONITOR,
    Routes.PROFILE,
    Routes.SETTINGS
)

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    private val isLoopPlayingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CalmSpaceTheme {

                val navController = rememberNavController()
                val currentRoute = navController
                    .currentBackStackEntryAsState().value?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute in bottomNavRoutes) {
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = Routes.WELCOME,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // ───────── Welcome Screen ─────────
                        composable(Routes.WELCOME) {
                            WelcomeScreen(
                                onLoginClick = {
                                    navController.navigate(Routes.LOGIN)
                                },
                                onSignupClick = {
                                    navController.navigate(Routes.SIGNUP)
                                }
                            )
                        }

                        // ───────── Login Screen ─────────
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onBack = {
                                    navController.popBackStack()
                                },
                                onLogin = { email, password ->
                                    // TODO:
                                    // - Authenticate user
                                    // - Handle success / error
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ───────── Signup Screen ─────────
                        composable(Routes.SIGNUP) {
                            SignupScreen(
                                onBackToLogin = {
                                    navController.popBackStack()
                                },
                                onSignup = { username, email, password ->
                                    // TODO:
                                    // - Create account
                                    // - Handle success / error
                                    navController.navigate(Routes.QUESTIONNAIRE)
                                }
                            )
                        }

                        // ───────── Questionnaire Screen ─────────
                        composable(Routes.QUESTIONNAIRE) {
                            QuestionnaireScreen(
                                onFinish = {
                                    // TODO:
                                    // - Save onboarding completion
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ───────── Home Screen ─────────
                        composable(Routes.HOME) {
                            HomeScreen(
                                onStartSession = {
                                    navController.navigate(Routes.MONITOR)
                                },
                                onSeeAllSessions = {
                                    // TODO: Navigate to session history screen
                                }
                            )
                        }

                        // ───────── Monitor Screen ─────────
                        composable(Routes.MONITOR) {
                            MonitorScreen(
                                onStopRecording = {
                                    // TODO: Stop foreground service, finalize session
                                }
                            )
                        }

                        // ───────── Profile Screen ─────────
                        composable(Routes.PROFILE) {
                            ProfileScreen()
                        }

                        // ───────── Settings Screen ─────────
                        composable(Routes.SETTINGS) {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
