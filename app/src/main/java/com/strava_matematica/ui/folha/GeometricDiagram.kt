package com.strava_matematica.ui.folha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeometricDiagram(
    spec: String,
    ink: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val params = remember(spec) { parseSpec(spec) }
    val type = params["type"] ?: "none"

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        val width = size.width
        val height = size.height

        when (type) {
            "right_triangle" -> {
                // Desenho do Triângulo Retângulo Premium
                val margin = 32.dp.toPx()
                val triangleWidth = width - (margin * 2)
                val triangleHeight = height - (margin * 2)

                // Vértices do Triângulo Retângulo
                val pA = Offset(margin, height - margin) // Canto inferior esquerdo (ângulo agudo)
                val pB = Offset(width - margin, height - margin) // Canto inferior direito (90 graus)
                val pC = Offset(width - margin, margin) // Canto superior direito

                // 1. Linhas do triângulo
                val path = Path().apply {
                    moveTo(pA.x, pA.y)
                    lineTo(pB.x, pB.y)
                    lineTo(pC.x, pC.y)
                    close()
                }
                drawPath(path = path, color = ink, style = Stroke(width = 2.dp.toPx()))

                // 2. Indicador de Ângulo Reto em B
                val rectSize = 10.dp.toPx()
                val angleRectPath = Path().apply {
                    moveTo(pB.x - rectSize, pB.y)
                    lineTo(pB.x - rectSize, pB.y - rectSize)
                    lineTo(pB.x, pB.y - rectSize)
                }
                drawPath(path = angleRectPath, color = ink, style = Stroke(width = 1.2f.dp.toPx()))
                drawCircle(color = ink, radius = 2.dp.toPx(), center = Offset(pB.x - rectSize / 2f, pB.y - rectSize / 2f))

                // 3. Arco de Ângulo Agudo em A (θ)
                val arcRadius = 24.dp.toPx()
                drawArc(
                    color = ink.copy(alpha = 0.5f),
                    startAngle = -45f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(pA.x - arcRadius, pA.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 1.2f.dp.toPx())
                )

                // 4. Desenhar Rótulos / Textos
                val angleLabel = params["angle"] ?: "θ"
                val hypLabel = params["hyp"] ?: ""
                val oppLabel = params["opp"] ?: ""
                val adjLabel = params["adj"] ?: ""

                // Ângulo θ
                val angleText = textMeasurer.measure(
                    text = angleLabel,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink)
                )
                drawText(
                    textLayoutResult = angleText,
                    topLeft = Offset(pA.x + arcRadius + 4.dp.toPx(), pA.y - arcRadius / 1.5f)
                )

                // Cateto Oposto (opp) à direita de BC
                if (oppLabel.isNotBlank()) {
                    val oppText = textMeasurer.measure(
                        text = oppLabel,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
                    )
                    drawText(
                        textLayoutResult = oppText,
                        topLeft = Offset(pB.x + 8.dp.toPx(), (pB.y + pC.y) / 2f - oppText.size.height / 2f)
                    )
                }

                // Cateto Adjacente (adj) abaixo de AB
                if (adjLabel.isNotBlank()) {
                    val adjText = textMeasurer.measure(
                        text = adjLabel,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
                    )
                    drawText(
                        textLayoutResult = adjText,
                        topLeft = Offset((pA.x + pB.x) / 2f - adjText.size.width / 2f, pB.y + 6.dp.toPx())
                    )
                }

                // Hipotenusa (hyp) acima da linha AC
                if (hypLabel.isNotBlank()) {
                    val hypText = textMeasurer.measure(
                        text = hypLabel,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
                    )
                    // Ponto médio da hipotenusa deslocado ligeiramente para cima e para a esquerda
                    drawText(
                        textLayoutResult = hypText,
                        topLeft = Offset((pA.x + pC.x) / 2f - hypText.size.width - 6.dp.toPx(), (pA.y + pC.y) / 2f - hypText.size.height - 4.dp.toPx())
                    )
                }
            }

            "venn_2" -> {
                // Desenho do Diagrama de Venn Didático de 2 Conjuntos
                val r = (height / 2.8f).coerceAtMost(width / 4.5f)
                val centerY = height / 2f
                val centerX = width / 2f
                val offsetDist = r * 0.65f

                val centerA = Offset(centerX - offsetDist, centerY)
                val centerB = Offset(centerX + offsetDist, centerY)

                // 1. Círculos de Venn (Azul sutil e Vermelho sutil dependendo do tema)
                val colorA = Color(0xFF2196F3)
                val colorB = Color(0xFFE91E63)

                drawCircle(color = colorA.copy(alpha = 0.06f), radius = r, center = centerA)
                drawCircle(color = colorA.copy(alpha = 0.25f), radius = r, center = centerA, style = Stroke(width = 1.5f.dp.toPx()))

                drawCircle(color = colorB.copy(alpha = 0.06f), radius = r, center = centerB)
                drawCircle(color = colorB.copy(alpha = 0.25f), radius = r, center = centerB, style = Stroke(width = 1.5f.dp.toPx()))

                // 2. Rótulos dos Conjuntos A e B
                val labelA = params["labelA"] ?: "A"
                val labelB = params["labelB"] ?: "B"

                val textA = textMeasurer.measure(
                    text = labelA,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colorA)
                )
                drawText(textLayoutResult = textA, topLeft = Offset(centerA.x - textA.size.width / 2f, centerA.y - r - textA.size.height - 4.dp.toPx()))

                val textB = textMeasurer.measure(
                    text = labelB,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colorB)
                )
                drawText(textLayoutResult = textB, topLeft = Offset(centerB.x - textB.size.width / 2f, centerB.y - r - textB.size.height - 4.dp.toPx()))

                // 3. Valores internos (Apenas A, Interseção, Apenas B, Fora)
                val valA = params["valA"] ?: ""
                val valInter = params["valInter"] ?: ""
                val valB = params["valB"] ?: ""
                val valOuter = params["valOuter"] ?: ""

                // Apenas A (esquerda)
                if (valA.isNotBlank()) {
                    val txt = textMeasurer.measure(text = valA, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    drawText(textLayoutResult = txt, topLeft = Offset(centerA.x - r * 0.4f - txt.size.width / 2f, centerY - txt.size.height / 2f))
                }

                // Apenas B (direita)
                if (valB.isNotBlank()) {
                    val txt = textMeasurer.measure(text = valB, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    drawText(textLayoutResult = txt, topLeft = Offset(centerB.x + r * 0.4f - txt.size.width / 2f, centerY - txt.size.height / 2f))
                }

                // Interseção (centro)
                if (valInter.isNotBlank()) {
                    val txt = textMeasurer.measure(text = valInter, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ink))
                    drawText(textLayoutResult = txt, topLeft = Offset(centerX - txt.size.width / 2f, centerY - txt.size.height / 2f))
                }

                // Fora dos conjuntos (canto inferior direito)
                if (valOuter.isNotBlank()) {
                    val txt = textMeasurer.measure(text = valOuter, style = TextStyle(fontSize = 13.sp, color = ink.copy(alpha = 0.5f)))
                    drawText(textLayoutResult = txt, topLeft = Offset(width - r * 0.8f - txt.size.width, height - r * 0.5f))
                }
            }

            "cartesian_log" -> {
                // Desenho do Gráfico Logarítmico Premium
                val margin = 24.dp.toPx()
                val axisX = height - margin - 20.dp.toPx() // Eixo X horizontal ligeiramente abaixo do centro
                val axisY = margin + 30.dp.toPx() // Eixo Y vertical na esquerda

                // 1. Eixos Cartesianos
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.5f.dp.toPx())

                // Setas nos eixos
                val arrowSize = 6.dp.toPx()
                val arrowXPath = Path().apply {
                    moveTo(width - margin, axisX)
                    lineTo(width - margin - arrowSize, axisX - arrowSize / 1.5f)
                    lineTo(width - margin - arrowSize, axisX + arrowSize / 1.5f)
                    close()
                }
                drawPath(path = arrowXPath, color = ink.copy(alpha = 0.3f))

                val arrowYPath = Path().apply {
                    moveTo(axisY, margin)
                    lineTo(axisY - arrowSize / 1.5f, margin + arrowSize)
                    lineTo(axisY + arrowSize / 1.5f, margin + arrowSize)
                    close()
                }
                drawPath(path = arrowYPath, color = ink.copy(alpha = 0.3f))

                // Rótulos X e Y
                val labelX = textMeasurer.measure("x", style = TextStyle(fontSize = 11.sp, color = ink.copy(alpha = 0.5f)))
                drawText(textLayoutResult = labelX, topLeft = Offset(width - margin - labelX.size.width, axisX + 4.dp.toPx()))

                val labelY = textMeasurer.measure("y", style = TextStyle(fontSize = 11.sp, color = ink.copy(alpha = 0.5f)))
                drawText(textLayoutResult = labelY, topLeft = Offset(axisY - labelY.size.width - 6.dp.toPx(), margin))

                // 2. Curva Logarítmica y = log(x)
                val curvePath = Path()
                var first = true
                val startX = axisY + 4.dp.toPx()
                val endX = width - margin - 12.dp.toPx()
                val steps = 60

                for (step in 0..steps) {
                    val fraction = step.toFloat() / steps
                    val currentXVisual = startX + fraction * (endX - startX)
                    val xMath = (currentXVisual - axisY) / 45f // Escala matemática de X
                    if (xMath <= 0.05f) continue

                    // f(x) = log2(xMath)
                    val yMath = kotlin.math.log2(xMath)
                    val currentYVisual = axisX - (yMath * 40f) // Escala de Y invertida

                    if (currentYVisual in margin..(height - margin)) {
                        if (first) {
                            curvePath.moveTo(currentXVisual, currentYVisual)
                            first = false
                        } else {
                            curvePath.lineTo(currentXVisual, currentYVisual)
                        }
                    }
                }
                drawPath(path = curvePath, color = Color(0xFF4CAF50), style = Stroke(width = 2.dp.toPx()))

                // Marcador de Interseção x=1
                val oneXVisual = axisY + 45f
                drawCircle(color = Color(0xFF4CAF50), radius = 3.dp.toPx(), center = Offset(oneXVisual, axisX))
                val labelOne = textMeasurer.measure("1", style = TextStyle(fontSize = 11.sp, color = ink.copy(alpha = 0.4f)))
                drawText(textLayoutResult = labelOne, topLeft = Offset(oneXVisual - labelOne.size.width / 2f, axisX + 4.dp.toPx()))

                // 3. Ponto de Projeção Especial (x = pointX, y = pointY)
                val pointXLabel = params["pointX"] ?: ""
                val pointYLabel = params["pointY"] ?: ""

                if (pointXLabel.isNotBlank() && pointYLabel.isNotBlank()) {
                    val pxMath = 4.0f // Ponto arbitrário para plotar graficamente
                    val pyMath = 2.0f // log2(4) = 2

                    val pxVisual = axisY + (pxMath * 45f)
                    val pyVisual = axisX - (pyMath * 40f)

                    // Bolinha do ponto no gráfico
                    drawCircle(color = Color(0xFFE91E63), radius = 4.dp.toPx(), center = Offset(pxVisual, pyVisual))

                    // Linhas pontilhadas até os eixos
                    val dotEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    drawLine(
                        color = ink.copy(alpha = 0.35f),
                        start = Offset(pxVisual, pyVisual),
                        end = Offset(pxVisual, axisX),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dotEffect
                    )
                    drawLine(
                        color = ink.copy(alpha = 0.35f),
                        start = Offset(pxVisual, pyVisual),
                        end = Offset(axisY, pyVisual),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dotEffect
                    )

                    // Texto X do Ponto
                    val txtX = textMeasurer.measure(pointXLabel, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63)))
                    drawText(textLayoutResult = txtX, topLeft = Offset(pxVisual - txtX.size.width / 2f, axisX + 4.dp.toPx()))

                    // Texto Y do Ponto
                    val txtY = textMeasurer.measure(pointYLabel, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63)))
                    drawText(textLayoutResult = txtY, topLeft = Offset(axisY - txtY.size.width - 6.dp.toPx(), pyVisual - txtY.size.height / 2f))
                }
            }
        }
    }
}

private fun parseSpec(spec: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val cleaned = spec.trim().removeSurrounding("[", "]")
    if (!cleaned.startsWith("fig:")) return result
    val content = cleaned.substring(4) // Skip "fig:"
    val parts = content.split(",")
    if (parts.isEmpty()) return result

    result["type"] = parts[0].trim()
    for (i in 1 until parts.size) {
        val entry = parts[i].split("=")
        if (entry.size == 2) {
            result[entry[0].trim()] = entry[1].trim()
        }
    }
    return result
}
