package com.calmspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.calmspace.ui.components.bottomNavigationBar
import com.calmspace.ui.theme.CalmSpaceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CalmSpaceTheme {
                CalmSpaceApp()
            }
        }
    }
}

// ─────────────────────────────────────────────
// Application Layout
// ─────────────────────────────────────────────

@Composable
fun CalmSpaceApp() {
    Scaffold(
        bottomBar = { bottomNavigationBar() }
    ) { paddingValues ->
        Text(
            text = "Main Screen",
            modifier = Modifier.padding(paddingValues)
        )
    }
}
