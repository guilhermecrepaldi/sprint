package com.sprint.ui.simulado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprint.model.Folha
import com.sprint.ui.folha.FolhaScreen
import com.sprint.model.SessionConfig
import kotlinx.coroutines.delay

@Composable
fun SimuladoSessionScreen(config: SessionConfig, folha: Folha, onFinish: () -> Unit) {
    val initialMs = if (config.durationLimitMs != null && config.durationLimitMs > 0) config.durationLimitMs else 0
    val remaining = remember { mutableIntStateOf(initialMs) }
    val finished = remember { mutableStateOf(false) }

    LaunchedEffect(remaining.intValue, finished.value) {
        if (finished.value) return@LaunchedEffect
        if (remaining.intValue > 0) {
            delay(1000L)
            remaining.intValue -= 1000
        } else {
            finished.value = true
            onFinish()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Simulado", style = MaterialTheme.typography.titleSmall)
            Text("%02d:%02d".format(remaining.intValue / 60000, (remaining.intValue / 1000) % 60))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            FolhaScreen(
                folha = folha,
                config = config,
                onAdvance = {},
                sessionStartedAtMs = System.currentTimeMillis(),
            )
        }
    }
}
