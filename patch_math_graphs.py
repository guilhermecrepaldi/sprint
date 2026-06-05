import re

file_path = "app/src/main/java/com/strava_matematica/ui/folha/GeometricDiagram.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# We need to insert the new blocks before the end of the `when (type)` block.
# We will find `cartesian_log` and we can just replace the end of the when block.

new_blocks = """
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
                    
                    val yMath = kotlin.math.pow(baseVal, xMath)
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
"""

replacement = new_blocks + "\\n        }\n    }\n}\n\nprivate fun parseSpec("
content = content.replace("        }\n    }\n}\n\nprivate fun parseSpec(", replacement)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied to GeometricDiagram.kt")
