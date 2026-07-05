package com.sprint.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.sprint.model.GestureConfig
import com.sprint.model.GestureConfig.Companion.ActionLabels
import com.sprint.model.GestureConfig.Companion.AllGestures
import com.sprint.model.GestureConfig.Companion.GestureLabels

private val ActionOrder = listOf(
    GestureConfig.ACTION_ADVANCE,
    GestureConfig.ACTION_GO_SPRINT,
    GestureConfig.ACTION_TOGGLE_ERASER,
    GestureConfig.ACTION_OPEN_NOTE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesTab(
    gestureConfig: GestureConfig = GestureConfig(),
    onGestureChange: (action: String, gesture: String) -> Unit = { _, _ -> },
    onGoToSprint: () -> Unit,
) {
    var editingAction by remember { mutableStateOf<String?>(null) }

    SettingsTabScaffold(title = "GESTOS", onGoToSprint = onGoToSprint) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            ActionOrder.forEachIndexed { i, action ->
                GestureRow(
                    actionLabel = ActionLabels[action] ?: action,
                    gestureLabel = GestureLabels[gestureConfig.gestureFor(action)] ?: "—",
                    onClick = { editingAction = action },
                )
                if (i < ActionOrder.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reset hint
            Text(
                text = "toque na linha para trocar o gesto",
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f),
            )
        }
    }

    // Picker sheet
    editingAction?.let { action ->
        GesturePicker(
            actionLabel = ActionLabels[action] ?: action,
            currentGesture = gestureConfig.gestureFor(action),
            onSelect = { gesture ->
                onGestureChange(action, gesture)
                editingAction = null
            },
            onDismiss = { editingAction = null },
        )
    }
}

@Composable
private fun GestureRow(
    actionLabel: String,
    gestureLabel: String,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = actionLabel,
            fontSize = 14.sp,
            color = ink.copy(alpha = 0.75f),
        )
        Text(
            text = gestureLabel,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = ink.copy(alpha = 0.35f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GesturePicker(
    actionLabel: String,
    currentGesture: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ink = MaterialTheme.colorScheme.onBackground

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = actionLabel.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = ink.copy(alpha = 0.35f),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            AllGestures.forEach { gesture ->
                val isSelected = gesture == currentGesture
                val label = GestureLabels[gesture] ?: gesture

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(gesture) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        color = if (isSelected) ink else ink.copy(alpha = 0.50f),
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(
                                    ink.copy(alpha = 0.10f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "atual",
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = ink.copy(alpha = 0.40f),
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = ink.copy(alpha = 0.06f),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
