package com.calmspace

import android.os.Bundle
import android.os.SystemClock
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calmspace.ui.authentication.LoginScreen
import com.calmspace.ui.authentication.SignupScreen
import com.calmspace.ui.authentication.WelcomeScreen
import com.calmspace.ui.components.BottomNavigationBar
import com.calmspace.ui.onboarding.QuestionnaireScreen
import com.calmspace.ui.ml.YamnetPocScreen
import com.calmspace.ui.screens.HomeScreen
import com.calmspace.ui.screens.MonitorScreen
import com.calmspace.ui.screens.PrivacyPolicyScreen
import com.calmspace.ui.screens.ProfileScreen
import com.calmspace.ui.screens.SettingsScreen
import com.calmspace.ui.player.PlaybackTrack
import com.calmspace.ui.player.PlaybackTrackOption
import com.calmspace.ui.player.PrecomputedPlaybackTracks
import com.calmspace.ui.player.mediaPlayerScreen
import com.calmspace.ui.theme.AppTheme
import com.calmspace.ui.theme.CalmSpaceTheme
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.calmspace.service.AudioTimingConfig.EXO_PLAYBACK_FADE_IN_DURATION_MS
import com.calmspace.service.AudioTimingConfig.EXO_PLAYBACK_FADE_IN_STEP_MS
import com.calmspace.service.AudioTimingConfig.MASKING_AUTOMATION_DUCKING_FACTOR
import com.calmspace.service.AudioTimingConfig.MASKING_AUTOMATION_REFERENCE_DB_MAX
import com.calmspace.service.AudioTimingConfig.MASKING_AUTOMATION_REFERENCE_DB_MIN
import com.calmspace.ui.player.UserTracksManager
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

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
    const val PRIVACY_POLICY = "privacy_policy"
    const val MEDIA_PLAYER = "media_player"
    const val YAMNET_POC = "yamnet_poc"
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
    companion object {
        private const val VISUALIZER_BAR_COUNT = 32
        private const val MIC_SAMPLE_RATE_HZ = 44100
        private const val MIC_BUFFER_SIZE = 2048
        private const val VISUALIZER_DB_FLOOR = -80f
        private const val VISUALIZER_DB_CEILING = 0f
        private const val PLAYBACK_VISUALIZER_UPDATE_MS = 33L
    }

    private val selectedThemeState = mutableStateOf(AppTheme.DEEP_WATER)

    private lateinit var userTracksManager: UserTracksManager
    private var exoPlayer: ExoPlayer? = null
    private var visualizer: android.media.audiofx.Visualizer? = null
    private val isLoopPlayingState = mutableStateOf(false)
    private val availablePlaybackTracks = PrecomputedPlaybackTracks.tracks.toMutableList()
    private val playbackTrackOptionsState = mutableStateOf(
        availablePlaybackTracks.map { PlaybackTrackOption(it.id, it.title) }
    )
    private val selectedTrackIdState = mutableStateOf(availablePlaybackTracks.firstOrNull()?.id.orEmpty())
    private val playbackLevelsState = mutableStateListOf<Float>().apply {
        repeat(VISUALIZER_BAR_COUNT) { add(0f) }
    }
    private val playbackDbfsState = mutableStateOf(VISUALIZER_DB_FLOOR)
    private val micLevelsState = mutableStateListOf<Float>().apply {
        repeat(VISUALIZER_BAR_COUNT) { add(0f) }
    }
    private val micDbfsState = mutableStateOf(VISUALIZER_DB_FLOOR)
    private val isMicRunningState = mutableStateOf(false)
    private val hasMicPermissionState = mutableStateOf(false)

    //private var playbackSyncJob: Job? = null
    private var playbackFadeInJob: Job? = null
    private var isExoFadeInInProgress = false
    private var pendingExoPlayerVolume = 1f
    private var exoPlaybackBaselineVolume = 1f
    private var loadedTrackId: String? = null
    private var micAudioRecord: AudioRecord? = null
    private var micCaptureJob: Job? = null
    private var startMicVisualizerAfterPermissionGrant = false

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermissionState.value = granted
        if (granted && startMicVisualizerAfterPermissionGrant) {
            try {
                startMicrophoneVisualizer()
            } catch (_: SecurityException) {
                hasMicPermissionState.value = false
                isMicRunningState.value = false
            }
        }
        startMicVisualizerAfterPermissionGrant = false
    }

    private val audioFilePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleImportedAudioUri(it) }
    }
    private fun handleImportedAudioUri(uri: Uri) {
        if (!userTracksManager.addUri(uri)) {
            // Show a toast error (use a Snackbar or Toast)
            return
        }
        val newTrack = userTracksManager.createTrackFromUri(uri) ?: return
        addUserTrackToRuntimeList(newTrack, uri)
    }

    @OptIn(UnstableApi::class)
    private fun addUserTrackToRuntimeList(track: PlaybackTrack, uri: Uri) {
        // Optional: verify MIME type
        val mime = contentResolver.getType(uri)
        if (mime == null || !mime.startsWith("audio/")) {
            // Show error toast
            return
        }
        availablePlaybackTracks.add(track)
        playbackTrackOptionsState.value = availablePlaybackTracks.map { PlaybackTrackOption(it.id, it.title) }
        selectPlaybackTrack(track.id)
        // Test if the file is readable
        try {
            contentResolver.openInputStream(uri)?.close()
            Log.d("ImportTest", "File is readable")
        } catch (e: Exception) {
            Log.e("ImportTest", "File not readable: ${e.message}")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("calmspace_prefs", MODE_PRIVATE)
        selectedThemeState.value = runCatching {
            AppTheme.valueOf(prefs.getString("app_theme", AppTheme.DEEP_WATER.name) ?: "")
        }.getOrDefault(AppTheme.DEEP_WATER)
        val startDestination = Routes.MONITOR
        // Future code for auto logging in users already logged in
        //val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        //    Routes.HOME  // User already logged in, go directly to home
        //} else {
        //    Routes.WELCOME
        //}
        hasMicPermissionState.value = hasRecordAudioPermission()
        userTracksManager = UserTracksManager(this)
        val savedUris = userTracksManager.getSavedUris()
        savedUris.forEach { uriString ->
            val uri = Uri.parse(uriString)
            userTracksManager.createTrackFromUri(uri)?.let { track ->
                availablePlaybackTracks.add(track)
            }
        }
        playbackTrackOptionsState.value = availablePlaybackTracks.map { PlaybackTrackOption(it.id, it.title) }
        refreshPlaybackLevelsFromSelectedTrack(0)

        setContent {
            CalmSpaceTheme(theme = selectedThemeState.value) {

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
                        startDestination = startDestination,
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
                                onSignupInstead = { navController.navigate(Routes.SIGNUP) },
                                onLoginSuccess = {
                                    // Clear back stack and go to home (or questionnaire if needed)
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ───────── Signup Screen ─────────
                        composable(Routes.SIGNUP) {
                            SignupScreen(
                                onBackToLogin = { navController.popBackStack() },
                                onSignupSuccess = {
                                    // After successful signup, go to onboarding questionnaire
                                    navController.navigate(Routes.QUESTIONNAIRE) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
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
                            val selectedMonitorTrackIdState = remember { mutableStateOf("white_noise") }
                            val isMonitoringSessionActiveState = remember { mutableStateOf(false) }
                            val exoTrackIds = remember(playbackTrackOptionsState.value) {
                                playbackTrackOptionsState.value.map { it.id }.toSet()
                            }
                            val isServiceTrack = remember(exoTrackIds) {
                                { trackId: String -> !exoTrackIds.contains(trackId) }
                            }
                            val generatedNoiseTrackOptions = remember {
                                listOf(
                                    PlaybackTrackOption(id = "white_noise",  title = "Bright Static"),
                                    PlaybackTrackOption(id = "pink_noise",   title = "Balanced Rain"),
                                    PlaybackTrackOption(id = "brown_noise",  title = "Deep Rumble"),
                                    PlaybackTrackOption(id = "blue_noise",   title = "High Hiss"),
                                    PlaybackTrackOption(id = "grey_noise",   title = "Neutral Static"),
                                )
                            }
                            val monitorTrackOptions = remember(playbackTrackOptionsState.value, generatedNoiseTrackOptions) {
                                playbackTrackOptionsState.value + generatedNoiseTrackOptions
                            }

                            LaunchedEffect(
                                selectedMonitorTrackIdState.value,
                                exoTrackIds,
                                isMonitoringSessionActiveState.value
                            ) {
                                val selectedMonitorTrackId = selectedMonitorTrackIdState.value
                                val isKnownTrack = monitorTrackOptions.any { it.id == selectedMonitorTrackId }
                                if (!isKnownTrack) {
                                    selectedMonitorTrackIdState.value = "white_noise"
                                }

                                val resolvedMonitorTrackId = selectedMonitorTrackIdState.value
                                if (!isServiceTrack(resolvedMonitorTrackId) &&
                                    selectedTrackIdState.value != resolvedMonitorTrackId
                                ) {
                                    selectPlaybackTrack(resolvedMonitorTrackId)
                                }

                                if (
                                    isMonitoringSessionActiveState.value &&
                                    !isServiceTrack(resolvedMonitorTrackId) &&
                                    !isLoopPlayingState.value
                                ) {
                                    startLoopPlayback()
                                }
                            }

                            MonitorScreen(
                                micLevels = micLevelsState,
                                playbackLevels = playbackLevelsState,
                                trackOptions = monitorTrackOptions,
                                selectedTrackId = selectedMonitorTrackIdState.value,
                                isServiceTrack = isServiceTrack,
                                onMonitoringSessionStateChanged = { isMonitoring ->
                                    isMonitoringSessionActiveState.value = isMonitoring
                                },
                                onTrackSelected = { trackId ->
                                    selectedMonitorTrackIdState.value = trackId
                                    val wasLoopPlaying = isLoopPlayingState.value
                                    val isGeneratedNoise = trackId in setOf("white_noise", "pink_noise", "brown_noise", "blue_noise", "grey_noise")
                                    if (isGeneratedNoise) {
                                        stopLoopPlayback()
                                    } else {
                                        selectedTrackIdState.value = trackId
                                        selectPlaybackTrack(trackId)

                                        if (isMonitoringSessionActiveState.value && wasLoopPlaying) {
                                            startLoopPlayback()
                                        }
                                    }
                                },
                                isTrackPlaybackPlaying = isLoopPlayingState.value,
                                onToggleTrackPlayback = {
                                    if (isLoopPlayingState.value) {
                                        stopLoopPlayback()
                                    } else {
                                        val selectedTrackId = selectedMonitorTrackIdState.value
                                        selectedTrackIdState.value = selectedTrackId
                                        selectPlaybackTrack(selectedTrackId)
                                        startLoopPlayback()
                                    }
                                },
                                onTrackVolumeChange = { volume ->
                                    if (!isServiceTrack(selectedMonitorTrackIdState.value)) {
                                        exoPlaybackBaselineVolume = volume.coerceIn(0f, 1f)
                                        if (!isExoFadeInInProgress) {
                                            exoPlayer?.volume = volume
                                        } else {
                                            pendingExoPlayerVolume = volume
                                        }
                                    }
                                },
                                onMaskingAutomationVolume = { volume ->
                                    // Option A control stream from monitor service
                                    // only drives ExoPlayer when the selected ambient
                                    // source is microphone/asset playback.
                                    if (!isServiceTrack(selectedMonitorTrackIdState.value)) {
                                        val dampened = dampenExoTargetWithPlaybackLoudness(volume)
                                        if (isExoFadeInInProgress) {
                                            pendingExoPlayerVolume = dampened
                                        } else {
                                            exoPlayer?.volume = dampened
                                        }
                                    }
                                },
                                onStopRecording = {
                                    stopLoopPlayback()
                                    resetLevelHistory(micLevelsState)
                                    micDbfsState.value = VISUALIZER_DB_FLOOR
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.MONITOR) { inclusive = true }
                                    }
                                },
                                onImportAudio = { audioFilePickerLauncher.launch(arrayOf("audio/*")) }
                            )
                        }

                        // ───────── Profile Screen ─────────
                        composable(Routes.PROFILE) {
                            ProfileScreen()
                        }

                        // ───────── Settings Screen ─────────
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onNavigateToPrivacyPolicy = {
                                    navController.navigate(Routes.PRIVACY_POLICY)
                                },
                                onNavigateToMediaPlayer = {
                                    navController.navigate(Routes.MEDIA_PLAYER)
                                },
                                currentTheme = selectedThemeState.value,
                                onThemeSelected = { theme ->
                                    selectedThemeState.value = theme
                                    getSharedPreferences("calmspace_prefs", MODE_PRIVATE)
                                        .edit().putString("app_theme", theme.name).apply()
                                }
                            )
                        }

                        // ───────── Privacy Policy Screen ─────────
                        composable(Routes.PRIVACY_POLICY) {
                            PrivacyPolicyScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // ───────── Media Player Debug Screen ─────────
                        composable(Routes.MEDIA_PLAYER) {
                            mediaPlayerScreen(
                                isPlaying = isLoopPlayingState.value,
                                trackOptions = playbackTrackOptionsState.value,   // use the state
                                selectedTrackId = selectedTrackIdState.value,
                                playerLevels = playbackLevelsState,
                                playbackDbfs = playbackDbfsState.value,
                                isMicRunning = isMicRunningState.value,
                                micLevels = micLevelsState,
                                micDbfs = micDbfsState.value,
                                hasMicPermission = hasMicPermissionState.value,
                                onTrackSelected = { trackId -> selectPlaybackTrack(trackId) },
                                onOpenYamnetPoc = { navController.navigate(Routes.YAMNET_POC) },
                                onTogglePlayback = { toggleLoopPlayback() },
                                onToggleMicrophone = { toggleMicrophoneVisualizer() },
                                onRequestMicrophonePermission = { requestMicrophonePermissionForMicVisualizer() },
                                onImportAudio = { audioFilePickerLauncher.launch(arrayOf("audio/*")) }
                            )
                        }

                        composable(Routes.YAMNET_POC) {
                            YamnetPocScreen(
                                hasMicPermission = hasMicPermissionState.value,
                                onRequestMicrophonePermission = { requestMicrophonePermissionForYamnet() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- ExoPlayer amplitude source (precomputed waveform) ---
    private fun createLoopingPlayerIfNeeded() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                repeatMode = Player.REPEAT_MODE_ONE

                // Add error listener
                addListener(object : Player.Listener {
                    @OptIn(UnstableApi::class)
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("ExoPlayer", "Error: ${error.message}", error)
                    }

                    @OptIn(UnstableApi::class)
                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("ExoPlayer", "State changed: $state")
                        when (state) {
                            Player.STATE_READY -> Log.d("ExoPlayer", "Player ready, audio should play")
                            Player.STATE_ENDED -> Log.d("ExoPlayer", "Playback ended")
                            Player.STATE_BUFFERING -> Log.d("ExoPlayer", "Buffering...")
                            Player.STATE_IDLE -> Log.d("ExoPlayer", "Idle")
                        }
                    }
                })
            }
        }
    }

    private fun selectedPlaybackTrack(): PlaybackTrack? {
        return availablePlaybackTracks.firstOrNull { it.id == selectedTrackIdState.value }
    }
    private fun selectPlaybackTrack(trackId: String) {
        if (trackId == selectedTrackIdState.value) return
        val wasPlaying = isLoopPlayingState.value
        selectedTrackIdState.value = trackId
        loadedTrackId = null
        createLoopingPlayerIfNeeded()   // ensure player exists
        loadSelectedTrackIntoPlayer()
        if (wasPlaying) {
            val currentTrackVolume = exoPlayer?.volume ?: 1f
            startLoopPlaybackWithFadeIn(currentTrackVolume)
        }
    }

    private fun loadSelectedTrackIntoPlayer() {
        val track = selectedPlaybackTrack() ?: return
        createLoopingPlayerIfNeeded()
        if (loadedTrackId == track.id) return

        val mediaItem = when {
            track.uriString != null -> MediaItem.fromUri(Uri.parse(track.uriString))
            track.rawResId != 0 -> MediaItem.fromUri("android.resource://$packageName/${track.rawResId}")
            else -> return
        }

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            seekToDefaultPosition()
        }
        loadedTrackId = track.id
    }

    private fun startLoopPlayback() {
        loadSelectedTrackIntoPlayer()
        val trackVolume = exoPlayer?.volume ?: 1f
        startLoopPlaybackWithFadeIn(trackVolume)
        isLoopPlayingState.value = true
        startPlaybackVisualizer()
    }

    private fun stopLoopPlayback() {
        playbackFadeInJob?.cancel()
        playbackFadeInJob = null
        isExoFadeInInProgress = false
        exoPlayer?.pause()
        exoPlayer?.seekToDefaultPosition()
        isLoopPlayingState.value = false
        stopPlaybackVisualizer()
        resetLevelHistory(playbackLevelsState)
        playbackDbfsState.value = VISUALIZER_DB_FLOOR
    }

    private fun toggleLoopPlayback() {
        if (isLoopPlayingState.value) {
            stopLoopPlayback()
        } else {
            startLoopPlayback()
        }
    }

    private fun startLoopPlaybackWithFadeIn(targetVolume: Float) {
        val player = exoPlayer ?: return
        val clampedTarget = targetVolume.coerceIn(0f, 1f)
        exoPlaybackBaselineVolume = clampedTarget
        playbackFadeInJob?.cancel()
        pendingExoPlayerVolume = clampedTarget

        player.volume = 0f
        player.play()
        isLoopPlayingState.value = true
        isExoFadeInInProgress = true

        playbackFadeInJob = lifecycleScope.launch {
            val startMs = SystemClock.elapsedRealtime()
            try {
                while (isActive) {
                    val elapsedMs = SystemClock.elapsedRealtime() - startMs
                    val fraction = (elapsedMs.toFloat() / EXO_PLAYBACK_FADE_IN_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                    // Quadratic easing keeps the initial rise gentle and avoids jumpy starts.
                    val easedFraction = (fraction * fraction).coerceIn(0f, 1f)
                    player.volume = (clampedTarget * easedFraction).coerceIn(0f, 1f)

                    if (fraction >= 1f) break
                    delay(EXO_PLAYBACK_FADE_IN_STEP_MS)
                }
            } finally {
                player.volume = pendingExoPlayerVolume.coerceIn(0f, 1f)
                isExoFadeInInProgress = false
            }
        }
    }

    /*private fun startPlaybackVisualizerSync() {
        stopPlaybackVisualizerSync()
        var lastSyncMs = 0L
        playbackSyncJob = lifecycleScope.launch {
            while (isActive) {
                val player = exoPlayer
                val track = selectedPlaybackTrack()
                val nowMs = SystemClock.elapsedRealtime()
                if (player != null && track != null && player.isPlaying && nowMs - lastSyncMs >= PLAYBACK_VISUALIZER_UPDATE_MS) {
                    val dbfs = sampleTrackDbfsAtPosition(
                        track = track,
                        positionMs = player.currentPosition,
                        durationMs = player.duration
                    )
                    playbackDbfsState.value = dbfs
                    pushLevel(playbackLevelsState, dbfsToVisualizerLevel(dbfs))
                    lastSyncMs = nowMs
                }
                awaitFrame()
            }
        }
    }*/

    /*private fun stopPlaybackVisualizerSync() {
        playbackSyncJob?.cancel()
        playbackSyncJob = null
    }*/

    private fun refreshPlaybackLevelsFromSelectedTrack(startIndex: Int) {
        resetLevelHistory(playbackLevelsState)
        playbackDbfsState.value = VISUALIZER_DB_FLOOR
    }

    // --- Microphone amplitude source ---
    private fun requestMicrophonePermissionForMicVisualizer() {
        startMicVisualizerAfterPermissionGrant = true
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestMicrophonePermissionForYamnet() {
        startMicVisualizerAfterPermissionGrant = false
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun toggleMicrophoneVisualizer() {
        if (isMicRunningState.value) {
            stopMicrophoneVisualizer()
        } else {
            startMicrophoneVisualizer()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicrophoneVisualizer() {
        if (isMicRunningState.value) return
        if (!hasRecordAudioPermission()) {
            hasMicPermissionState.value = false
            requestMicrophonePermissionForMicVisualizer()
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return

        val bufferSize = maxOf(minBufferSize, MIC_BUFFER_SIZE)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                MIC_SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (_: SecurityException) {
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        micAudioRecord = recorder
        try {
            recorder.startRecording()
        } catch (_: SecurityException) {
            recorder.release()
            micAudioRecord = null
            isMicRunningState.value = false
            return
        }
        isMicRunningState.value = true

        micCaptureJob = lifecycleScope.launch(Dispatchers.Default) {
            val sampleBuffer = ShortArray(MIC_BUFFER_SIZE / 2)

            while (isActive) {
                val samplesRead = recorder.read(sampleBuffer, 0, sampleBuffer.size)
                if (samplesRead > 0) {
                    val linearLevel = calculateMicrophoneLevel(sampleBuffer, samplesRead)
                    val micDbfs = linearToDbfs(linearLevel)
                    runOnUiThread {
                        micDbfsState.value = micDbfs
                        pushLevel(micLevelsState, dbfsToVisualizerLevel(micDbfs))
                    }
                }
            }
        }
    }

    private fun stopMicrophoneVisualizer() {
        micCaptureJob?.cancel()
        micCaptureJob = null

        micAudioRecord?.let { recorder ->
            try {
                recorder.stop()
            } catch (_: IllegalStateException) {
            }
            recorder.release()
        }
        micAudioRecord = null
        isMicRunningState.value = false
        resetLevelHistory(micLevelsState)
        micDbfsState.value = VISUALIZER_DB_FLOOR
    }

    // --- Shared visualizer helpers ---
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*private fun sampleTrackDbfsAtPosition(
        track: PlaybackTrack,
        positionMs: Long,
        durationMs: Long
    ): Float {
        val levels = track.envelopeDbfsLevels
        if (levels.isEmpty()) return VISUALIZER_DB_FLOOR
        val safeDurationMs = durationMs.coerceAtLeast((track.envelopeHopMs * levels.size).toLong())
        val wrappedPositionMs = ((positionMs % safeDurationMs) + safeDurationMs) % safeDurationMs
        val positionInBins = wrappedPositionMs.toDouble() / track.envelopeHopMs.toDouble()
        val baseIndex = (positionInBins.toInt() % levels.size).coerceAtLeast(0)
        val nextIndex = (baseIndex + 1) % levels.size
        val fraction = (positionInBins - baseIndex).toFloat().coerceIn(0f, 1f)
        return lerp(levels[baseIndex], levels[nextIndex], fraction)
    }*/

    private fun calculateMicrophoneLevel(buffer: ShortArray, samplesRead: Int): Float {
        var sumSquares = 0.0
        for (i in 0 until samplesRead) {
            val value = buffer[i].toDouble()
            sumSquares += value * value
        }
        val rms = sqrt(sumSquares / samplesRead)
        return (rms / 32767.0).toFloat().coerceIn(0f, 1f)
    }

    private fun pushLevel(history: MutableList<Float>, level: Float) {
        if (history.isNotEmpty()) {
            history.removeAt(0)
        }
        history.add(level)
    }

    private fun resetLevelHistory(history: MutableList<Float>) {
        for (i in history.indices) {
            history[i] = 0f
        }
    }

    private fun linearToDbfs(linear: Float): Float {
        if (linear <= 1e-12f) return VISUALIZER_DB_FLOOR
        val db = (20.0 * log10(linear.toDouble())).toFloat()
        return db.coerceIn(VISUALIZER_DB_FLOOR, VISUALIZER_DB_CEILING)
    }

    private fun dbfsToVisualizerLevel(dbfs: Float): Float {
        val clamped = dbfs.coerceIn(VISUALIZER_DB_FLOOR, VISUALIZER_DB_CEILING)
        return ((clamped - VISUALIZER_DB_FLOOR) /
            (VISUALIZER_DB_CEILING - VISUALIZER_DB_FLOOR)).coerceIn(0f, 1f)
    }

    private fun dampenExoTargetWithPlaybackLoudness(target: Float): Float {
        val clampedBaseline = exoPlaybackBaselineVolume.coerceIn(0f, 1f)
        val clampedTarget = target.coerceIn(0f, 1f)
        val loudnessRange = MASKING_AUTOMATION_REFERENCE_DB_MAX - MASKING_AUTOMATION_REFERENCE_DB_MIN

        if (loudnessRange <= 0f) {
            return clampedTarget
        }

        val normalizedLoudness = ((playbackDbfsState.value - MASKING_AUTOMATION_REFERENCE_DB_MIN) / loudnessRange).coerceIn(0f, 1f)
        val duckAmount = normalizedLoudness * MASKING_AUTOMATION_DUCKING_FACTOR
        val targetIncrease = max(0f, clampedTarget - clampedBaseline)
        return (clampedBaseline + targetIncrease * (1f - duckAmount)).coerceIn(clampedBaseline, 1f)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun startPlaybackVisualizer() {
        val player = exoPlayer ?: return
        val audioSessionId = player.audioSessionId
        if (audioSessionId == 0) return

        try {
            visualizer?.release()
            visualizer = android.media.audiofx.Visualizer(audioSessionId).apply {
                setCaptureSize(Visualizer.getCaptureSizeRange()[1])
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
                            var sum = 0.0
                            for (b in waveform) {
                                val sample = (b.toInt() and 0xFF) / 128.0 - 1.0
                                sum += sample * sample
                            }
                            val rms = Math.sqrt(sum / waveform.size).toFloat()
                            val dbfs = linearToDbfs(rms.coerceIn(0f, 1f))
                            runOnUiThread {
                                playbackDbfsState.value = dbfs
                                pushLevel(playbackLevelsState, dbfsToVisualizerLevel(dbfs))
                            }
                        }

                        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {}
                    },
                    (1000 / 33).coerceAtMost(Visualizer.getMaxCaptureRate()), // ~30 fps
                    true,
                    false
                )
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopPlaybackVisualizer() {
        visualizer?.let {
            try {
                it.enabled = false
                it.release()
            } catch (e: Exception) { }
            visualizer = null
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }


    override fun onDestroy() {
        stopMicrophoneVisualizer()
        playbackFadeInJob?.cancel()
        stopPlaybackVisualizer()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
