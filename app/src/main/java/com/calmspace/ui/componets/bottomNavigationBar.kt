package com.calmspace.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// ─────────────────────────────────────────────
// Bottom Navigation
// ─────────────────────────────────────────────

@Composable
fun BottomNavigationBar() 
{
    NavigationBar {

        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Monitor") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.AccountBox, null) },
            label = { Text("Profile") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}
