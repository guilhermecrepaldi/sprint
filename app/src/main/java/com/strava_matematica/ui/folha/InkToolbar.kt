package com.strava_matematica.ui.folha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.strava_matematica.design.Spacing

@Composable
fun InkToolbar(
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = {}) { Icon(Icons.Outlined.Brush, contentDescription = "Caneta") }
        IconButton(onClick = {}) { Icon(Icons.Outlined.CleaningServices, contentDescription = "Borracha") }
        IconButton(onClick = {}) { Icon(Icons.Outlined.Undo, contentDescription = "Desfazer") }
        IconButton(onClick = {}) { Icon(Icons.Outlined.Redo, contentDescription = "Refazer") }
        IconButton(onClick = onClear) { Icon(Icons.Outlined.CleaningServices, contentDescription = "Limpar campo") }
    }
}
