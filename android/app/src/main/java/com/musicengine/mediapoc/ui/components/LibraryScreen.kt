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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.StatusReplay
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

@Composable
fun LibraryScreen(viewModel: PlayerViewModel) {
    val topTracks by viewModel.topTracks.collectAsState()
    val totalTracks by viewModel.totalTrackCount.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val penalties by viewModel.activePenalties.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Top Tracks (${topTracks.size})", "Transitions (${transitions.size})", "Penalties (${penalties.size})")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats Overview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Library", value = totalTracks.toString(), color = AccentPink)
            StatItem(label = "Transitions", value = transitions.size.toString(), color = StatusReplay)
            StatItem(label = "Active Penalties", value = penalties.size.toString(), color = StatusEarlySkip)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AccentPink,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentPink
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) TextPrimary else TextMuted
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> TopTracksList(tracks = topTracks)
            1 -> TransitionsList(transitions = transitions)
            2 -> PenaltiesList(penalties = penalties, viewModel = viewModel)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun TopTracksList(tracks: List<TrackEntity>) {
    if (tracks.isEmpty()) {
        EmptyState(message = "No tracks in Personal Library yet.\nPlay music in Apple Music or YouTube Music to start learning!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracks, key = { it.trackKey }) { track ->
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (track.userRating == UserRating.LIKED) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.Favorite, contentDescription = "Liked", tint = Color.Red, modifier = Modifier.size(14.dp))
                                } else if (track.userRating == UserRating.DISLIKED) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.ThumbDown, contentDescription = "Disliked", tint = StatusEarlySkip, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "${track.artist} • ${track.album}",
                                fontSize = 12.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Plays: ${track.totalPlays}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusPlaying
                            )
                            Text(
                                text = "Done: ${track.totalCompletions} | Skips: ${track.totalEarlySkips + track.totalLateSkips}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionsList(transitions: List<TransitionEntity>) {
    if (transitions.isEmpty()) {
        EmptyState(message = "No transitions learned yet.\nComplete or skip songs sequentially to train Markov pairs!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transitions, key = { "${it.fromTrackKey}->${it.toTrackKey}" }) { item ->
                val scoreColor = when {
                    item.transitionScore > 0.3f -> StatusPlaying
                    item.transitionScore < -0.2f -> StatusEarlySkip
                    else -> TextSecondary
                }

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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fromTrackKey,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "to", tint = AccentPink, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.toTrackKey,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Count: ${item.transitionCount} (Success: ${item.successCount}, Skips: ${item.earlySkipCount + item.lateSkipCount})",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(scoreColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format("%+.2f", item.transitionScore),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PenaltiesList(penalties: List<SkipPenaltyEntity>, viewModel: PlayerViewModel) {
    if (penalties.isEmpty()) {
        EmptyState(message = "No active skip penalties.\nWhen you skip a song early, a decaying penalty will appear here.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(penalties, key = { it.trackKey }) { item ->
                val currentPenalty = viewModel.calculateEffectivePenalty(item)
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
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.trackKey,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Half-life: ${item.halfLifeHours}h (Initial: -${item.initialPenalty.toInt()} pts)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("-%.1f pts", currentPenalty),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusEarlySkip
                            )
                            Text(
                                text = "Decaying",
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
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
