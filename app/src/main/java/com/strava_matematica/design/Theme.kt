package com.strava_matematica.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
        background = FocusColors.SlateBackground,
        surface = FocusColors.SlateBackground,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = FocusColors.WhiteTextPrimary,
        onSurface = FocusColors.WhiteTextPrimary,
    )
    else -> lightColorScheme(
        primary = FocusColors.Progress,
        secondary = FocusColors.Info,
        error = FocusColors.Error,
        background = FocusColors.WhiteBackground,
        surface = FocusColors.WhiteSurface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = FocusColors.WhiteTextPrimary,
        onSurface = FocusColors.WhiteTextPrimary,
    )
}

@Composable
fun StravaMathTheme(
    backgroundMode: BackgroundMode = BackgroundMode.WHITE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = appColorScheme(backgroundMode),
        typography = AppTypography,
        content = content,
    )
}
