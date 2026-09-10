package com.musicengine.mediapoc.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicengine.mediapoc.model.TelemetryEvent
import com.musicengine.mediapoc.model.TelemetryEventType
import com.musicengine.mediapoc.ui.glass.GlassCard
import com.musicengine.mediapoc.ui.glass.GlassShapeCard
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.StatusCompleted
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusLateSkip
import com.musicengine.mediapoc.ui.theme.StatusPaused
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.StatusReplay
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary

@Composable
fun EventLogList(
    events: List<TelemetryEvent>,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
                .clip(GlassShapeCard)
                .background(GlassSurfaceSubtle)
                .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f), GlassShapeCard)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No playback events detected yet.\nStart playing a song in Apple Music or YouTube Music.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events, key = { it.id }) { event ->
                EventItemCard(event)
            }
        }
    }
}

@Composable
private fun EventItemCard(event: TelemetryEvent) {
    val (badgeColor, badgeLabel) = when (event.type) {
        TelemetryEventType.NATURAL_COMPLETION -> StatusCompleted to "COMPLETED"
        TelemetryEventType.SKIP_EARLY -> StatusEarlySkip to "EARLY SKIP"
        TelemetryEventType.SKIP_LATE -> StatusLateSkip to "LATE SKIP"
        TelemetryEventType.REPLAY -> StatusReplay to "REPLAY"
        TelemetryEventType.TRACK_STARTED -> StatusPlaying to "STARTED"
        TelemetryEventType.PAUSED -> StatusPaused to "PAUSED"
        TelemetryEventType.RESUMED -> StatusPlaying to "RESUMED"
        TelemetryEventType.SEEKED -> StatusLateSkip to "SEEKED"
        TelemetryEventType.USER_LIKE -> AccentPink to "LIKED"
        TelemetryEventType.USER_DISLIKE -> StatusEarlySkip to "DISLIKED"
    }

    GlassCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = badgeLabel,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = event.timestamp,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = event.description,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 17.sp
        )
    }
}