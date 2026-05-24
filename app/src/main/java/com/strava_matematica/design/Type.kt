package com.strava_matematica.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography().copy(
    headlineMedium = Typography().headlineMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 16.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
)
