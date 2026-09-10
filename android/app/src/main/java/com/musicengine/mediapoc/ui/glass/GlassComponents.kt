package com.musicengine.mediapoc.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicengine.mediapoc.ui.theme.AccentPink
import com.musicengine.mediapoc.ui.theme.DarkBg
import com.musicengine.mediapoc.ui.theme.GlassBorder
import com.musicengine.mediapoc.ui.theme.GlassBorderFaint
import com.musicengine.mediapoc.ui.theme.GlassGlow
import com.musicengine.mediapoc.ui.theme.GlassShadowColor
import com.musicengine.mediapoc.ui.theme.GlassSurface
import com.musicengine.mediapoc.ui.theme.GlassSurfaceStrong
import com.musicengine.mediapoc.ui.theme.GlassSurfaceSubtle
import com.musicengine.mediapoc.ui.theme.NavPillBg
import com.musicengine.mediapoc.ui.theme.ProgressInactive
import com.musicengine.mediapoc.ui.theme.StatusEarlySkip
import com.musicengine.mediapoc.ui.theme.StatusPlaying
import com.musicengine.mediapoc.ui.theme.TextMuted
import com.musicengine.mediapoc.ui.theme.TextPrimary
import com.musicengine.mediapoc.ui.theme.TextSecondary
import kotlin.math.roundToInt

// --- Standard Liquid-Glass Shapes ---
val GlassShapeCard = RoundedCornerShape(24.dp)
val GlassShapePill = RoundedCornerShape(50)
val GlassShapeSheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Applies genuine translucent liquid-glass background and specular hairline border.
 */
fun Modifier.glassSurface(
    shape: Shape = GlassShapeCard,
    surfaceColor: Color = GlassSurface,
    borderColor: Color = GlassBorderFaint,
    topSpecularCatch: Boolean = true
): Modifier {
    val fillBrush = if (topSpecularCatch) {
        Brush.verticalGradient(
            colors = listOf(
                GlassGlow.copy(alpha = 0.16f),
                surfaceColor,
                surfaceColor.copy(alpha = (surfaceColor.alpha * 0.75f).coerceIn(0f, 1f))
            )
        )
    } else {
        Brush.verticalGradient(listOf(surfaceColor, surfaceColor))
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            GlassBorder.copy(alpha = 0.30f),
            borderColor,
            borderColor.copy(alpha = 0.15f)
        )
    )

    return this
        .background(fillBrush, shape)
        .border(1.dp, borderBrush, shape)
}

/**
 * Deep soft ambient shadow for true spatial depth.
 */
fun Modifier.glassShadow(
    shape: Shape = GlassShapeCard,
    elevation: androidx.compose.ui.unit.Dp = 18.dp
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = GlassShadowColor,
    spotColor = GlassShadowColor
)

/**
 * Translucent Liquid-Glass Card with tactile press animations.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapeCard,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    surfaceColor: Color = GlassSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glassCardScale"
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassShadow(shape)
            .glassSurface(shape = shape, surfaceColor = if (isPressed) GlassSurfaceStrong else surfaceColor)
            .then(clickMod)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Frosted Liquid-Glass Button with tactile scale & light response.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    containerColor: Color = GlassSurfaceStrong,
    contentColor: Color = TextPrimary,
    accentBorder: Color = AccentPink.copy(alpha = 0.45f),
    shape: Shape = RoundedCornerShape(18.dp),
    fullWidth: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glassBtnScale"
    )

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .background(if (pressed) containerColor.copy(alpha = 0.9f) else containerColor, shape)
            .border(1.dp, if (pressed) accentBorder.copy(alpha = 0.8f) else accentBorder, shape)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Tactile Frosted Circular Icon Button.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimary,
    containerColor: Color = GlassSurfaceStrong,
    borderColor: Color = GlassBorderFaint
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "glassIconBtnScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (pressed) containerColor.copy(alpha = 0.4f) else containerColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Data Model for Bottom Navigation Items.
 */
data class GlassNavItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val label: String,
    val accent: Color = AccentPink
)

/**
 * Floating iOS-Inspired Liquid-Glass Navigation Bar with Sliding Pill Indicator.
 */
@Composable
fun GlassNavigationBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .glassShadow(shape = GlassShapePill, elevation = 20.dp)
            .glassSurface(
                shape = GlassShapePill,
                surfaceColor = NavPillBg,
                borderColor = GlassBorder.copy(alpha = 0.35f)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / items.size.coerceAtLeast(1)

            // 1. Sliding Luminous Glass Indicator (Spring Physics)
            val indicatorOffset by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
                label = "slidingNavIndicator"
            )

            val activeColor = items.getOrNull(selectedIndex)?.accent ?: AccentPink

            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset { IntOffset(x = (indicatorOffset * tabWidth.toPx()).roundToInt(), y = 0) }
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .clip(GlassShapePill)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.30f),
                                Color(0x22FFFFFF),
                                Color(0x10FFFFFF)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                activeColor.copy(alpha = 0.65f),
                                Color(0x33FFFFFF)
                            )
                        ),
                        GlassShapePill
                    )
            )

            // 2. Tab Items Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val itemAccent = item.accent

                    val iconTint by animateColorAsState(
                        targetValue = if (selected) itemAccent else TextMuted,
                        animationSpec = tween(220),
                        label = "navIconTint"
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) TextPrimary else TextMuted.copy(alpha = 0.8f),
                        animationSpec = tween(220),
                        label = "navLabelColor"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.10f else 1f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                        label = "navIconScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(GlassShapePill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected && item.selectedIcon != null) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Floating Glass Metric Tile (Library Statistics).
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .glassShadow(shape = RoundedCornerShape(20.dp), elevation = 12.dp)
            .glassSurface(
                shape = RoundedCornerShape(20.dp),
                surfaceColor = GlassSurface
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column {
            // Icon with Ambient Colored Glow
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Liquid-Glass Segmented Control with sliding pill.
 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .glassSurface(
                shape = GlassShapePill,
                surfaceColor = GlassSurfaceSubtle,
                borderColor = GlassBorderFaint
            )
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / options.size.coerceAtLeast(1)

            val indicatorOffset by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "segmentedIndicator"
            )

            // Sliding pill
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset { IntOffset(x = (indicatorOffset * tabWidth.toPx()).roundToInt(), y = 0) }
                    .clip(GlassShapePill)
                    .background(GlassSurfaceStrong)
                    .border(1.dp, GlassBorder.copy(alpha = 0.5f), GlassShapePill)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) TextPrimary else TextMuted,
                        label = "segmentedLabel"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(GlassShapePill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dual Artwork Transition Card (Song A ➔ Song B).
 */
@Composable
fun DualArtworkTransitionCard(
    fromTitle: String,
    fromArtist: String,
    fromArtworkUri: String?,
    toTitle: String,
    toArtist: String,
    toArtworkUri: String?,
    score: Float,
    count: Int,
    modifier: Modifier = Modifier
) {
    val isPositive = score >= 0f
    val scoreColor = if (isPositive) StatusPlaying else StatusEarlySkip
    val scoreText = if (isPositive) "+${String.format("%.2f", score)}" else String.format("%.2f", score)
    val progressNorm = ((score + 1f) / 2f).coerceIn(0f, 1f)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // From Track
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurfaceStrong)
                        .border(1.dp, GlassBorderFaint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!fromArtworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = fromArtworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fromTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fromArtist,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Connection Arrow with subtle glowing circle
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceSubtle)
                    .border(1.dp, GlassBorderFaint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "flows to",
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }

            // To Track
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurfaceStrong)
                        .border(1.dp, GlassBorderFaint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!toArtworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = toArtworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = toTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = toArtist,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Bar and Score & Count details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Tag
            Text(
                text = "Flow Score: $scoreText",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )

            // Probability Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .padding(horizontal = 10.dp)
                    .clip(GlassShapePill)
                    .background(ProgressInactive)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressNorm)
                        .height(4.dp)
                        .clip(GlassShapePill)
                        .background(scoreColor)
                )
            }

            // Count Badge
            Text(
                text = "$count transitions",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

/**
 * Modern translucent Glass Chip for filtering, presets, and category selections.
 */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color = AccentPink,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "chipScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedTint.copy(alpha = 0.20f) else GlassSurfaceStrong,
        animationSpec = tween(220),
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedTint.copy(alpha = 0.55f) else GlassBorderFaint,
        animationSpec = tween(220),
        label = "chipBorder"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) (if (selectedTint == AccentPink) TextPrimary else selectedTint) else TextSecondary,
        animationSpec = tween(220),
        label = "chipContent"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(GlassShapePill)
            .background(bgColor)
            .border(1.dp, borderColor, GlassShapePill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(15.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Ultra-sleek Liquid Glass Dropdown Menu popup container.
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavPillBg,
                        DarkBg.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        GlassBorder.copy(alpha = 0.45f),
                        GlassBorderFaint.copy(alpha = 0.20f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        content = content
    )
}

/**
 * Styled Glass Dropdown Menu Item with soft pill icon containers and micro-interactions.
 */
@Composable
fun GlassDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = AccentPink,
    textColor: Color = TextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.12f else 0.0f,
        animationSpec = tween(150),
        label = "itemBgAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TextPrimary.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}