package com.calmspace

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.loginScreen
import com.calmspace.ui.authentication.signupScreen
import com.calmspace.ui.authentication.welcomeScreen
import com.calmspace.ui.player.mediaPlayerScreen
import com.calmspace.ui.theme.CalmSpaceTheme

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private val isLoopPlayingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CalmSpaceTheme {
                MainNavGraph()
            }
        }
    }

    @Composable
    private fun MainNavGraph() {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "media_player"
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

            composable("media_player") {
                mediaPlayerScreen(
                    isPlaying = isLoopPlayingState.value,
                    onTogglePlayback = { toggleLoopPlayback() }
                )
            }
        }
    }

    private fun createLoopingPlayerIfNeeded() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.rain_from_indoors_perfect_loop).apply {
                isLooping = true
            }
        }
    }

    private fun startLoopPlayback() {
        createLoopingPlayerIfNeeded()
        mediaPlayer?.start()
        isLoopPlayingState.value = true
    }

    private fun stopLoopPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                player.seekTo(0)
            }
        }
        isLoopPlayingState.value = false
    }

    private fun toggleLoopPlayback() {
        if (isLoopPlayingState.value) {
            stopLoopPlayback()
        } else {
            startLoopPlayback()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
