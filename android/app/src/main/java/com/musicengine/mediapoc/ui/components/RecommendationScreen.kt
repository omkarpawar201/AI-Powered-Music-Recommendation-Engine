package com.musicengine.mediapoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.ScoredCandidate
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.ui.theme.AccentGreen
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.AccentRed
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.CardBorder
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.StatusReplay
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

@Composable
fun RecommendationScreen(viewModel: PlayerViewModel) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val recommendationResult by viewModel.recommendationResult.collectAsState()
    val isRecommending by viewModel.isRecommending.collectAsState()
    val recommendationError by viewModel.recommendationError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Seed Track & Trigger Header
        SeedTrackHeader(
            seedTrack = nowPlaying,
            isRecommending = isRecommending,
            onGenerateClick = { viewModel.generateRecommendations() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecommending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentPink, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Generating & Ranking Candidate Pool...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (recommendationError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Couldn't generate recommendations",
                        color = StatusEarlySkip,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recommendationError ?: "",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.clearRecommendationError() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (recommendationResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap 'Recommend Next Song' above to generate on-device recommendations from your Personal Library, Markov Transitions, and iTunes Catalog.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            val result = recommendationResult!!

            // Top Winner Hero Card
            result.topCandidate?.let { winner ->
                WinnerHeroCard(
                    candidate = winner,
                    onPlayClick = { viewModel.playCandidate(winner.track) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ranked Candidate Pool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ranked Candidates (${result.rankedCandidates.size} of ${result.candidatePoolSize} evaluated)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(result.rankedCandidates, key = { it.track.trackKey }) { scored ->
                    CandidateRow(
                        scored = scored,
                        onPlayClick = { viewModel.playCandidate(scored.track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeedTrackHeader(
    seedTrack: TrackMetadata?,
    isRecommending: Boolean,
    onGenerateClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "CURRENT SEED TRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPink)
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
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
                            tint = TextMuted,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seedTrack?.title ?: "No Track Playing",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = seedTrack?.artist ?: "Start playback in Apple Music or YT Music",
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGenerateClick,
                enabled = seedTrack != null && !isRecommending,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecommending) "Analyzing & Ranking..." else "Recommend Next Song",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WinnerHeroCard(
    candidate: ScoredCandidate,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AccentPink.copy(alpha = 0.25f),
                        StatusReplay.copy(alpha = 0.15f)
                    )
                )
            )
            .border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg)
            ) {
                if (candidate.track.artworkUri != null) {
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
                        tint = AccentPink,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(source = candidate.track.source)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#1 RECOMMENDATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusPlaying
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = candidate.track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = candidate.track.artist,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = StatusPlaying),
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun CandidateRow(
    scored: ScoredCandidate,
    onPlayClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(source = scored.track.source)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scored.track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = scored.track.artist,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Score: ${String.format("%.1f", scored.breakdown.totalScore)} (Pref: +${scored.breakdown.longTermPref.toInt()} | Trans: ${if (scored.breakdown.transitionScore >= 0) "+" else ""}${scored.breakdown.transitionScore.toInt()} | Art: +${scored.breakdown.artistAffinity.toInt()}${if (scored.breakdown.skipPenalty > 0) " | -Skip: ${scored.breakdown.skipPenalty.toInt()}" else ""})",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SourceBadge(source: CandidateSource) {
    val (label, color) = when (source) {
        CandidateSource.TRANSITION_HISTORY -> "Transition" to StatusPlaying
        CandidateSource.PERSONAL_LIBRARY -> "Library" to AccentPink
        CandidateSource.CATALOG_ARTIST_TOP -> "Artist Top" to StatusReplay
        CandidateSource.CATALOG_SEARCH -> "Catalog" to AccentGreen
        CandidateSource.EXPLORATION -> "Discovery" to Color.Cyan
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
