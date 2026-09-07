package com.musicengine.mediapoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicengine.mediapoc.ui.components.EventLogList
import com.musicengine.mediapoc.ui.components.NowPlayingCard
import com.musicengine.mediapoc.ui.components.PlaybackControlPanel
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.CardBg
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.MediaPOCTheme
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaPOCTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PlayerViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isMediaPermissionGranted by viewModel.isMediaPermissionGranted.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val events by viewModel.events.collectAsState()
    val activeApp by viewModel.activeApp.collectAsState()
    val isServiceConnected by viewModel.isServiceConnected.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_title),
                        color = TextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshSessions() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_sessions),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isServiceConnected && isMediaPermissionGranted) StatusPlaying else AccentPink)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isServiceConnected && isMediaPermissionGranted -> stringResource(R.string.status_active)
                                !isMediaPermissionGranted -> stringResource(R.string.status_no_permission)
                                else -> stringResource(R.string.status_waiting)
                            },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isMediaPermissionGranted) {
                PermissionRequestCard(
                    title = stringResource(R.string.perm_media_title),
                    description = stringResource(R.string.perm_media_desc),
                    buttonText = stringResource(R.string.perm_media_button),
                    icon = Icons.Default.Settings,
                    onGrantClick = { viewModel.openNotificationListenerSettings() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isBatteryOptimized) {
                PermissionRequestCard(
                    title = stringResource(R.string.perm_battery_title),
                    description = stringResource(R.string.perm_battery_desc),
                    buttonText = stringResource(R.string.perm_battery_button),
                    icon = Icons.Default.BatteryAlert,
                    onGrantClick = { viewModel.requestBatteryOptimizationExemption() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            NowPlayingCard(
                track = nowPlaying,
                playbackState = playbackState,
                activeAppName = activeApp
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlaybackControlPanel(
                playbackState = playbackState,
                activeAppName = activeApp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.event_feed_header, events.size),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (events.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearEventLog() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_log),
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            EventLogList(
                events = events,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PermissionRequestCard(
    title: String,
    description: String,
    buttonText: String,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, AccentPink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
