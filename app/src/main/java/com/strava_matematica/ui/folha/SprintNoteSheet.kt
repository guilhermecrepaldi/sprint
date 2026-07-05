package com.strava_matematica.ui.folha

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.strava_matematica.design.FocusColors
import com.strava_matematica.model.BackgroundMode
import com.strava_matematica.model.SessionConfig
import com.strava_matematica.viewmodel.SprintNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen note canvas that opens mid-sprint.
 * Tagged automatically with session context: folhaIndex + exerciseIndex + statement.
 * Pill tap = save and close.
 */
@Composable
fun SprintNoteSheet(
    config: SessionConfig,
    sessionId: String?,
    folhaIndex: Int,
    exerciseIndex: Int,
    exerciseStatement: String,
    onSave: (SprintNote) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = if (config.backgroundMode == BackgroundMode.DARK)
        FocusColors.DarkTextPrimary else FocusColors.WhiteTextPrimary

    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    val timestamp = remember { System.currentTimeMillis() }
    val timeLabel = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Pill — tap to save + close ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clickable {
                        if (strokes.isNotEmpty()) {
                            onSave(
                                SprintNote(
                                    sessionId = sessionId,
                                    folhaIndex = folhaIndex,
                                    exerciseIndex = exerciseIndex,
                                    exerciseStatement = exerciseStatement,
                                    timestamp = timestamp,
                                    strokes = strokes,
                                )
                            )
                        }
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(ink.copy(alpha = 0.18f), RoundedCornerShape(2.dp)),
                )
            }

            // ── Context card — onde essa nota foi escrita ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Folha ${folhaIndex + 1}  ·  Ex ${exerciseIndex + 1}",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = ink.copy(alpha = 0.30f),
                    fontWeight = FontWeight.Normal,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = timeLabel,
                    fontSize = 10.sp,
                    color = ink.copy(alpha = 0.20f),
                )
            }

            // ── Exercise statement (context, read-only) ─────────────────────
            Text(
                text = renderLatex(exerciseStatement),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = ink.copy(alpha = 0.55f),
            )

            Spacer(Modifier.height(8.dp))

            // ── Note canvas ─────────────────────────────────────────────────
            NoteCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                penColor = ink,
                penWidth = 2.dp,
                onStrokesChanged = { strokes = it },
            )

            // ── Hint ─────────────────────────────────────────────────────────
            Text(
                text = "toque no traço acima para salvar",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                fontSize = 10.sp,
                color = ink.copy(alpha = 0.18f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
