package com.calmspace

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.loginScreen
import com.calmspace.ui.authentication.signupScreen
import com.calmspace.ui.authentication.welcomeScreen
import com.calmspace.ui.player.mediaPlayerScreen
import com.calmspace.ui.theme.CalmSpaceTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
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
        if (exoPlayer == null) {
            val rawUri =
                "android.resource://$packageName/${R.raw.rain_from_indoors_perfect_loop}".toUri()
            val mediaItem = MediaItem.fromUri(rawUri)

            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
        }
    }

    private fun startLoopPlayback() {
        createLoopingPlayerIfNeeded()
        exoPlayer?.play()
        isLoopPlayingState.value = true
    }

    private fun stopLoopPlayback() {
        exoPlayer?.pause()
        exoPlayer?.seekToDefaultPosition()
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
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
