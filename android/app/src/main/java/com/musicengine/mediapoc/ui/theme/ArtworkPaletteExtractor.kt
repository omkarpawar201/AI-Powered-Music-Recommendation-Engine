package com.musicengine.mediapoc.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PaletteColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    companion object {
        val Default = PaletteColors(
            primary = Color(0xFFFA2D48),    // Radiant Pink
            secondary = Color(0xFF00E5FF),  // Cyan
            tertiary = Color(0xFF9C27B0)    // Purple
        )
    }
}

private val paletteCache = LruCache<String, PaletteColors>(30)

@Composable
fun rememberArtworkPalette(artModel: Any?): PaletteColors {
    val context = LocalContext.current
    var paletteColors by remember(artModel) {
        val cacheKey = artModel?.toString() ?: ""
        mutableStateOf(paletteCache.get(cacheKey) ?: PaletteColors.Default)
    }

    LaunchedEffect(artModel) {
        if (artModel == null) {
            paletteColors = PaletteColors.Default
            return@LaunchedEffect
        }

        val cacheKey = artModel.toString()
        val cached = paletteCache.get(cacheKey)
        if (cached != null) {
            paletteColors = cached
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val bitmap = loadBitmap(context, artModel)
            if (bitmap != null && !bitmap.isRecycled) {
                try {
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(16)
                        .generate()

                    val primaryInt = palette.getVibrantColor(
                        palette.getDominantColor(0xFFFA2D48.toInt())
                    )
                    val secondaryInt = palette.getDarkVibrantColor(
                        palette.getMutedColor(0xFF00E5FF.toInt())
                    )
                    val tertiaryInt = palette.getLightVibrantColor(
                        palette.getLightMutedColor(0xFF9C27B0.toInt())
                    )

                    val extracted = PaletteColors(
                        primary = Color(primaryInt),
                        secondary = Color(secondaryInt),
                        tertiary = Color(tertiaryInt)
                    )
                    paletteCache.put(cacheKey, extracted)
                    withContext(Dispatchers.Main) {
                        paletteColors = extracted
                    }
                } catch (_: Exception) {
                    // Fallback to default
                }
            }
        }
    }

    return paletteColors
}

private suspend fun loadBitmap(context: Context, artModel: Any): Bitmap? {
    return when (artModel) {
        is Bitmap -> artModel
        is Uri, is String -> {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artModel)
                    .size(120, 120) // Downsample for ultra-fast color extraction
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    (result.drawable as? BitmapDrawable)?.bitmap
                } else null
            } catch (_: Exception) {
                null
            }
        }
        else -> null
    }
}
