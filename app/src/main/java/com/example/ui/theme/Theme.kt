package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val GlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun TakaFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (isDark) DarkGlassPalette else LightGlassPalette
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            background = palette.bgTop,
            onBackground = palette.ink,
            surface = palette.glassSurface,
            onSurface = palette.ink,
            surfaceVariant = palette.glassChip,
            onSurfaceVariant = palette.subInk,
            error = palette.expense,
            onError = palette.onExpense,
            outline = palette.glassBorderTop,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            secondary = palette.secondary,
            background = palette.bgTop,
            onBackground = palette.ink,
            surface = palette.glassSurface,
            onSurface = palette.ink,
            surfaceVariant = palette.glassChip,
            onSurfaceVariant = palette.subInk,
            error = palette.expense,
            onError = palette.onExpense,
            outline = palette.glassBorderBottom,
        )
    }

    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GlassShapes,
            content = content,
        )
    }
}
