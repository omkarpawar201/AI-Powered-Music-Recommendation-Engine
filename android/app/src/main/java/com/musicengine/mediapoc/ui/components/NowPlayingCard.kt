package com.musicengine.mediapoc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.model.PlaybackStateEnum
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import com.musicengine.mediapoc.ui.theme.AccentGreen
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.AccentRed
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.CardBorder
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPaused
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary

@Composable
fun NowPlayingCard(
    track: TrackMetadata?,
    playbackState: PlaybackTelemetryState,
    activeAppName: String,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // App badge & state pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = when {
                        activeAppName.contains("Apple", ignoreCase = true) -> AccentPink
                        activeAppName.contains("YouTube", ignoreCase = true) -> AccentRed
                        activeAppName.contains("Spotify", ignoreCase = true) -> AccentGreen
                        else -> TextMuted
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeAppName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Playback State Badge
                val (stateColor, stateText) = when (playbackState.state) {
                    PlaybackStateEnum.PLAYING -> StatusPlaying to "PLAYING"
                    PlaybackStateEnum.PAUSED -> StatusPaused to "PAUSED"
                    PlaybackStateEnum.BUFFERING -> AccentPink to "BUFFERING"
                    PlaybackStateEnum.STOPPED -> TextMuted to "STOPPED"
                    PlaybackStateEnum.NONE -> TextMuted to "IDLE"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stateText,
                        color = stateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Artwork + Title / Artist + Like/Dislike Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork Box
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBorder),
                    contentAlignment = Alignment.Center
                ) {
                    if (track?.artBitmap != null) {
                        Image(
                            bitmap = track.artBitmap.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier.size(76.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else if (track?.artUri != null) {
                        AsyncImage(
                            model = track.artUri,
                            contentDescription = "Album Art",
                            modifier = Modifier.size(76.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No Artwork",
                            tint = TextMuted,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Metadata column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "No track playing",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track?.artist ?: "Start music in Apple Music or YouTube Music",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track?.album ?: "",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Like & Dislike Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Like Button
                    IconButton(
                        onClick = { MediaNotificationListenerService.toggleLike() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        val isLiked = track?.userRating == UserRating.LIKED
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Song",
                            tint = if (isLiked) AccentPink else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Dislike Button
                    IconButton(
                        onClick = { MediaNotificationListenerService.toggleDislike() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        val isDisliked = track?.userRating == UserRating.DISLIKED
                        Icon(
                            imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                            contentDescription = "Dislike Song",
                            tint = if (isDisliked) StatusEarlySkip else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Progress Slider
            val currentFraction = if (isSeeking) seekFraction else playbackState.completionRatio
            Slider(
                value = currentFraction.coerceIn(0f, 1f),
                onValueChange = { fraction ->
                    isSeeking = true
                    seekFraction = fraction
                },
                onValueChangeFinished = {
                    if (playbackState.durationMs > 0) {
                        val targetMs = (seekFraction * playbackState.durationMs).toLong()
                        MediaNotificationListenerService.seekTo(targetMs)
                    }
                    isSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = AccentPink,
                    activeTrackColor = AccentPink,
                    inactiveTrackColor = CardBorder
                )
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPosition = if (isSeeking && playbackState.durationMs > 0) {
                    val sec = ((seekFraction * playbackState.durationMs) / 1000).toLong()
                    String.format("%02d:%02d", sec / 60, sec % 60)
                } else {
                    playbackState.formattedPosition
                }

                Text(
                    text = displayPosition,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.0f", currentFraction * 100)}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = playbackState.formattedDuration,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
