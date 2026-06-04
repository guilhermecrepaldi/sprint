package com.strava_matematica.ui.folha

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MathAnswerTemplate(
    expectedAnswer: String?,
    studentInput: String,
    isCorrect: Boolean,
    ink: androidx.compose.ui.graphics.Color
) {
    val isSet = expectedAnswer?.trim()?.startsWith("{") == true && expectedAnswer.trim().endsWith("}")
    val isVariableX = expectedAnswer?.trim()?.startsWith("x=") == true
    val isVariableY = expectedAnswer?.trim()?.startsWith("y=") == true
    
    val prefix = when {
        isSet -> "S = {"
        isVariableX -> "x = "
        isVariableY -> "y = "
        else -> ""
    }
    
    val suffix = when {
        isSet -> "}"
        else -> ""
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.5f)
            )
        }
        
        Text(
            text = studentInput,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isCorrect -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                else -> androidx.compose.ui.graphics.Color(0xFFC62828)
            }
        )
        
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ink.copy(alpha = 0.5f)
            )
        }
    }
}
