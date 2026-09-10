package com.musicengine.mediapoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicengine.mediapoc.ui.components.AmbientBackground
import com.musicengine.mediapoc.ui.components.LibraryScreen
import com.musicengine.mediapoc.ui.components.NowPlayingScreen
import com.musicengine.mediapoc.ui.components.RecommendationScreen
import com.musicengine.mediapoc.ui.components.SearchScreen
import com.musicengine.mediapoc.ui.glass.GlassButton
import com.musicengine.mediapoc.ui.glass.GlassCard
import com.musicengine.mediapoc.ui.glass.GlassNavigationBar
import com.musicengine.mediapoc.ui.glass.GlassNavItem
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.MatchCyan
import com.musicengine.mediapoc.ui.theme.MediaPOCTheme
import com.musicengine.mediapoc.ui.theme.MetricGold
import com.musicengine.mediapoc.ui.theme.MetricPurple
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import com.musicengine.mediapoc.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
    val nowPlaying by viewModel.nowPlaying.collectAsState()

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

    val navItems = remember {
        listOf(
            GlassNavItem(
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = "Home",
                accent = AccentPink
            ),
            GlassNavItem(
                icon = Icons.Outlined.LibraryMusic,
                selectedIcon = Icons.Filled.LibraryMusic,
                label = "Library",
                accent = MetricPurple
            ),
            GlassNavItem(
                icon = Icons.Filled.AutoAwesome,
                label = "AI Recs",
                accent = MatchCyan
            ),
            GlassNavItem(
                icon = Icons.Outlined.Search,
                selectedIcon = Icons.Filled.Search,
                label = "Search",
                accent = MetricGold
            )
        )
    }

    val artModel = nowPlaying?.let { it.artUri ?: it.artBitmap }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fullscreen dynamic ambient artwork backdrop
        AmbientBackground(
            artModel = artModel,
            modifier = Modifier.fillMaxSize()
        )

        // Screen content safely inset below status bar and flowing full-bleed behind floating bottom bar
        AnimatedContent(
            targetState = selectedScreenTab,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            label = "screen",
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (fadeIn(animationSpec = tween(240)) +
                    slideInHorizontally(animationSpec = spring(dampingRatio = 0.88f, stiffness = 550f)) { 36 * direction } +
                    scaleIn(initialScale = 0.985f, animationSpec = tween(240))) togetherWith
                    (fadeOut(animationSpec = tween(160)) +
                        slideOutHorizontally(animationSpec = tween(160)) { -36 * direction } +
                        scaleOut(targetScale = 0.985f, animationSpec = tween(160)))
            }
        ) { tab ->
            when (tab) {
                0 -> NowPlayingScreen(viewModel = viewModel)
                1 -> LibraryScreen(viewModel = viewModel)
                2 -> RecommendationScreen(viewModel = viewModel)
                else -> SearchScreen(viewModel = viewModel)
            }
        }

        // Floating Glass Navigation Bar (docked at bottom center, above gesture insets)
        GlassNavigationBar(
            items = navItems,
            selectedIndex = selectedScreenTab,
            onSelect = { selectedScreenTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp)
        )
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AccentPink.copy(alpha = 0.14f), androidx.compose.foundation.shape.CircleShape)
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        GlassButton(
            text = buttonText,
            onClick = onGrantClick,
            containerColor = AccentPink,
            contentColor = androidx.compose.ui.graphics.Color.White,
            accentBorder = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        )
    }
}