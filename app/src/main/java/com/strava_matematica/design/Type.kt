package com.strava_matematica.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography().copy(
    headlineMedium = Typography().headlineMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    titleMedium = Typography().titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    labelMedium = Typography().labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, letterSpacing = 1.2.sp)
)
