package com.musicengine.mediapoc.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.PermissionRequestCard
import com.musicengine.mediapoc.model.PlaybackStateEnum
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import com.musicengine.mediapoc.ui.glass.GlassCard
import com.musicengine.mediapoc.ui.glass.GlassDropdownMenu
import com.musicengine.mediapoc.ui.glass.GlassDropdownMenuItem
import com.musicengine.mediapoc.ui.glass.GlassIconButton
import com.musicengine.mediapoc.ui.glass.GlassShapePill
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.GlassBorder
import com.musicengine.mediapoc.ui.theme.GlassBorderFaint
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.GlassSurfaceStrong
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.MatchCyan
import com.musicengine.mediapoc.ui.theme.ProgressInactive
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.theme.rememberArtworkPalette
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: PlayerViewModel) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isPermissionGranted by viewModel.isMediaPermissionGranted.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val activeApp by viewModel.activeApp.collectAsState()
    val events by viewModel.events.collectAsState()
    val upNextTrack by viewModel.upNextTrack.collectAsState()

    val artModel = nowPlaying?.let { it.artUri ?: it.artBitmap }
    val palette = rememberArtworkPalette(artModel)

    var showMenu by remember { mutableStateOf(false) }
    var showTelemetrySheet by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    val isPlaying = playbackState.state == PlaybackStateEnum.PLAYING
    val currentPosition = if (isSeeking) seekPositionMs.toLong() else playbackState.positionMs
    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 104.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Top Header (Active Player Pill, Like Button, Overflow Menu) ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active Player Pill
            Box(
                modifier = Modifier
                    .clip(GlassShapePill)
                    .background(GlassSurfaceStrong)
                    .border(1.dp, GlassBorderFaint, GlassShapePill)
                    .clickable { showMenu = true }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) StatusPlaying else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeApp.contains("Apple", ignoreCase = true)) "Apple Music"
                        else if (activeApp.contains("YouTube", ignoreCase = true) || activeApp.contains("YT", ignoreCase = true)) "YouTube Music"
                        else activeApp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Actions: Heart Like, Dislike & 3-Dots Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLiked = nowPlaying?.userRating == UserRating.LIKED
                val isDisliked = nowPlaying?.userRating == UserRating.DISLIKED

                val heartScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "heartSpring"
                )
                val thumbScale by animateFloatAsState(
                    targetValue = if (isDisliked) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "thumbSpring"
                )

                // Like Button
                GlassIconButton(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = if (isLiked) palette.primary else TextSecondary,
                    onClick = {
                        val newRating = if (isLiked) UserRating.NONE else UserRating.LIKED
                        viewModel.rateTrack(newRating)
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    },
                    size = 38.dp,
                    iconSize = 19.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Dislike Button
                GlassIconButton(
                    icon = Icons.Filled.ThumbDown,
                    contentDescription = "Dislike Song (Never Recommend)",
                    tint = if (isDisliked) StatusEarlySkip else TextMuted,
                    onClick = {
                        val newRating = if (isDisliked) UserRating.NONE else UserRating.DISLIKED
                        viewModel.rateTrack(newRating)
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                    },
                    size = 38.dp,
                    iconSize = 18.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    GlassIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        onClick = { showMenu = !showMenu },
                        size = 38.dp,
                        iconSize = 19.dp
                    )

                    GlassDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        GlassDropdownMenuItem(
                            text = "Developer Telemetry Sheet",
                            onClick = {
                                showMenu = false
                                showTelemetrySheet = true
                            },
                            leadingIcon = Icons.AutoMirrored.Filled.QueueMusic,
                            iconTint = MatchCyan
                        )
                        GlassDropdownMenuItem(
                            text = "Refresh Media Sessions",
                            onClick = {
                                showMenu = false
                                viewModel.refreshSessions()
                            },
                            leadingIcon = Icons.Filled.Refresh,
                            iconTint = TextSecondary
                        )
                        GlassDropdownMenuItem(
                            text = "Clear Event Log",
                            onClick = {
                                showMenu = false
                                viewModel.clearEventLog()
                            },
                            leadingIcon = Icons.Filled.Delete,
                            iconTint = StatusEarlySkip
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── Permission Banner if needed ─────────────────────────────────────
        if (!isPermissionGranted) {
            PermissionRequestCard(
                title = "Notification Access Required",
                description = "Grant Notification Listener permission so the engine can capture playback telemetry.",
                buttonText = "Grant Access",
                icon = Icons.Filled.Settings,
                onGrantClick = { viewModel.openNotificationListenerSettings() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else if (isBatteryOptimized) {
            PermissionRequestCard(
                title = "Battery Optimization Active",
                description = "Disable battery optimization so Android doesn't kill the background listener service.",
                buttonText = "Disable Optimization",
                icon = Icons.Filled.Settings,
                onGrantClick = { viewModel.requestBatteryOptimizationExemption() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ─── Floating Hero Album Artwork (Apple Music-Style Fluid Spring Transition) ──
        val artScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.0f else 0.88f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 300f
            ),
            label = "heroArtScale"
        )

        val artElevation by animateDpAsState(
            targetValue = if (isPlaying) 32.dp else 12.dp,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 300f
            ),
            label = "heroArtElevation"
        )

        val borderSpecularAlpha by animateFloatAsState(
            targetValue = if (isPlaying) 0.48f else 0.20f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 300f
            ),
            label = "heroArtBorderAlpha"
        )

        Box(
            modifier = Modifier
                .size(270.dp)
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                }
                .shadow(
                    elevation = artElevation,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = palette.primary.copy(alpha = if (isPlaying) 0.55f else 0.22f),
                    spotColor = palette.secondary.copy(alpha = if (isPlaying) 0.45f else 0.16f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(GlassSurfaceStrong)
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = borderSpecularAlpha),
                            palette.primary.copy(alpha = borderSpecularAlpha * 0.75f),
                            GlassBorderFaint
                        )
                    ),
                    RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artModel != null) {
                Crossfade(targetState = artModel, label = "artworkCrossfade") { model ->
                    AsyncImage(
                        model = model,
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.15f))
                            .border(1.dp, palette.primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ready to Play",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // ─── Track Title & Artist Info ───────────────────────────────────────
        AnimatedContent(
            targetState = nowPlaying,
            transitionSpec = {
                (fadeIn(animationSpec = tween(320)) + slideInVertically(animationSpec = tween(320)) { 18 })
                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -18 })
            },
            label = "trackInfoTransition"
        ) { track ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.title ?: "No Track Playing",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track?.artist ?: "Start music in Apple Music or YT Music",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!track?.album.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track?.album.orEmpty(),
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Liquid Timeline Progress Bar ────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    isSeeking = true
                    seekPositionMs = newProgress * duration
                },
                onValueChangeFinished = {
                    isSeeking = false
                    MediaNotificationListenerService.seekTo(seekPositionMs.toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = palette.primary,
                    inactiveTrackColor = ProgressInactive
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                val remainingMs = (duration - currentPosition).coerceAtLeast(0L)
                Text(
                    text = "-${formatDuration(remainingMs)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── Floating Playback Controls Bar ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Track
            GlassIconButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Track",
                onClick = { MediaNotificationListenerService.skipToPrevious() },
                size = 54.dp,
                iconSize = 28.dp
            )

            // Hero Play / Pause Orb
            val playInteractionSource = remember { MutableInteractionSource() }
            val playPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.93f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                label = "playScale"
            )

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = palette.primary.copy(alpha = 0.6f),
                        spotColor = palette.secondary.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.primary, palette.secondary.copy(alpha = 0.9f))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        onClick = {
                            if (isPlaying) {
                                MediaNotificationListenerService.pause()
                            } else {
                                MediaNotificationListenerService.play()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Next Track
            GlassIconButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                onClick = { MediaNotificationListenerService.skipToNext() },
                size = 54.dp,
                iconSize = 28.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Floating "Up Next (AI)" Preview Card ────────────────────────────
        upNextTrack?.let { upNext ->
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(12.dp),
                onClick = { viewModel.playCandidate(upNext) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurfaceStrong)
                            .border(1.dp, GlassBorderFaint, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!upNext.artworkUri.isNullOrBlank()) {
                            AsyncImage(
                                model = upNext.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MatchCyan, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(GlassShapePill)
                                    .background(MatchCyan.copy(alpha = 0.16f))
                                    .border(1.dp, MatchCyan.copy(alpha = 0.4f), GlassShapePill)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MatchCyan, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "UP NEXT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MatchCyan)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = upNext.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = upNext.artist,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceStrong)
                            .border(1.dp, GlassBorderFaint, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play Up Next", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // ─── Developer Telemetry Sheet ──────────────────────────────────────────
    if (showTelemetrySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showTelemetrySheet = false },
            sheetState = sheetState,
            containerColor = DarkBg,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Telemetry Diagnostics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live MediaSession events and engine listener state",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TelemetryRow("Active Music Player", activeApp)
                    TelemetryRow("Playback State", playbackState.state.name)
                    TelemetryRow("Position", "${playbackState.positionMs / 1000}s / ${playbackState.durationMs / 1000}s")
                    TelemetryRow("Completion Ratio", "${(playbackState.completionRatio * 100).toInt()}%")
                    TelemetryRow("Recent Event Count", "${events.size} captured")
                }

                Spacer(modifier = Modifier.height(16.dp))
                EventLogList(events = events)
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}