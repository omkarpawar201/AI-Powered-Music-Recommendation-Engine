package com.musicengine.mediapoc.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.ui.theme.DarkBaseBg
import com.musicengine.mediapoc.ui.theme.rememberArtworkPalette

/**
 * Dynamic Atmospheric Backdrop driven by currently playing album art colors.
 *
 * Renders multiple ultra-soft, continuous ambient color fields with zero geometric edges,
 * smoothly morphing over 1400ms across song changes to bathe the interface in ambient illumination.
 */
@Composable
fun AmbientBackground(
    artModel: Any?,
    modifier: Modifier = Modifier
) {
    val palette = rememberArtworkPalette(artModel)

    // Smooth color morphing animations (1400ms)
    val animPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "ambientPrimary"
    )
    val animSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "ambientSecondary"
    )
    val animTertiary by animateColorAsState(
        targetValue = palette.tertiary,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "ambientTertiary"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Deep Midnight Base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBaseBg)
        )

        // 2. Continuous Ambient Light Field: Top-Left to Bottom-Right Primary Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            animPrimary.copy(alpha = 0.22f),
                            animPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        start = Offset(-200f, -100f),
                        end = Offset(1400f, 1600f)
                    )
                )
        )

        // 3. Continuous Ambient Light Field: Center-Right Secondary Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            animSecondary.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset(600f, 200f),
                        end = Offset(-400f, 1800f)
                    )
                )
        )

        // 4. Continuous Ambient Light Field: Bottom Tertiary Atmosphere
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.80f to animTertiary.copy(alpha = 0.20f),
                        1.0f to animTertiary.copy(alpha = 0.12f)
                    )
                )
        )

        // 5. Hardware-Accelerated Heavy Blur Artwork Layer (API 31+)
        if (artModel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Crossfade(
                targetState = artModel,
                animationSpec = tween(1200, easing = FastOutSlowInEasing),
                label = "ambientArtBlur"
            ) { model ->
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(96.dp)
                        .alpha(0.26f)
                )
            }
        }

        // 6. Atmospheric Contrast Scrim (Smooth, even lighting without artificial bottom black bar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBaseBg.copy(alpha = 0.35f),
                            DarkBaseBg.copy(alpha = 0.10f),
                            DarkBaseBg.copy(alpha = 0.25f),
                            DarkBaseBg.copy(alpha = 0.38f)
                        )
                    )
                )
        )
    }
}