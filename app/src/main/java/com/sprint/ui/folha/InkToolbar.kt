package com.sprint.ui.folha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sprint.design.Spacing

@Composable
fun InkToolbar(
    modifier: Modifier = Modifier,
    ink: Color = Color(0xFF151713),
    isErasing: Boolean = false,
    onEraserToggle: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    val base = ink.copy(alpha = 0.35f)
    val active = ink.copy(alpha = 0.80f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onEraserToggle, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.AutoFixNormal, contentDescription = "Borracha",
                tint = if (isErasing) active else base, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onUndo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Undo, contentDescription = "Desfazer",
                tint = base, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onRedo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Redo, contentDescription = "Refazer",
                tint = base, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.CleaningServices, contentDescription = "Limpar",
                tint = base, modifier = Modifier.size(20.dp))
        }
    }
}
