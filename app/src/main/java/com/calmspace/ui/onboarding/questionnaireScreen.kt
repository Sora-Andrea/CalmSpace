package com.calmspace.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Questionnaire Screen
// First-time onboarding flow
// Includes permissions as final step
// ─────────────────────────────────────────────

@Composable
fun questionnaireScreen(
    onFinish: () -> Unit
) {

    // ─────────────────────────────────────────────
    // Questionnaire State
    // TODO: Move to ViewModel later
    // ─────────────────────────────────────────────
    var sleepEnvironment by remember { mutableStateOf("") }
    var noiseSensitivity by remember { mutableStateOf("") }
    val commonNoises = remember { mutableStateListOf<String>() }
    var sleepSound by remember { mutableStateOf("") }

    var micPermission by remember { mutableStateOf(false) }
    var notificationPermission by remember { mutableStateOf(false) }

    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val progress =
        (pagerState.currentPage + 1).toFloat() / totalPages.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // ───────── App Title ─────────
        Text(
            text = "CalmSpace",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ───────── Progress Bar ─────────
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Swipeable Pages ─────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {

                // ───── Page 1: Sleep Environment ─────
                0 -> RadioQuestion(
                    title = "Sleep Environment",
                    options = listOf(
                        "Quiet",
                        "Occasionally noisy",
                        "Frequently noisy",
                        "Very noisy"
                    ),
                    selected = sleepEnvironment,
                    onSelect = { sleepEnvironment = it }
                )

                // ───── Page 2: Noise Sensitivity ─────
                1 -> RadioQuestion(
                    title = "Noise Sensitivity",
                    options = listOf(
                        "Light sleeper",
                        "Average sleeper",
                        "Heavy sleeper"
                    ),
                    selected = noiseSensitivity,
                    onSelect = { noiseSensitivity = it }
                )

                // ───── Page 3: Common Noise Types ─────
                2 -> CheckboxQuestion(
                    title = "Common Noise Types",
                    options = listOf(
                        "Traffic / vehicles",
                        "Voices / neighbors",
                        "Pets",
                        "Household sounds",
                        "Sudden loud noises",
                        "Not sure"
                    ),
                    selectedItems = commonNoises
                )

                // ───── Page 4: Sleep Sound ─────
                3 -> RadioQuestion(
                    title = "Preferred Sleep Sound",
                    options = listOf(
                        "White noise",
                        "Brown noise",
                        "Rain",
                        "Ocean waves",
                        "Fan"
                    ),
                    selected = sleepSound,
                    onSelect = { sleepSound = it }
                )

                // ───── Page 5: Permissions ─────
                4 -> PermissionPage(
                    micPermission = micPermission,
                    notificationPermission = notificationPermission,
                    onMicToggle = { micPermission = it },
                    onNotificationToggle = { notificationPermission = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ───────── Progress Dots ─────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .padding(4.dp)
                        .background(
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.LightGray,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ───────── Finish Button (Last Page Only) ─────────
        if (pagerState.currentPage == totalPages - 1) {
            Button(
                onClick = {
                    // TODO:
                    // - Request Android permissions
                    // - Save questionnaire answers
                    // - Mark onboarding complete
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = micPermission && notificationPermission
            ) {
                Text("Finish Setup")
            }
        }
    }
}

// ─────────────────────────────────────────────
// Question Components
// ─────────────────────────────────────────────

@Composable
fun RadioQuestion(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        options.forEach { option ->
            OutlinedButton(
                onClick = { onSelect(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(option)
            }
        }
    }
}

@Composable
fun CheckboxQuestion(
    title: String,
    options: List<String>,
    selectedItems: MutableList<String>
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = selectedItems.contains(option),
                    onCheckedChange = {
                        if (it) selectedItems.add(option)
                        else selectedItems.remove(option)
                    }
                )
                Text(option)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Permissions Page  (will create a component once Settings is implemented)
// ─────────────────────────────────────────────

@Composable
fun PermissionPage(
    micPermission: Boolean,
    notificationPermission: Boolean,
    onMicToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit
) {
    Column {

        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text =
                "CalmSpace requires the following permissions to function correctly. " +
                "All processing occurs locally on your device and no audio is recorded or stored.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionRow(
            title = "Microphone Access",
            description =
                "Used to monitor ambient noise during sleep so CalmSpace can automatically " +
                "adjust masking audio. Audio is analyzed in real time and never saved.",
            checked = micPermission,
            onToggle = onMicToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionRow(
            title = "Notifications",
            description =
                "Required to keep sleep sessions running overnight using a foreground service " +
                "and to provide session controls without waking the screen.",
            checked = notificationPermission,
            onToggle = onNotificationToggle
        )
    }
}

// ─────────────────────────────────────────────
// Permission Row Component (will create a component once Settings imnplemented)
// ─────────────────────────────────────────────

@Composable
fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
