package com.musicengine.mediapoc.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicengine.mediapoc.model.PlaybackStateEnum
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import com.musicengine.mediapoc.ui.theme.AccentGreen
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.AccentRed
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.CardBorder
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary

@Composable
fun PlaybackControlPanel(
    playbackState: PlaybackTelemetryState,
    activeAppName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("Starboy The Weeknd") }
    var selectedTargetApp by remember { mutableStateOf("Auto") }
    var lastCommandResult by remember { mutableStateOf<String?>(null) }

    val presetQueries = listOf(
        "Starboy The Weeknd",
        "Blinding Lights The Weeknd",
        "Kesariya Arijit Singh",
        "Tum Hi Ho Arijit Singh",
        "Save Your Tears The Weeknd"
    )

    val targetApps = listOf("Auto", "Apple Music", "YouTube Music", "Spotify")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POC 2: Playback & Search Controls",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Active: $activeAppName",
                    color = AccentPink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Basic Transport Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek -15s
                IconButton(
                    onClick = {
                        val newPos = (playbackState.positionMs - 15000L).coerceAtLeast(0L)
                        MediaNotificationListenerService.seekTo(newPos)
                        lastCommandResult = "Dispatched: seekTo(${newPos / 1000}s)"
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 15s",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = {
                        val success = MediaNotificationListenerService.skipToPrevious()
                        lastCommandResult = if (success) "Dispatched: skipToPrevious()" else "No controller bound"
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause Toggle
                IconButton(
                    onClick = {
                        val isPlaying = playbackState.state == PlaybackStateEnum.PLAYING
                        val success = if (isPlaying) {
                            MediaNotificationListenerService.pause()
                        } else {
                            MediaNotificationListenerService.play()
                        }
                        lastCommandResult = if (success) {
                            if (isPlaying) "Dispatched: pause()" else "Dispatched: play()"
                        } else {
                            "Failed: No controller bound"
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentPink)
                ) {
                    Icon(
                        imageVector = if (playbackState.state == PlaybackStateEnum.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Next Track
                IconButton(
                    onClick = {
                        val success = MediaNotificationListenerService.skipToNext()
                        lastCommandResult = if (success) "Dispatched: skipToNext()" else "No controller bound"
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Seek +15s
                IconButton(
                    onClick = {
                        val newPos = (playbackState.positionMs + 15000L).coerceAtMost(
                            if (playbackState.durationMs > 0) playbackState.durationMs else Long.MAX_VALUE
                        )
                        MediaNotificationListenerService.seekTo(newPos)
                        lastCommandResult = "Dispatched: seekTo(${newPos / 1000}s)"
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 15s",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. PlayFromSearch & Target App Routing
            Text(
                text = "Target Music Player for Search",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // App Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                targetApps.forEach { appName ->
                    val isSelected = selectedTargetApp == appName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTargetApp = appName },
                        label = { Text(appName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (appName) {
                                "Apple Music" -> AccentPink.copy(alpha = 0.25f)
                                "YouTube Music" -> AccentRed.copy(alpha = 0.25f)
                                "Spotify" -> AccentGreen.copy(alpha = 0.25f)
                                else -> AccentPink.copy(alpha = 0.2f)
                            },
                            selectedLabelColor = TextPrimary,
                            containerColor = CardBorder,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Search Songs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetQueries.forEach { preset ->
                    FilterChip(
                        selected = (searchQuery == preset),
                        onClick = { searchQuery = preset },
                        label = { Text(preset, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPink.copy(alpha = 0.2f),
                            selectedLabelColor = AccentPink,
                            containerColor = CardBorder,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Query Input Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Song Title / Artist", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPink,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Execution Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button 1: Smart Search & Play (MediaSession playFromSearch with fallback)
                Button(
                    onClick = {
                        val extras = Bundle().apply {
                            putString(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
                            putString(MediaStore.EXTRA_MEDIA_TITLE, searchQuery.substringBefore(" - ").substringBefore(" by "))
                            putString(MediaStore.EXTRA_MEDIA_ARTIST, searchQuery.substringAfter(" - ", "").substringAfter(" by ", ""))
                        }

                        val targetPkg = getPackageForSelection(selectedTargetApp)
                        val activePkg = MediaNotificationListenerService.getActivePackageName()

                        if (targetPkg != null && targetPkg != activePkg) {
                            // Target is explicitly different from currently active -> Launch deep link directly
                            launchAppSearchIntent(context, searchQuery, targetPkg) { res ->
                                lastCommandResult = res
                            }
                        } else {
                            val success = MediaNotificationListenerService.playFromSearch(searchQuery, extras)
                            if (success) {
                                lastCommandResult = "✅ Dispatched playFromSearch(\"$searchQuery\") to active session"
                            } else {
                                // Fallback to deep-link intent
                                val fallbackPkg = targetPkg ?: if (activePkg.isNotEmpty()) activePkg else "com.apple.android.music"
                                launchAppSearchIntent(context, searchQuery, fallbackPkg) { res ->
                                    lastCommandResult = res
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1. Play Song via playFromSearch()", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Button 2: Direct App Search Deep-Link
                Button(
                    onClick = {
                        val targetPkg = getPackageForSelection(selectedTargetApp)
                            ?: MediaNotificationListenerService.getActivePackageName().ifEmpty { "com.apple.android.music" }
                        launchAppSearchIntent(context, searchQuery, targetPkg) { res ->
                            lastCommandResult = res
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2. Open Search in $selectedTargetApp (Deep-Link)", fontSize = 12.sp)
                }
            }

            // Command Feedback Banner
            if (lastCommandResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBorder)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = lastCommandResult ?: "",
                        color = StatusPlaying,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getPackageForSelection(selection: String): String? {
    return when (selection) {
        "Apple Music" -> "com.apple.android.music"
        "YouTube Music" -> "app.morphe.android.apps.youtube.music"
        "Spotify" -> "com.spotify.music"
        else -> null
    }
}

private fun launchAppSearchIntent(
    context: Context,
    query: String,
    targetPackage: String,
    onResult: (String) -> Unit
) {
    try {
        val intent = when {
            targetPackage.contains("apple", ignoreCase = true) -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://music.apple.com/search?term=${Uri.encode(query)}")).apply {
                    setPackage(targetPackage)
                }
            }
            targetPackage.contains("youtube", ignoreCase = true) -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${Uri.encode(query)}")).apply {
                    setPackage(targetPackage)
                }
            }
            targetPackage.contains("spotify", ignoreCase = true) -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}")).apply {
                    setPackage(targetPackage)
                }
            }
            else -> {
                Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
                    putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        val appName = targetPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        onResult("✅ Launched $appName with search for: \"$query\"")
    } catch (e: Exception) {
        onResult("❌ Intent error: ${e.message}")
    }
}
