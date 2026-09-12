package com.musicengine.mediapoc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.ui.glass.GlassDropdownMenu
import com.musicengine.mediapoc.ui.glass.GlassDropdownMenuItem
import com.musicengine.mediapoc.ui.glass.DualArtworkTransitionCard
import com.musicengine.mediapoc.ui.glass.GlassCard
import com.musicengine.mediapoc.ui.glass.GlassIconButton
import com.musicengine.mediapoc.ui.glass.GlassSegmentedControl
import com.musicengine.mediapoc.ui.glass.GlassShapePill
import com.musicengine.mediapoc.ui.glass.MetricTile
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.GlassBorder
import com.musicengine.mediapoc.ui.theme.GlassBorderFaint
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.GlassSurfaceStrong
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.MetricCyan
import com.musicengine.mediapoc.ui.theme.MetricGold
import com.musicengine.mediapoc.ui.theme.MetricPink
import com.musicengine.mediapoc.ui.theme.MetricPurple
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(viewModel: PlayerViewModel) {
    val topTracks by viewModel.topTracks.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val dislikedTracks by viewModel.dislikedTracks.collectAsState()
    val totalCount by viewModel.totalTrackCount.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val penalties by viewModel.activePenalties.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDislikedInLikedTab by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val trackByKey = remember(topTracks, likedTracks, dislikedTracks) {
        val map = topTracks.associateBy { it.trackKey }.toMutableMap()
        likedTracks.forEach { map[it.trackKey] = it }
        dislikedTracks.forEach { map[it.trackKey] = it }
        map
    }

    val filteredLikedTracks = remember(likedTracks, searchQuery) {
        if (searchQuery.isBlank()) likedTracks
        else likedTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredDislikedTracks = remember(dislikedTracks, searchQuery) {
        if (searchQuery.isBlank()) dislikedTracks
        else dislikedTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredTopTracks = remember(topTracks, searchQuery) {
        if (searchQuery.isBlank()) topTracks
        else topTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
    ) {
        // Screen Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MetricPurple.copy(alpha = 0.18f))
                    .border(1.dp, MetricPurple.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = MetricPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Library & Flow",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Your listening habits & transition map",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Floating Glass Metric Widgets (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Filled.Favorite,
                value = "${likedTracks.size}",
                label = "Liked",
                accentColor = MetricPink,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
            )
            MetricTile(
                icon = Icons.Filled.MusicNote,
                value = "$totalCount",
                label = "Tracks",
                accentColor = MetricPurple,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
            )
            MetricTile(
                icon = Icons.Filled.Route,
                value = "${transitions.size}",
                label = "Flow Paths",
                accentColor = MetricCyan,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 2 }
            )
            MetricTile(
                icon = Icons.Filled.Timer,
                value = "${penalties.size}",
                label = "Cooldowns",
                accentColor = if (penalties.isNotEmpty()) StatusEarlySkip else MetricGold,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 3 }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Liquid Segmented Switcher: Liked | Top Songs | Flow Map | Cooldowns
        GlassSegmentedControl(
            options = listOf("Liked", "Top Songs", "Flow Map", "Cooldowns"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Section Content
        when (selectedTab) {
            0 -> {
                // Liked / Disliked Sub-Segment Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Liked Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GlassShapePill)
                            .background(if (!showDislikedInLikedTab) AccentPink.copy(alpha = 0.20f) else GlassSurfaceStrong)
                            .border(1.dp, if (!showDislikedInLikedTab) AccentPink.copy(alpha = 0.65f) else GlassBorderFaint, GlassShapePill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showDislikedInLikedTab = false }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = if (!showDislikedInLikedTab) AccentPink else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Liked (${likedTracks.size})",
                                fontSize = 12.sp,
                                fontWeight = if (!showDislikedInLikedTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (!showDislikedInLikedTab) TextPrimary else TextSecondary
                            )
                        }
                    }

                    // Disliked Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GlassShapePill)
                            .background(if (showDislikedInLikedTab) StatusEarlySkip.copy(alpha = 0.20f) else GlassSurfaceStrong)
                            .border(1.dp, if (showDislikedInLikedTab) StatusEarlySkip.copy(alpha = 0.65f) else GlassBorderFaint, GlassShapePill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showDislikedInLikedTab = true }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ThumbDown,
                                contentDescription = null,
                                tint = if (showDislikedInLikedTab) StatusEarlySkip else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Disliked (${dislikedTracks.size})",
                                fontSize = 12.sp,
                                fontWeight = if (showDislikedInLikedTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (showDislikedInLikedTab) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                // Filter Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassShapePill)
                        .background(GlassSurfaceStrong)
                        .border(1.dp, GlassBorderFaint, GlassShapePill)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (!showDislikedInLikedTab) "Filter liked songs..." else "Filter disliked songs...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(if (!showDislikedInLikedTab) AccentPink else StatusEarlySkip),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!showDislikedInLikedTab) {
                    LikedTracksList(
                        tracks = filteredLikedTracks,
                        viewModel = viewModel,
                        hasFilter = searchQuery.isNotEmpty()
                    )
                } else {
                    DislikedTracksList(
                        tracks = filteredDislikedTracks,
                        viewModel = viewModel,
                        hasFilter = searchQuery.isNotEmpty()
                    )
                }
            }
            1 -> {
                // Filter Search Bar for Top Songs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassShapePill)
                        .background(GlassSurfaceStrong)
                        .border(1.dp, GlassBorderFaint, GlassShapePill)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Filter songs or artists...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(MetricPurple),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TopTracksList(
                    tracks = filteredTopTracks,
                    viewModel = viewModel
                )
            }
            2 -> {
                TransitionsList(
                    transitions = transitions,
                    trackByKey = trackByKey
                )
            }
            3 -> {
                PenaltiesList(
                    penalties = penalties,
                    viewModel = viewModel,
                    trackByKey = trackByKey
                )
            }
        }
    }
}

@Composable
private fun LikedTracksList(
    tracks: List<TrackEntity>,
    viewModel: PlayerViewModel,
    hasFilter: Boolean
) {
    if (tracks.isEmpty()) {
        if (hasFilter) {
            LibraryEmptyState(message = "No liked tracks matching your search.")
        } else {
            LikedEmptyState()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tracks, key = { it.trackKey }) { track ->
                TrackRowItem(
                    track = track,
                    onPlayClick = { viewModel.playTrack(track) },
                    onRate = { rating -> viewModel.rateSpecificTrack(track.trackKey, rating) }
                )
            }
        }
    }
}

@Composable
private fun DislikedTracksList(
    tracks: List<TrackEntity>,
    viewModel: PlayerViewModel,
    hasFilter: Boolean
) {
    if (tracks.isEmpty()) {
        if (hasFilter) {
            LibraryEmptyState(message = "No disliked tracks matching your search.")
        } else {
            DislikedEmptyState()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tracks, key = { it.trackKey }) { track ->
                TrackRowItem(
                    track = track,
                    onPlayClick = { viewModel.playTrack(track) },
                    onRate = { rating -> viewModel.rateSpecificTrack(track.trackKey, rating) }
                )
            }
        }
    }
}

@Composable
private fun DislikedEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(StatusEarlySkip.copy(alpha = 0.16f))
                        .border(1.dp, StatusEarlySkip.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbDown,
                        contentDescription = null,
                        tint = StatusEarlySkip,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Disliked Songs",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Songs you dislike from the 3-dots menu are permanently blocked from AI recommendations and collected here.\nYou can restore them at any time.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun LikedEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentPink.copy(alpha = 0.16f))
                        .border(1.dp, AccentPink.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = AccentPink,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Liked Songs Yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the heart icon on Now Playing or in your library to save tracks here.\nLiked songs receive a +12 pt preference boost in AI recommendations!",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun TopTracksList(
    tracks: List<TrackEntity>,
    viewModel: PlayerViewModel
) {
    if (tracks.isEmpty()) {
        LibraryEmptyState(message = "No tracks in your library yet.\nStart playing songs in Apple Music or YT Music to build your library!")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tracks, key = { it.trackKey }) { track ->
                TrackRowItem(
                    track = track,
                    onPlayClick = { viewModel.playTrack(track) },
                    onRate = { rating -> viewModel.rateSpecificTrack(track.trackKey, rating) }
                )
            }
        }
    }
}

@Composable
private fun TrackRowItem(
    track: TrackEntity,
    onPlayClick: () -> Unit,
    onRate: (UserRating) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(10.dp),
        onClick = onPlayClick
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
                if (!track.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = track.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MetricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.userRating == UserRating.DISLIKED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusEarlySkip.copy(alpha = 0.16f))
                                .border(1.dp, StatusEarlySkip.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ThumbDown,
                                    contentDescription = null,
                                    tint = StatusEarlySkip,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "DISLIKED",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusEarlySkip,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${track.totalPlays} plays",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Like Toggle
            val isLiked = track.userRating == UserRating.LIKED
            val isDisliked = track.userRating == UserRating.DISLIKED
            IconButton(
                onClick = {
                    val next = if (isLiked) UserRating.NONE else UserRating.LIKED
                    onRate(next)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) AccentPink else TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Options Menu (Liquid Glass Popup)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                GlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    GlassDropdownMenuItem(
                        text = "Play in Music App",
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        },
                        leadingIcon = Icons.Filled.PlayArrow,
                        iconTint = AccentPink
                    )
                    GlassDropdownMenuItem(
                        text = if (isLiked) "Remove Like" else "Favorite Track",
                        onClick = {
                            showMenu = false
                            onRate(if (isLiked) UserRating.NONE else UserRating.LIKED)
                        },
                        leadingIcon = Icons.Filled.Favorite,
                        iconTint = AccentPink
                    )
                    GlassDropdownMenuItem(
                        text = if (isDisliked) "Remove Dislike (Restore)" else "Dislike Track (Never Recommend)",
                        onClick = {
                            showMenu = false
                            onRate(if (isDisliked) UserRating.NONE else UserRating.DISLIKED)
                        },
                        leadingIcon = Icons.Filled.ThumbDown,
                        iconTint = if (isDisliked) TextSecondary else StatusEarlySkip
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionsList(
    transitions: List<TransitionEntity>,
    trackByKey: Map<String, TrackEntity>
) {
    if (transitions.isEmpty()) {
        LibraryEmptyState(message = "No transition flow paths recorded yet.\nAs you listen to consecutive tracks, Markov transition probabilities will appear here.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transitions, key = { "${it.fromTrackKey}->${it.toTrackKey}" }) { item ->
                val fromEntity = trackByKey[item.fromTrackKey]
                val toEntity = trackByKey[item.toTrackKey]

                val fromTitle = fromEntity?.title ?: item.fromTrackKey.substringBeforeLast(" - ").trim()
                val fromArtist = fromEntity?.artist ?: item.fromTrackKey.substringAfterLast(" - ").trim()
                val toTitle = toEntity?.title ?: item.toTrackKey.substringBeforeLast(" - ").trim()
                val toArtist = toEntity?.artist ?: item.toTrackKey.substringAfterLast(" - ").trim()

                DualArtworkTransitionCard(
                    fromTitle = fromTitle,
                    fromArtist = fromArtist,
                    fromArtworkUri = fromEntity?.artworkUri,
                    toTitle = toTitle,
                    toArtist = toArtist,
                    toArtworkUri = toEntity?.artworkUri,
                    score = item.transitionScore,
                    count = item.transitionCount
                )
            }
        }
    }
}

@Composable
private fun PenaltiesList(
    penalties: List<SkipPenaltyEntity>,
    viewModel: PlayerViewModel,
    trackByKey: Map<String, TrackEntity>
) {
    if (penalties.isEmpty()) {
        LibraryEmptyState(message = "No active cooldowns!\nTracks skipped early decay smoothly over a 4-hour half-life.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(penalties, key = { it.trackKey }) { item ->
                val track = trackByKey[item.trackKey]
                val currentPenalty = viewModel.calculateEffectivePenalty(item)
                val remaining = if (item.initialPenalty > 0f) currentPenalty / item.initialPenalty else 0f
                val title = track?.title ?: item.trackKey.substringBeforeLast(" - ").trim()
                val artist = track?.artist ?: item.trackKey.substringAfterLast(" - ").trim()

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassSurfaceStrong)
                                .border(1.dp, GlassBorderFaint, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!track?.artworkUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = track?.artworkUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val fill by animateFloatAsState(
                                targetValue = remaining,
                                animationSpec = tween(durationMillis = 700),
                                label = "decay"
                            )
                            LinearProgressIndicator(
                                progress = { fill.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = StatusEarlySkip.copy(alpha = 0.85f),
                                trackColor = GlassSurfaceSubtle
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("-%.1f pts", currentPenalty),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusEarlySkip
                            )
                            Text(
                                text = "Decaying (4h)",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}