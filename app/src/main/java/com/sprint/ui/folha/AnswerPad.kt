package com.sprint.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Math answer keypad — numeric pad with operators.
 * Extracted from ExerciseField.kt.
 */
@Composable
fun AnswerPad(
    ink: Color,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keys = listOf(
        "7", "8", "9", "-",
        "4", "5", "6", ".",
        "1", "2", "3", "/",
        "0", "x", "+", "=",
        "(", ")", "del", "clr", "ok"
    )
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .size(width = if (key.length > 1) 38.dp else 30.dp, height = 30.dp)
                    .background(ink.copy(alpha = 0.045f), RoundedCornerShape(5.dp))
                    .clickable { onKey(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key,
                    fontSize = 12.sp,
                    color = ink.copy(alpha = if (key == "ok") 0.62f else 0.46f),
                )
            }
        }
    }
}
