package com.calmspace.ui.player

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

class UserTracksManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("user_tracks", Context.MODE_PRIVATE)

    fun getSavedUris(): List<String> =
        prefs.getStringSet("uris", emptySet())?.toList() ?: emptyList()

    fun addUri(uri: Uri): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val currentUris = getSavedUris().toMutableSet()
            currentUris.add(uri.toString())
            prefs.edit { putStringSet("uris", currentUris) }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun createTrackFromUri(uri: Uri): PlaybackTrack? {
        val fileName = getFileNameFromUri(uri) ?: return null
        return PlaybackTrack(
            id = "user_${System.currentTimeMillis()}",
            title = fileName,
            uriString = uri.toString()
            // other parameters default to 0/empty
        )
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }
}