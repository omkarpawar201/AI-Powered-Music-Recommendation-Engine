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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.musicengine.mediapoc.ui.components.LibraryScreen
import com.musicengine.mediapoc.ui.components.RecommendationScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    var selectedScreenTab by remember { mutableIntStateOf(0) }

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
        ) {
            // Top Navigation Tab Bar (3 Tabs)
            TabRow(
                selectedTabIndex = selectedScreenTab,
                containerColor = CardBg,
                contentColor = AccentPink,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedScreenTab]),
                        color = AccentPink
                    )
                }
            ) {
                Tab(
                    selected = selectedScreenTab == 0,
                    onClick = { selectedScreenTab = 0 },
                    text = {
                        Text(
                            text = "Now Playing",
                            fontWeight = if (selectedScreenTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedScreenTab == 0) TextPrimary else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedScreenTab == 1,
                    onClick = { selectedScreenTab = 1 },
                    text = {
                        Text(
                            text = "Library & Stats",
                            fontWeight = if (selectedScreenTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedScreenTab == 1) TextPrimary else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedScreenTab == 2,
                    onClick = { selectedScreenTab = 2 },
                    text = {
                        Text(
                            text = "AI Recs",
                            fontWeight = if (selectedScreenTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedScreenTab == 2) TextPrimary else TextMuted
                        )
                    }
                )
            }

            when (selectedScreenTab) {
                0 -> {
                    // Existing POC 1 & POC 2 Controls
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Header Status Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Music Recommendation POC",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isServiceConnected) StatusPlaying else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isServiceConnected) "Active: $activeApp" else "Service Disconnected",
                                        fontSize = 12.sp,
                                        color = if (isServiceConnected) StatusPlaying else TextMuted
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { viewModel.refreshSessions() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = AccentPink)
                                }
                                IconButton(onClick = { viewModel.clearEventLog() }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Clear Logs", tint = TextMuted)
                                }
                            }
                        }

                        // Permission Warnings if needed
                        if (!isMediaPermissionGranted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PermissionRequestCard(
                                title = stringResource(R.string.perm_media_title),
                                description = stringResource(R.string.perm_media_desc),
                                buttonText = stringResource(R.string.perm_media_button),
                                icon = Icons.Default.Settings,
                                onGrantClick = { viewModel.openNotificationListenerSettings() }
                            )
                        }

                        if (isBatteryOptimized) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PermissionRequestCard(
                                title = stringResource(R.string.perm_battery_title),
                                description = stringResource(R.string.perm_battery_desc),
                                buttonText = stringResource(R.string.perm_battery_button),
                                icon = Icons.Default.BatteryAlert,
                                onGrantClick = { viewModel.requestBatteryOptimizationExemption() }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        NowPlayingCard(
                            track = nowPlaying,
                            playbackState = playbackState,
                            activeAppName = activeApp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaybackControlPanel(
                            playbackState = playbackState,
                            activeAppName = activeApp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Live Telemetry Feed (${events.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        EventLogList(
                            events = events,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                1 -> {
                    // Personal Library & Transitions Screen
                    LibraryScreen(viewModel = viewModel)
                }
                2 -> {
                    // Multi-Tier Recommendation Engine & Playground
                    RecommendationScreen(viewModel = viewModel)
                }
            }
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
