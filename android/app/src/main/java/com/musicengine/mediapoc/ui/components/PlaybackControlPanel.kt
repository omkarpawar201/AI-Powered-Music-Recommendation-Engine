package com.musicengine.mediapoc.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicengine.mediapoc.model.PlaybackStateEnum
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import com.musicengine.mediapoc.ui.glass.GlassButton
import com.musicengine.mediapoc.ui.glass.GlassChip
import com.musicengine.mediapoc.ui.glass.GlassIconButton
import com.musicengine.mediapoc.ui.glass.glassSurface
import com.musicengine.mediapoc.ui.glass.GlassShapeCard
import com.musicengine.mediapoc.ui.theme.AccentGreen
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.AccentRed
import com.musicengine.mediapoc.ui.theme.GlassBorderFaint
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary

@Suppress("UNUSED_PARAMETER")
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = GlassShapeCard)
            .padding(16.dp)
    ) {
        // Target App Selector
        Text(
            text = "Target Music Player for Search",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            targetApps.forEach { appName ->
                val isSelected = selectedTargetApp == appName
                val tint = when (appName) {
                    "Apple Music" -> AccentPink
                    "YouTube Music" -> AccentRed
                    "Spotify" -> AccentGreen
                    else -> AccentPink
                }
                GlassChip(
                    text = appName,
                    selected = isSelected,
                    onClick = { selectedTargetApp = appName },
                    selectedTint = tint
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Search Songs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetQueries.forEach { preset ->
                GlassChip(
                    text = preset,
                    selected = (searchQuery == preset),
                    onClick = { searchQuery = preset }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Query Input Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Song Title / Artist", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPink,
                unfocusedBorderColor = GlassBorderFaint,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurfaceSubtle,
                cursorColor = AccentPink
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Execution Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Button 1: Smart Search & Play (MediaSession playFromSearch with fallback)
            GlassButton(
                text = "1. Play Song via playFromSearch()",
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
                containerColor = AccentPink,
                contentColor = Color.White,
                accentBorder = Color.White.copy(alpha = 0.35f),
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            )

            // Button 2: Direct App Search Deep-Link
            GlassButton(
                text = "2. Open Search in $selectedTargetApp (Deep-Link)",
                onClick = {
                    val targetPkg = getPackageForSelection(selectedTargetApp)
                        ?: MediaNotificationListenerService.getActivePackageName().ifEmpty { "com.apple.android.music" }
                    launchAppSearchIntent(context, searchQuery, targetPkg) { res ->
                        lastCommandResult = res
                    }
                },
                containerColor = GlassSurface,
                contentColor = TextPrimary,
                accentBorder = GlassBorderFaint,
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            )
        }

        // Command Feedback Banner
        if (lastCommandResult != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(StatusPlaying.copy(alpha = 0.1f))
                    .border(1.dp, StatusPlaying.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = lastCommandResult ?: "",
                    color = StatusPlaying,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlaybackTransportRow(
    playbackState: PlaybackTelemetryState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Seek -15s
        GlassIconButton(
            onClick = {
                val newPos = (playbackState.positionMs - 15000L).coerceAtLeast(0L)
                MediaNotificationListenerService.seekTo(newPos)
            },
            icon = Icons.Filled.FastRewind,
            contentDescription = "Rewind 15s",
            modifier = Modifier.size(42.dp),
            iconSize = 20.dp,
            tint = TextPrimary
        )

        // Previous Track
        GlassIconButton(
            onClick = { MediaNotificationListenerService.skipToPrevious() },
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Previous Track",
            modifier = Modifier.size(42.dp),
            iconSize = 22.dp,
            tint = TextPrimary
        )

        // Play / Pause Toggle
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val containerColor by animateColorAsState(
            targetValue = if (pressed) AccentPink.copy(alpha = 0.85f) else AccentPink,
            label = "transportPlay"
        )
        androidx.compose.material3.IconButton(
            onClick = {
                val isPlaying = playbackState.state == PlaybackStateEnum.PLAYING
                if (isPlaying) {
                    MediaNotificationListenerService.pause()
                } else {
                    MediaNotificationListenerService.play()
                }
            },
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(containerColor, containerColor.copy(alpha = 0.82f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = if (playbackState.state == PlaybackStateEnum.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Next Track
        GlassIconButton(
            onClick = { MediaNotificationListenerService.skipToNext() },
            icon = Icons.Filled.SkipNext,
            contentDescription = "Next Track",
            modifier = Modifier.size(42.dp),
            iconSize = 22.dp,
            tint = TextPrimary
        )

        // Seek +15s
        GlassIconButton(
            onClick = {
                val newPos = (playbackState.positionMs + 15000L).coerceAtMost(
                    if (playbackState.durationMs > 0) playbackState.durationMs else Long.MAX_VALUE
                )
                MediaNotificationListenerService.seekTo(newPos)
            },
            icon = Icons.Filled.FastForward,
            contentDescription = "Forward 15s",
            modifier = Modifier.size(42.dp),
            iconSize = 20.dp,
            tint = TextPrimary
        )
    }
}

private fun getPackageForSelection(selection: String): String? {
    return when (selection) {
        "Apple Music" -> "com.apple.android.music"
        "YouTube Music" -> "com.google.android.apps.youtube.music"
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
    val intent = buildSearchIntent(query, targetPackage)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
        val appName = targetPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        onResult("✅ Launched $appName with search for: \"$query\"")
    } catch (e: Exception) {
        // YouTube Music official client may be absent; fall back to alternate clients,
        // then to a generic browser deep link so search still works.
        if (targetPackage.contains("youtube", ignoreCase = true)) {
            val fallbacks = listOf(
                "app.morphe.android.apps.youtube.music",
                "app.revanced.android.apps.youtube.music"
            )
            for (fallback in fallbacks) {
                try {
                    val retryIntent = buildSearchIntent(query, fallback)
                    retryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(retryIntent)
                    onResult("✅ Launched YouTube Music (alternate client) for: \"$query\"")
                    return
                } catch (_: Exception) { /* try next fallback */ }
            }
        }
        onResult("❌ Intent error: ${e.message}")
    }
}

private fun buildSearchIntent(query: String, targetPackage: String): Intent {
    return when {
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
}