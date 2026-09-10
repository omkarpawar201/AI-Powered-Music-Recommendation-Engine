package com.musicengine.mediapoc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.ScoredCandidate
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.ui.glass.GlassButton
import com.musicengine.mediapoc.ui.glass.GlassCard
import com.musicengine.mediapoc.ui.glass.GlassShapePill
import com.musicengine.mediapoc.ui.theme.AccentGreen
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.GlassBorder
import com.musicengine.mediapoc.ui.theme.GlassBorderFaint
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.GlassSurfaceStrong
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.GlowCyan
import com.musicengine.mediapoc.ui.theme.GlowPink
import com.musicengine.mediapoc.ui.theme.MatchCyan
import com.musicengine.mediapoc.ui.theme.MatchCyanBg
import com.musicengine.mediapoc.ui.theme.MetricGold
import com.musicengine.mediapoc.ui.theme.MetricPurple
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.theme.TextSubtle
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

@Composable
fun RecommendationScreen(viewModel: PlayerViewModel) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val recommendationResult by viewModel.recommendationResult.collectAsState()
    val isRecommending by viewModel.isRecommending.collectAsState()
    val recommendationError by viewModel.recommendationError.collectAsState()
    var detailCandidate by remember { mutableStateOf<ScoredCandidate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MatchCyan.copy(alpha = 0.18f))
                        .border(1.dp, MatchCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MatchCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Recommendations",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Curated for your current vibe & flow",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Refresh / Re-rank button
            IconButton(
                onClick = { viewModel.generateRecommendations() },
                enabled = nowPlaying != null && !isRecommending
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Recommendations",
                    tint = if (nowPlaying != null && !isRecommending) MatchCyan else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Current Seed Track Header Card
        SeedTrackCard(
            seedTrack = nowPlaying,
            isRecommending = isRecommending,
            onGenerateClick = { viewModel.generateRecommendations() },
            onPlayClick = {
                nowPlaying?.let {
                    viewModel.playTrack(
                        TrackEntity(
                            trackKey = it.trackKey,
                            title = it.title,
                            artist = it.artist,
                            album = it.album,
                            durationMs = it.durationMs,
                            artworkUri = it.artUri?.toString()
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Offline Mode Banner if active
        val currentResult = recommendationResult
        if (!isRecommending && currentResult != null && currentResult.offlineUsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(StatusEarlySkip.copy(alpha = 0.12f))
                    .border(1.dp, StatusEarlySkip.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = StatusEarlySkip,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline mode - recommendations from your on-device library only",
                    fontSize = 11.sp,
                    color = StatusEarlySkip
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Body Content State Handling
        if (isRecommending) {
            AnalyzingState(modifier = Modifier.weight(1f))
        } else if (recommendationError != null) {
            ErrorState(
                message = recommendationError.orEmpty(),
                onDismiss = { viewModel.clearRecommendationError() },
                modifier = Modifier.weight(1f)
            )
        } else if (recommendationResult == null) {
            EmptyRecommendationState(
                onGenerateClick = { viewModel.generateRecommendations() },
                modifier = Modifier.weight(1f)
            )
        } else {
            val result = recommendationResult!!

            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 104.dp)
            ) {
                // 2. Best Next Song (#1 Match Hero Card)
                result.topCandidate?.let { winner ->
                    item(key = "top_candidate_hero") {
                        Text(
                            text = "✨ BEST NEXT SONG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatchCyan,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        BestNextSongHeroCard(
                            candidate = winner,
                            onPlayClick = { viewModel.playCandidate(winner.track) },
                            onDetailsClick = { detailCandidate = winner }
                        )
                    }
                }

                // 3. More Candidates Section
                if (result.rankedCandidates.isNotEmpty()) {
                    item(key = "more_candidates_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More Candidates",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${result.rankedCandidates.size} evaluated",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    items(result.rankedCandidates, key = { it.track.trackKey }) { scored ->
                        CandidateRow(
                            scored = scored,
                            onPlayClick = { viewModel.playCandidate(scored.track) },
                            onDetailsClick = { detailCandidate = scored }
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    detailCandidate?.let { scored ->
        ScoreBreakdownSheet(
            candidate = scored,
            onDismissClick = { detailCandidate = null }
        )
    }
}

/**
 * 1. Current Seed Track Header Card
 */
@Composable
private fun SeedTrackCard(
    seedTrack: TrackMetadata?,
    isRecommending: Boolean,
    onGenerateClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CURRENT SEED TRACK",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MatchCyan.copy(alpha = 0.9f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        GlassCard(
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(12.dp),
            onClick = if (seedTrack != null) onPlayClick else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seed Artwork Thumbnail
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceStrong)
                        .border(1.dp, GlassBorderFaint, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (seedTrack?.artUri != null) {
                        AsyncImage(
                            model = seedTrack.artUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MatchCyan.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seedTrack?.title ?: "No Track Playing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = seedTrack?.artist ?: "Play music in Apple Music or YT Music to seed",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action Pill / Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MatchCyan, MatchCyan.copy(alpha = 0.85f))
                            )
                        )
                        .clickable(
                            enabled = seedTrack != null && !isRecommending,
                            onClick = onGenerateClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecommending) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Generate",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Best Next Song (#1 Match Hero Card)
 */
@Composable
private fun BestNextSongHeroCard(
    candidate: ScoredCandidate,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = GlowCyan,
                spotColor = GlowPink
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MatchCyan.copy(alpha = 0.18f),
                        GlowPink.copy(alpha = 0.12f),
                        GlassSurface
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        MatchCyan.copy(alpha = 0.65f),
                        AccentPink.copy(alpha = 0.45f),
                        GlassBorderFaint
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDetailsClick
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Source Badge & #1 Match Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceBadge(source = candidate.track.source)

                // Cyan #1 Match Badge
                Box(
                    modifier = Modifier
                        .clip(GlassShapePill)
                        .background(MatchCyanBg)
                        .border(1.dp, MatchCyan.copy(alpha = 0.5f), GlassShapePill)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "#1 MATCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MatchCyan,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Track Row: Artwork + Info + Circular Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Rounded Artwork
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurfaceStrong)
                        .border(1.dp, MatchCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!candidate.track.artworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = candidate.track.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MatchCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.track.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = candidate.track.artist,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (candidate.track.genre.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = candidate.track.genre,
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Circular Vibrant Play Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MatchCyan, Color(0xFF00B0FF))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play Match",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Chips Row (Horizontal Scroll / Row)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ScoreChip(
                        label = "Match",
                        value = String.format("%.1f", candidate.breakdown.totalScore),
                        highlight = true
                    )
                }
                item {
                    ScoreChip(
                        label = "Pref",
                        value = String.format("%+d", candidate.breakdown.longTermPref.toInt())
                    )
                }
                item {
                    ScoreChip(
                        label = "Flow",
                        value = String.format("%+d", candidate.breakdown.transitionScore.toInt())
                    )
                }
                item {
                    ScoreChip(
                        label = "Artist",
                        value = String.format("%+d", candidate.breakdown.artistAffinity.toInt())
                    )
                }
                if (candidate.breakdown.skipPenalty > 0f) {
                    item {
                        ScoreChip(
                            label = "Cooldown",
                            value = String.format("-%d", candidate.breakdown.skipPenalty.toInt()),
                            isNegative = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(
    label: String,
    value: String,
    highlight: Boolean = false,
    isNegative: Boolean = false
) {
    val bgColor = when {
        highlight -> MatchCyan.copy(alpha = 0.2f)
        isNegative -> StatusEarlySkip.copy(alpha = 0.15f)
        else -> GlassSurfaceStrong
    }
    val textColor = when {
        highlight -> MatchCyan
        isNegative -> StatusEarlySkip
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (highlight) MatchCyan.copy(alpha = 0.4f) else GlassBorderFaint,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * 3. Candidate Row for Ranked Pool
 */
@Composable
private fun CandidateRow(
    scored: ScoredCandidate,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(10.dp),
        onClick = onDetailsClick
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
                if (!scored.track.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = scored.track.artworkUri,
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

            // Candidate Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(source = scored.track.source)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scored.track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scored.track.artist,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Score ${String.format("%.1f", scored.breakdown.totalScore)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatchCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceStrong)
                    .border(1.dp, GlassBorderFaint, CircleShape)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 3-Dots for details
            IconButton(
                onClick = onDetailsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Breakdown",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SourceBadge(source: CandidateSource) {
    val (label, color) = when (source) {
        CandidateSource.TRANSITION_HISTORY -> "Flow Path" to StatusPlaying
        CandidateSource.PERSONAL_LIBRARY -> "Library" to MetricPurple
        CandidateSource.CATALOG_ARTIST_TOP -> "Artist Top" to MetricGold
        CandidateSource.CATALOG_SEARCH -> "Catalog" to AccentGreen
        CandidateSource.EXPLORATION -> "Discovery" to MatchCyan
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun AnalyzingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MatchCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Curating Next Tracks...",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Analyzing Markov transitions, taste affinity, and catalog",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = "Couldn't curate recommendations",
                color = StatusEarlySkip,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton(
                text = "Dismiss",
                onClick = onDismiss,
                fullWidth = true,
                containerColor = GlassSurfaceStrong
            )
        }
    }
}

@Composable
private fun EmptyRecommendationState(
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MatchCyan.copy(alpha = 0.12f))
                    .border(1.dp, MatchCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MatchCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap to Discover Next Songs",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The AI brain analyzes your Personal Library, Markov Flow, and iTunes Catalog to rank the best matching next tracks.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            GlassButton(
                text = "Curate Next Songs",
                onClick = onGenerateClick,
                containerColor = MatchCyan,
                contentColor = Color.Black,
                accentBorder = Color.White.copy(alpha = 0.4f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreBreakdownSheet(
    candidate: ScoredCandidate,
    onDismissClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissClick,
        containerColor = DarkBg,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Why this track was chosen",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MatchCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(source = candidate.track.source)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = candidate.track.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = candidate.track.artist,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (candidate.track.genre.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Genre: ${candidate.track.genre}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            BreakdownRow("Taste Preference (completion & likes)", candidate.breakdown.longTermPref)
            BreakdownRow("Markov Flow Transition (A ➔ C)", candidate.breakdown.transitionScore)
            BreakdownRow("Artist Affinity / Catalog Boost", candidate.breakdown.artistAffinity)
            BreakdownRow("Genre Affinity", candidate.breakdown.genreAffinity)
            BreakdownRow("Replay Factor", candidate.breakdown.replayBoost)
            BreakdownRow("Discovery & Novelty", candidate.breakdown.noveltyBoost)
            BreakdownRow("Decaying Skip Penalty", -candidate.breakdown.skipPenalty)
            BreakdownRow("Recency Penalty", -candidate.breakdown.recencyPenalty)

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = GlassBorderFaint)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL MATCH SCORE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = String.format("%.1f pts", candidate.breakdown.totalScore),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatchCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "All factors are computed on-device. Skips decay with a 4-hour half-life; recency fades after ~15 songs.",
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: Float) {
    val color = when {
        value > 0f -> StatusPlaying
        value < 0f -> StatusEarlySkip
        else -> TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%+.1f", value),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}