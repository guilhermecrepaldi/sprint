package com.strava_matematica.ui.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.strava_matematica.design.CanvasColors

private const val MAP_MODE_THRESHOLD      = 0.55f
private const val EXERCISE_MODE_THRESHOLD = 0.80f
private const val MIN_SCALE               = 0.08f
private const val MAX_SCALE               = 1.00f

/**
 * Pinch delta threshold: cumulative scale change below this → 2-finger tap (advance),
 * above this → genuine pinch (zoom).
 */
private const val PINCH_INTENT_THRESHOLD = 0.02f

@Composable
fun ZoomableCanvas(
    isInSession: Boolean,
    onAdvance: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    mapContent: @Composable () -> Unit,
    focusContent: @Composable () -> Unit,
) {
    var scale   by remember { mutableFloatStateOf(MAX_SCALE) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Accumulated zoom factor while fingers are down; reset each gesture cycle.
    // Used by the 2-finger tap detector to distinguish tap from pinch.
    var cumulativeZoomDelta by remember { mutableStateOf(1f) }

    val inMapMode = scale < MAP_MODE_THRESHOLD

    LaunchedEffect(inMapMode) {
        if (isInSession) {
            if (inMapMode) onPause() else onResume()
        }
    }

    val mapAlpha   by animateFloatAsState(if (inMapMode) 1f else 0f, label = "mapAlpha")
    val focusAlpha by animateFloatAsState(if (scale > EXERCISE_MODE_THRESHOLD) 1f else 0f, label = "focusAlpha")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasColors.Background)
            // ── Pinch-to-zoom + pan ──────────────────────────────────────────
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    cumulativeZoomDelta *= zoom
                    scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    // Pan only active while map is visible
                    if (scale < EXERCISE_MODE_THRESHOLD) {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
            // ── 2-finger tap = advance exercise ─────────────────────────────
            .pointerInput(onAdvance) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    cumulativeZoomDelta = 1f   // reset at start of each gesture

                    // Drain events until all fingers lift, tracking max pointer count
                    var maxPointers = 1
                    var allUp = false
                    while (!allUp) {
                        val ev = awaitPointerEvent(PointerEventPass.Main)
                        maxPointers = maxOf(maxPointers, ev.changes.count { it.pressed })
                        allUp = ev.changes.none { it.pressed }
                    }

                    // 2 fingers + no meaningful scale change = tap, not pinch
                    if (maxPointers == 2 &&
                        kotlin.math.abs(cumulativeZoomDelta - 1f) < PINCH_INTENT_THRESHOLD &&
                        !inMapMode
                    ) {
                        onAdvance()
                    }
                    cumulativeZoomDelta = 1f
                }
            },
    ) {
        // ── Map layer — fades in as the user zooms out ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = mapAlpha
                    translationX = offsetX
                    translationY = offsetY
                },
        ) {
            mapContent()
        }

        // ── Focus layer — fades in as the user zooms in ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = focusAlpha },
        ) {
            focusContent()
        }
    }
}
