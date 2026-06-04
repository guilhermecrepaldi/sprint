package com.strava_matematica.ui.folha

import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class CaptureController {
    internal var captureAction: (() -> Unit)? = null

    fun capture() {
        captureAction?.invoke()
    }
}

@Composable
fun rememberCaptureController(): CaptureController {
    return remember { CaptureController() }
}

@Composable
fun Modifier.graphicsLayerCapture(
    controller: CaptureController,
    onCapture: (ImageBitmap) -> Unit
): Modifier {
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()

    controller.captureAction = {
        coroutineScope.launch {
            val bitmap = graphicsLayer.toImageBitmap()
            onCapture(bitmap)
        }
    }

    return this.drawWithContent {
        graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(graphicsLayer)
    }
}

@Composable
fun CaptureButton(controller: CaptureController, onCaptureClick: () -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        controller.capture()
        onCaptureClick()
        Toast.makeText(context, "Rascunho salvo!", Toast.LENGTH_SHORT).show()
    }) {
        Text("📸 Salvar Rascunho")
    }
}
