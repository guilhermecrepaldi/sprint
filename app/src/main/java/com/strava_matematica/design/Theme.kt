package com.strava_matematica.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strava_matematica.model.BackgroundMode

fun appColorScheme(mode: BackgroundMode): ColorScheme = when (mode) {
    BackgroundMode.DARK -> darkColorScheme(
        primary = FocusColors.ProgressDark,
        secondary = FocusColors.Info,
        error = FocusColors.Error,
        background = FocusColors.DarkBackground,
        surface = FocusColors.DarkSurface,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = FocusColors.DarkTextPrimary,
        onSurface = FocusColors.DarkTextPrimary,
    )
    BackgroundMode.PARCHMENT -> lightColorScheme(
        primary = FocusColors.Progress,
        secondary = FocusColors.Info,
        error = FocusColors.Error,
        background = FocusColors.ParchmentBackground,
        surface = FocusColors.ParchmentBackground,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = FocusColors.WhiteTextPrimary,
        onSurface = FocusColors.WhiteTextPrimary,
    )
    BackgroundMode.SLATE -> lightColorScheme(
        primary = FocusColors.Progress,
        secondary = FocusColors.Info,
        error = FocusColors.Error,
        background = FocusColors.SlateBackground, // Actually, we'll redefine this to Slate-50 in Color.kt
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = FocusColors.WhiteTextPrimary,
        onSurface = FocusColors.WhiteTextPrimary,
        surfaceVariant = Color(0xFFF8FAFC), // Slate-50
        onSurfaceVariant = Color(0xFF64748B) // Slate-500
    )
    else -> lightColorScheme(
        primary = FocusColors.Progress,
        secondary = FocusColors.Info,
        error = FocusColors.Error,
        background = Color(0xFFF8FAFC), // Slate-50 for high-density modern look
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = FocusColors.WhiteTextPrimary,
        onSurface = FocusColors.WhiteTextPrimary,
        surfaceVariant = Color(0xFFF1F5F9), // Slate-100
        onSurfaceVariant = Color(0xFF64748B) // Slate-500
    )
}

val AppShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun StravaMathTheme(
    backgroundMode: BackgroundMode = BackgroundMode.WHITE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = appColorScheme(backgroundMode),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
