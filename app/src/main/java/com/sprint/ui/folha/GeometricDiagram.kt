package com.sprint.ui.folha

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

            "trig_unit_circle" -> {
                val radius = (minOf(width, height) / 2f) * 0.8f
                val cx = width / 2f
                val cy = height / 2f

                // Eixos X e Y
                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(cx - radius * 1.2f, cy), end = Offset(cx + radius * 1.2f, cy), strokeWidth = 1.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(cx, cy - radius * 1.2f), end = Offset(cx, cy + radius * 1.2f), strokeWidth = 1.dp.toPx())

                // Círculo
                drawCircle(color = ink.copy(alpha = 0.6f), radius = radius, center = Offset(cx, cy), style = Stroke(width = 1.5f.dp.toPx()))

                // Raio e Ângulo
                val angleDeg = params["angle"]?.toFloatOrNull() ?: 45f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val endX = cx + (radius * Math.cos(angleRad)).toFloat()
                val endY = cy - (radius * Math.sin(angleRad)).toFloat() // Y is inverted in Canvas

                drawLine(color = Color(0xFF2196F3), start = Offset(cx, cy), end = Offset(endX, endY), strokeWidth = 2.dp.toPx())

                // Projeções (Seno/Cosseno)
                val showProj = params["show_proj"] == "true"
                if (showProj) {
                    val dotEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    // Seno (Vertical)
                    drawLine(color = Color(0xFFE91E63), start = Offset(endX, endY), end = Offset(endX, cy), strokeWidth = 1.5f.dp.toPx(), pathEffect = dotEffect)
                    // Cosseno (Horizontal)
                    drawLine(color = Color(0xFF4CAF50), start = Offset(endX, endY), end = Offset(cx, endY), strokeWidth = 1.5f.dp.toPx(), pathEffect = dotEffect)
                }

                // Arco do ângulo
                val arcRadius = radius * 0.25f
                drawArc(
                    color = ink.copy(alpha = 0.8f),
                    startAngle = 0f,
                    sweepAngle = -angleDeg,
                    useCenter = false,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 1.5f.dp.toPx())
                )

                // Rótulo do ângulo
                val angleLabel = params["label"] ?: "${angleDeg.toInt()}°"
                val labelText = textMeasurer.measure(angleLabel, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ink))
                drawText(textLayoutResult = labelText, topLeft = Offset(cx + arcRadius * 1.2f, cy - arcRadius * 1.2f - labelText.size.height / 2f))
            }

            "triangle_generic" -> {
                val margin = 32.dp.toPx()
                val tw = width - margin * 2
                val th = height - margin * 2

                val angleA = params["A"]?.toDoubleOrNull() ?: 60.0
                val angleB = params["B"]?.toDoubleOrNull() ?: 60.0
                val angleC = 180.0 - angleA - angleB

                val pA = Offset(margin, height - margin)
                val pB = Offset(width - margin, height - margin)
                
                val sideC = width - margin * 2
                val radA = Math.toRadians(angleA)
                val radB = Math.toRadians(angleB)
                val radC = Math.toRadians(angleC)

                val sideB = (sideC * Math.sin(radB)) / Math.sin(radC)
                
                val pC_x = margin + (sideB * Math.cos(radA)).toFloat()
                val pC_y = (height - margin) - (sideB * Math.sin(radA)).toFloat()

                val pC = Offset(pC_x, pC_y)

                val path = Path().apply {
                    moveTo(pA.x, pA.y)
                    lineTo(pB.x, pB.y)
                    lineTo(pC.x, pC.y)
                    close()
                }
                drawPath(path = path, color = ink, style = Stroke(width = 2.dp.toPx()))

                val aLabel = params["a_label"] ?: ""
                val bLabel = params["b_label"] ?: ""
                val cLabel = params["c_label"] ?: ""

                if (aLabel.isNotBlank()) {
                    val txt = textMeasurer.measure(aLabel, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    drawText(txt, topLeft = Offset((pB.x + pC.x)/2f + 4f, (pB.y + pC.y)/2f - txt.size.height/2f))
                }
                if (bLabel.isNotBlank()) {
                    val txt = textMeasurer.measure(bLabel, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    drawText(txt, topLeft = Offset((pA.x + pC.x)/2f - txt.size.width - 4f, (pA.y + pC.y)/2f - txt.size.height/2f))
                }
                if (cLabel.isNotBlank()) {
                    val txt = textMeasurer.measure(cLabel, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    drawText(txt, topLeft = Offset((pA.x + pB.x)/2f - txt.size.width/2f, pB.y + 4f))
                }
            }

            "trig_wave" -> {
                val func = params["func"] ?: "sin"
                val amp = params["amp"]?.toFloatOrNull() ?: 1f
                val freq = params["freq"]?.toFloatOrNull() ?: 1f
                val shift = params["shift"]?.toFloatOrNull() ?: 0f

                val margin = 24.dp.toPx()
                val axisX = height / 2f
                val axisY = margin + 20.dp.toPx()

                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.dp.toPx())

                val curvePath = Path()
                var first = true
                val startX = axisY
                val endX = width - margin

                for (xPx in startX.toInt()..endX.toInt()) {
                    val xMath = (xPx - axisY) / 40f
                    val angle = (xMath * freq) + shift
                    val yMath = if (func == "cos") Math.cos(angle.toDouble()).toFloat() else Math.sin(angle.toDouble()).toFloat()
                    val yPx = axisX - (yMath * amp * 40f)

                    if (yPx in margin..(height - margin)) {
                        if (first) {
                            curvePath.moveTo(xPx.toFloat(), yPx)
                            first = false
                        } else {
                            curvePath.lineTo(xPx.toFloat(), yPx)
                        }
                    }
                }
                drawPath(path = curvePath, color = Color(0xFFE91E63), style = Stroke(width = 2.dp.toPx()))
            }

            "vector_2d" -> {
                val margin = 32.dp.toPx()
                val cx = width / 2f
                val cy = height / 2f
                
                // Eixos
                drawLine(color = ink.copy(alpha = 0.2f), start = Offset(0f, cy), end = Offset(width, cy), strokeWidth = 1.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.2f), start = Offset(cx, 0f), end = Offset(cx, height), strokeWidth = 1.dp.toPx())
                
                val scale = 20f
                val vx = params["vx"]?.toFloatOrNull() ?: 3f
                val vy = params["vy"]?.toFloatOrNull() ?: 2f
                val ux = params["ux"]?.toFloatOrNull() ?: -2f
                val uy = params["uy"]?.toFloatOrNull() ?: 1f
                
                val endVx = cx + vx * scale
                val endVy = cy - vy * scale
                val endUx = cx + ux * scale
                val endUy = cy - uy * scale

                // Vector v (Blue)
                drawLine(color = Color(0xFF2196F3), start = Offset(cx, cy), end = Offset(endVx, endVy), strokeWidth = 2.5f.dp.toPx())
                drawCircle(color = Color(0xFF2196F3), radius = 4.dp.toPx(), center = Offset(endVx, endVy))
                val vLabel = textMeasurer.measure("v", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3)))
                drawText(vLabel, topLeft = Offset(endVx + 4f, endVy - vLabel.size.height))

                // Vector u (Red)
                drawLine(color = Color(0xFFE91E63), start = Offset(cx, cy), end = Offset(endUx, endUy), strokeWidth = 2.5f.dp.toPx())
                drawCircle(color = Color(0xFFE91E63), radius = 4.dp.toPx(), center = Offset(endUx, endUy))
                val uLabel = textMeasurer.measure("u", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63)))
                drawText(uLabel, topLeft = Offset(endUx + 4f, endUy - uLabel.size.height))
            }

            "area_under_curve" -> {
                val margin = 24.dp.toPx()
                val axisX = height - margin - 20.dp.toPx()
                val axisY = margin + 30.dp.toPx()

                // Eixos
                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.3f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.dp.toPx())

                val curvePath = Path()
                val fillPath = Path()
                
                var first = true
                val startX = axisY
                val endX = width - margin - 20.dp.toPx()
                
                val lowerBound = params["a"]?.toFloatOrNull() ?: 1f
                val upperBound = params["b"]?.toFloatOrNull() ?: 3f
                
                fillPath.moveTo(axisY + lowerBound * 40f, axisX)

                for (xPx in startX.toInt()..endX.toInt()) {
                    val xMath = (xPx - axisY) / 40f
                    // Exemplo: curva y = 0.5 * x^2 + 1
                    val yMath = 0.5f * xMath * xMath + 1f
                    val yPx = axisX - (yMath * 20f)

                    if (yPx in margin..(height - margin)) {
                        if (first) {
                            curvePath.moveTo(xPx.toFloat(), yPx)
                            first = false
                        } else {
                            curvePath.lineTo(xPx.toFloat(), yPx)
                        }
                        
                        // Adicionar ponto ao preenchimento se estiver no intervalo da integral
                        if (xMath >= lowerBound && xMath <= upperBound) {
                            fillPath.lineTo(xPx.toFloat(), yPx)
                        }
                    }
                }
                
                // Desenhar a curva da função
                drawPath(path = curvePath, color = Color(0xFF9C27B0), style = Stroke(width = 2.dp.toPx()))
                
                // Fechar o polígono de preenchimento e pintar a área da integral
                val lastXMath = minOf((endX - axisY) / 40f, upperBound)
                val lastYMath = 0.5f * lastXMath * lastXMath + 1f
                val lastYPx = axisX - (lastYMath * 20f)
                
                fillPath.lineTo(axisY + lastXMath * 40f, axisX)
                fillPath.close()
                
                drawPath(path = fillPath, color = Color(0xFF9C27B0).copy(alpha = 0.2f))
                
                // Delimitadores a e b
                val dotEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                val aPx = axisY + lowerBound * 40f
                val bPx = axisY + upperBound * 40f
                drawLine(color = Color(0xFF9C27B0), start = Offset(aPx, axisX), end = Offset(aPx, axisX - (0.5f*lowerBound*lowerBound+1f)*20f), pathEffect = dotEffect)
                drawLine(color = Color(0xFF9C27B0), start = Offset(bPx, axisX), end = Offset(bPx, axisX - (0.5f*upperBound*upperBound+1f)*20f), pathEffect = dotEffect)
                
                // Textos a e b
                val aText = textMeasurer.measure("a", style = TextStyle(fontSize = 12.sp, color = ink))
                drawText(aText, topLeft = Offset(aPx - aText.size.width/2f, axisX + 4f))
                val bText = textMeasurer.measure("b", style = TextStyle(fontSize = 12.sp, color = ink))
                drawText(bText, topLeft = Offset(bPx - bText.size.width/2f, axisX + 4f))
            }

            "math_parabola" -> {
                val margin = 24.dp.toPx()
                val axisX = height - margin - 30.dp.toPx()
                val axisY = width / 2f
                
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.5f.dp.toPx())
                
                val aVal = params["a"]?.toFloatOrNull() ?: 1f
                val bVal = params["b"]?.toFloatOrNull() ?: 0f
                val cVal = params["c"]?.toFloatOrNull() ?: -2f
                
                val curvePath = Path()
                var first = true
                val steps = 60
                
                for (step in 0..steps) {
                    val fraction = step.toFloat() / steps
                    val currentXVisual = margin + fraction * (width - 2 * margin)
                    val xMath = (currentXVisual - axisY) / 30f
                    
                    val yMath = aVal * (xMath * xMath) + bVal * xMath + cVal
                    val currentYVisual = axisX - (yMath * 30f)
                    
                    if (currentYVisual in (margin - 10f)..(height - margin + 10f)) {
                        if (first) {
                            curvePath.moveTo(currentXVisual, currentYVisual)
                            first = false
                        } else {
                            curvePath.lineTo(currentXVisual, currentYVisual)
                        }
                    } else {
                        first = true
                    }
                }
                drawPath(path = curvePath, color = Color(0xFF2196F3), style = Stroke(width = 2.dp.toPx()))
                
                // Vértice
                val xv = -bVal / (2 * aVal)
                val yv = aVal * xv * xv + bVal * xv + cVal
                drawCircle(color = Color(0xFFE91E63), radius = 4.dp.toPx(), center = Offset(axisY + xv * 30f, axisX - yv * 30f))
            }
            
            "math_line" -> {
                val margin = 24.dp.toPx()
                val axisX = height / 2f
                val axisY = width / 2f
                
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.5f.dp.toPx())
                
                val aVal = params["a"]?.toFloatOrNull() ?: 1f
                val bVal = params["b"]?.toFloatOrNull() ?: 0f
                
                val x1Math = -10f
                val y1Math = aVal * x1Math + bVal
                val x2Math = 10f
                val y2Math = aVal * x2Math + bVal
                
                val p1 = Offset(axisY + x1Math * 30f, axisX - y1Math * 30f)
                val p2 = Offset(axisY + x2Math * 30f, axisX - y2Math * 30f)
                
                drawLine(color = Color(0xFFFF9800), start = p1, end = p2, strokeWidth = 2.dp.toPx())
            }
            
            "math_exponential" -> {
                val margin = 24.dp.toPx()
                val axisX = height - margin - 20.dp.toPx()
                val axisY = width / 2f
                
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.5f.dp.toPx())
                
                val baseVal = params["base"]?.toFloatOrNull() ?: 2f
                
                val curvePath = Path()
                var first = true
                val steps = 60
                
                for (step in 0..steps) {
                    val fraction = step.toFloat() / steps
                    val currentXVisual = margin + fraction * (width - 2 * margin)
                    val xMath = (currentXVisual - axisY) / 30f
                    
                    val yMath = Math.pow(baseVal.toDouble(), xMath.toDouble()).toFloat()
                    val currentYVisual = axisX - (yMath * 30f)
                    
                    if (currentYVisual in (margin - 10f)..(height - margin + 10f)) {
                        if (first) {
                            curvePath.moveTo(currentXVisual, currentYVisual)
                            first = false
                        } else {
                            curvePath.lineTo(currentXVisual, currentYVisual)
                        }
                    } else {
                        first = true
                    }
                }
                drawPath(path = curvePath, color = Color(0xFFF44336), style = Stroke(width = 2.dp.toPx()))
            }
            
            "math_absolute" -> {
                val margin = 24.dp.toPx()
                val axisX = height - margin - 20.dp.toPx()
                val axisY = width / 2f
                
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(margin, axisX), end = Offset(width - margin, axisX), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink.copy(alpha = 0.25f), start = Offset(axisY, margin), end = Offset(axisY, height - margin), strokeWidth = 1.5f.dp.toPx())
                
                val curvePath = Path()
                var first = true
                val steps = 60
                
                for (step in 0..steps) {
                    val fraction = step.toFloat() / steps
                    val currentXVisual = margin + fraction * (width - 2 * margin)
                    val xMath = (currentXVisual - axisY) / 30f
                    
                    val yMath = kotlin.math.abs(xMath)
                    val currentYVisual = axisX - (yMath * 30f)
                    
                    if (first) {
                        curvePath.moveTo(currentXVisual, currentYVisual)
                        first = false
                    } else {
                        curvePath.lineTo(currentXVisual, currentYVisual)
                    }
                }
                drawPath(path = curvePath, color = ink, style = Stroke(width = 2.dp.toPx()))
            }
            
            "cube" -> {
                val margin = 32.dp.toPx()
                val sizeVal = minOf(width, height) - margin * 2
                val L = sizeVal * 0.7f
                val offset = sizeVal * 0.3f
                
                // Posições base
                val startX = margin
                val startY = margin + offset
                
                // Face frontal
                val fTL = Offset(startX, startY)
                val fTR = Offset(startX + L, startY)
                val fBL = Offset(startX, startY + L)
                val fBR = Offset(startX + L, startY + L)
                
                // Face traseira
                val bTL = Offset(startX + offset, startY - offset)
                val bTR = Offset(startX + L + offset, startY - offset)
                val bBL = Offset(startX + offset, startY + L - offset)
                val bBR = Offset(startX + L + offset, startY + L - offset)
                
                val path = Path().apply {
                    // Face traseira
                    moveTo(bTL.x, bTL.y)
                    lineTo(bTR.x, bTR.y)
                    lineTo(bBR.x, bBR.y)
                    lineTo(bBL.x, bBL.y)
                    close()
                    
                    // Face frontal
                    moveTo(fTL.x, fTL.y)
                    lineTo(fTR.x, fTR.y)
                    lineTo(fBR.x, fBR.y)
                    lineTo(fBL.x, fBL.y)
                    close()
                    
                    // Conexões
                    moveTo(fTL.x, fTL.y); lineTo(bTL.x, bTL.y)
                    moveTo(fTR.x, fTR.y); lineTo(bTR.x, bTR.y)
                    moveTo(fBR.x, fBR.y); lineTo(bBR.x, bBR.y)
                    moveTo(fBL.x, fBL.y); lineTo(bBL.x, bBL.y)
                }
                
                drawPath(path = path, color = ink, style = Stroke(width = 2.dp.toPx()))
                
                val edgeLabel = params["edge"] ?: ""
                if (edgeLabel.isNotBlank()) {
                    val txt = textMeasurer.measure(edgeLabel, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                    // Rotular a aresta inferior da face frontal
                    drawText(txt, topLeft = Offset(fBL.x + L / 2f - txt.size.width / 2f, fBL.y + 4.dp.toPx()))
                }
            }

            "parallel_lines" -> {
                val margin = 24.dp.toPx()
                val gap = (params["gap"]?.toFloatOrNull() ?: 40f) / 100f * height * 0.5f
                val angleDeg = params["angle"]?.toFloatOrNull() ?: 60f
                val pair = params["pair"] ?: "none"
                val showAll = params["show_all"] == "true"

                val y1 = height / 2f - gap / 2f  // parallel line r
                val y2 = height / 2f + gap / 2f  // parallel line s
                val transLen = height * 0.7f

                // Transversal start/end points
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dx = (transLen * Math.cos(angleRad)).toFloat()
                val dy = (transLen * Math.sin(angleRad)).toFloat()
                val tx = width / 2f
                val ty = height / 2f
                val tStart = Offset(tx - dx / 2f, ty - dy / 2f)
                val tEnd = Offset(tx + dx / 2f, ty + dy / 2f)

                // Draw parallel lines
                val lineLength = width * 0.8f
                val lineStart = (width - lineLength) / 2f
                val lineEnd = lineStart + lineLength
                drawLine(color = ink, start = Offset(lineStart, y1), end = Offset(lineEnd, y1), strokeWidth = 2.dp.toPx())
                drawLine(color = ink, start = Offset(lineStart, y2), end = Offset(lineEnd, y2), strokeWidth = 2.dp.toPx())

                // Arrow tips for parallel lines
                val arrSize = 6.dp.toPx()
                drawLine(color = ink, start = Offset(lineEnd + arrSize, y1 - arrSize), end = Offset(lineEnd, y1), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink, start = Offset(lineEnd + arrSize, y1 + arrSize), end = Offset(lineEnd, y1), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink, start = Offset(lineEnd + arrSize, y2 - arrSize), end = Offset(lineEnd, y2), strokeWidth = 1.5f.dp.toPx())
                drawLine(color = ink, start = Offset(lineEnd + arrSize, y2 + arrSize), end = Offset(lineEnd, y2), strokeWidth = 1.5f.dp.toPx())

                // Labels for lines r and s
                val rLabel = textMeasurer.measure("r", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                drawText(rLabel, topLeft = Offset(lineStart - rLabel.size.width - 6.dp.toPx(), y1 - rLabel.size.height / 2f))
                val sLabel = textMeasurer.measure("s", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
                drawText(sLabel, topLeft = Offset(lineStart - sLabel.size.width - 6.dp.toPx(), y2 - sLabel.size.height / 2f))

                // Draw transversal
                drawLine(color = ink.copy(alpha = 0.8f), start = tStart, end = tEnd, strokeWidth = 2.dp.toPx())

                // Calculate intersection points
                fun intersect(y: Float): Offset {
                    val t = (y - tStart.y) / (tEnd.y - tStart.y)
                    val ix = tStart.x + t * (tEnd.x - tStart.x)
                    return Offset(ix, y)
                }

                val int1 = intersect(y1)
                val int2 = intersect(y2)

                // Arc helper: draw angle arc at a vertex
                fun drawAngleArc(center: Offset, startAngle: Double, sweepAngle: Double, radius: Float, label: String, color: androidx.compose.ui.graphics.Color) {
                    val startX = center.x + radius * Math.cos(startAngle).toFloat()
                    val startY = center.y + radius * Math.sin(startAngle).toFloat()
                    val endX = center.x + radius * Math.cos(startAngle + sweepAngle).toFloat()
                    val endY = center.y + radius * Math.sin(startAngle + sweepAngle).toFloat()
                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(startX, startY)
                        moveTo(startX, startY)
                        // Simple arc approximation
                        val steps = 20
                        for (s in 0..steps) {
                            val a = startAngle + sweepAngle * s / steps
                            val px = center.x + radius * Math.cos(a).toFloat()
                            val py = center.y + radius * Math.sin(a).toFloat()
                            if (s == 0) moveTo(px, py) else lineTo(px, py)
                        }
                    }
                    drawPath(path = path, color = color, style = Stroke(width = 1.5f.dp.toPx()))

                    // Label at midpoint of arc
                    val midAngle = startAngle + sweepAngle / 2
                    val labelRadius = radius + 12.dp.toPx()
                    val lx = center.x + labelRadius * Math.cos(midAngle).toFloat()
                    val ly = center.y + labelRadius * Math.sin(midAngle).toFloat()
                    val txt = textMeasurer.measure(label, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color))
                    drawText(txt, topLeft = Offset(lx - txt.size.width / 2f, ly - txt.size.height / 2f))
                }

                // Calculate angles for the transversal
                val transAngle = Math.toRadians(angleDeg.toDouble())
                // At the upper intersection (int1), the transversal creates angles:
                // If we measure from the positive x direction
                val sweep = 0.5  // arc sweep in radians
                val arcRadius = 18.dp.toPx()

                // Angle 1 (upper-left, acute): between transversal and parallel line (from left)
                val a1 = Math.PI - transAngle
                val a2 = transAngle
                val a3 = Math.PI - transAngle
                val a4 = transAngle

                val highlightColor = Color(0xFF2196F3)  // blue for highlighted
                val normalColor = ink.copy(alpha = 0.45f)

                fun highlightIf(label: String, isHighlighted: Boolean): androidx.compose.ui.graphics.Color {
                    return if (isHighlighted) highlightColor else normalColor
                }

                val highlightMode = pair  // "corresponding", "alternate_int", etc.

                // Check if a specific angle pair should be highlighted
                fun isHighlighted(angleNum: Int): Boolean {
                    return when (highlightMode) {
                        "corresponding" -> angleNum in listOf(1, 2)  // upper 1 and lower corresponding
                        "alternate_int" -> angleNum in listOf(3, 4)
                        "alternate_ext" -> angleNum in listOf(1, 2)
                        "collateral_int" -> angleNum in listOf(2, 3)
                        "collateral_ext" -> angleNum in listOf(1, 4)
                        else -> false
                    }
                }

                // At upper intersection: draw angle arcs
                if (showAll || highlightMode != "none") {
                    // Angle 1: between line (to right) and transversal (going up-right)
                    val arcAngles = listOf(
                        Pair(0.0, transAngle) to "1",     // between parallel (right) and transversal
                        Pair(transAngle, Math.PI - transAngle) to "2", // between transversal and parallel (left-up)
                        Pair(Math.PI - transAngle, Math.PI) to "3",
                        Pair(Math.PI, transAngle + Math.PI) to "4",
                    )
                    for ((angleRange, num) in arcAngles) {
                        val (startA, endA) = angleRange
                        if (endA > startA) {
                            drawAngleArc(int1, startA, endA - startA, arcRadius, num,
                                if (isHighlighted(num.toInt())) highlightColor else normalColor)
                        }
                    }

                    // At lower intersection, same angles mirrored
                    for ((angleRange, num) in arcAngles) {
                        val (startA, endA) = angleRange
                        if (endA > startA) {
                            drawAngleArc(int2, Math.PI + startA, endA - startA, arcRadius, 
                                "${num.toInt() + 4}", normalColor)
                        }
                    }
                }

                // Draw angle markers on the parallel lines (small marks indicating parallel)
                val markCount = 2
                val markSpacing = 8.dp.toPx()
                for (m in 0 until markCount) {
                    val mx1 = lineStart + lineLength * 0.3f + m * markSpacing
                    val mx2 = lineStart + lineLength * 0.3f + m * markSpacing
                    drawLine(color = ink.copy(alpha = 0.5f), 
                        start = Offset(mx1, y1 - 6.dp.toPx()), end = Offset(mx1, y1 + 6.dp.toPx()), strokeWidth = 1.dp.toPx())
                    drawLine(color = ink.copy(alpha = 0.5f), 
                        start = Offset(mx2, y2 - 6.dp.toPx()), end = Offset(mx2, y2 + 6.dp.toPx()), strokeWidth = 1.dp.toPx())
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
