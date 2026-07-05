package com.sprint.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprint.viewmodel.SprintNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesTab(
    notes: List<SprintNote>,
    onGoToSprint: () -> Unit,
) {
    SettingsTabScaffold(title = "NOTAS", onGoToSprint = onGoToSprint) {
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "nenhuma nota ainda",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes.sortedByDescending { it.timestamp }) { note ->
                    NoteCard(note)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: SprintNote) {
    val ink = MaterialTheme.colorScheme.onBackground
    val dateStr = remember(note.timestamp) {
        SimpleDateFormat("dd/MM · HH:mm", Locale.getDefault()).format(Date(note.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ink.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Context chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateStr,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = ink.copy(alpha = 0.30f),
            )
            Text(
                text = "·",
                fontSize = 9.sp,
                color = ink.copy(alpha = 0.20f),
            )
            Text(
                text = "Folha ${note.folhaIndex + 1}  Ex ${note.exerciseIndex + 1}",
                fontSize = 9.sp,
                color = ink.copy(alpha = 0.30f),
            )
        }

        // Exercise statement (trimmed)
        Text(
            text = note.exerciseStatement,
            fontSize = 13.sp,
            color = ink.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        // Ink preview — scaled-down render of the strokes
        NoteStrokesPreview(
            strokes = note.strokes,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            inkColor = ink,
        )
    }
}

@Composable
private fun NoteStrokesPreview(
    strokes: List<List<Offset>>,
    modifier: Modifier = Modifier,
    inkColor: androidx.compose.ui.graphics.Color,
) {
    if (strokes.isEmpty() || strokes.all { it.isEmpty() }) return

    Canvas(modifier = modifier) {
        if (strokes.isEmpty()) return@Canvas

        // Find bounding box of all strokes
        val allPts = strokes.flatten()
        if (allPts.isEmpty()) return@Canvas
        val minX = allPts.minOf { it.x }
        val minY = allPts.minOf { it.y }
        val maxX = allPts.maxOf { it.x }
        val maxY = allPts.maxOf { it.y }
        val srcW = (maxX - minX).coerceAtLeast(1f)
        val srcH = (maxY - minY).coerceAtLeast(1f)

        // Scale to fit canvas with padding
        val pad = 4.dp.toPx()
        val scaleX = (size.width - pad * 2) / srcW
        val scaleY = (size.height - pad * 2) / srcH
        val scale = minOf(scaleX, scaleY, 1f)  // never upscale

        fun transform(pt: Offset) = Offset(
            (pt.x - minX) * scale + pad,
            (pt.y - minY) * scale + pad,
        )

        val style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        strokes.forEach { pts ->
            if (pts.size < 2) return@forEach
            drawPath(
                path = Path().apply {
                    val t0 = transform(pts[0])
                    moveTo(t0.x, t0.y)
                    pts.drop(1).forEach { p ->
                        val t = transform(p)
                        lineTo(t.x, t.y)
                    }
                },
                color = inkColor.copy(alpha = 0.65f),
                style = style,
            )
        }
    }
}

