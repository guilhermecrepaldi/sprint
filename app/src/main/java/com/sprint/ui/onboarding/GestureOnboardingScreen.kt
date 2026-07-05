package com.sprint.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sprint.design.Spacing

/**
 * Tela de onboarding única — ensina o gesto de "enter" do SPRINT:
 * dois dedos simultâneos em qualquer lugar da tela.
 *
 * Após 2 acertos consecutivos, chama [onComplete].
 * O aluno também pode pular.
 */
@Composable
fun GestureOnboardingScreen(onComplete: () -> Unit) {
    var hits by remember { mutableIntStateOf(0) }
    val needed = 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .twoFingerTapDetector {
                hits++
                if (hits >= needed) onComplete()
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xl),
        ) {
            Text(
                text = "SPRINT",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Dois dedos\njuntos",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Em qualquer lugar da tela.\nÉ tudo que você precisa saber.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(Spacing.xl))

            // Indicador de progresso
            val label = when (hits) {
                0    -> "Tente agora"
                1    -> "Mais uma vez"
                else -> "Perfeito"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            TextButton(onClick = onComplete) {
                Text(
                    "Pular",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
            }
        }
    }
}

/**
 * Detecta dois dedos pousando simultaneamente (no mesmo frame de evento).
 * Não exige movimento — só o toque é suficiente.
 */
private fun Modifier.twoFingerTapDetector(onTwoFinger: () -> Unit): Modifier =
    pointerInput(onTwoFinger) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val newPresses = event.changes.count { it.pressed && !it.previousPressed }
                if (newPresses >= 2) {
                    event.changes.forEach { it.consume() }
                    onTwoFinger()
                }
            }
        }
    }
