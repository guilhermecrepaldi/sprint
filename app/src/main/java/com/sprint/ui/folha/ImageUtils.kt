package com.sprint.ui.folha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Exports ink strokes as a base64-encoded PNG suitable for OCR.
     *
     * Strokes are in screen-pixel coordinates from InkCanvas. They are
     * normalized to fill the export canvas (with padding), so the OCR
     * always receives a well-sized image regardless of screen resolution.
     *
     * outWidth / outHeight: final image dimensions in pixels.
     */
    fun exportBitmap(
        strokes: List<List<Offset>>,
        outWidth: Int = 800,
        outHeight: Int = 300,
    ): String {
        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val allPoints = strokes.flatten()
        if (allPoints.isEmpty()) {
            // Nothing drawn — return white image.
            return encodeToBase64(bitmap)
        }

        val padding = outWidth * 0.05f   // 5% padding on each side

        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }

        val inkW = (maxX - minX).coerceAtLeast(1f)
        val inkH = (maxY - minY).coerceAtLeast(1f)

        // Scale to fill the export canvas (always upscale if strokes are small).
        val availW = outWidth - 2 * padding
        val availH = outHeight - 2 * padding
        val scale = minOf(availW / inkW, availH / inkH)

        // Center the scaled ink inside the padded area.
        val dx = padding + (availW - inkW * scale) / 2f - minX * scale
        val dy = padding + (availH - inkH * scale) / 2f - minY * scale

        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = (4f * scale).coerceIn(2f, 8f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.isEmpty()) continue

            if (stroke.size == 1) {
                val sx = stroke[0].x * scale + dx
                val sy = stroke[0].y * scale + dy
                paint.style = Paint.Style.FILL
                canvas.drawCircle(sx, sy, paint.strokeWidth / 2f, paint)
                paint.style = Paint.Style.STROKE
            } else {
                val path = android.graphics.Path()
                path.moveTo(stroke[0].x * scale + dx, stroke[0].y * scale + dy)
                for (i in 1 until stroke.size) {
                    path.lineTo(stroke[i].x * scale + dx, stroke[i].y * scale + dy)
                }
                canvas.drawPath(path, paint)
            }
        }

        return encodeToBase64(bitmap)
    }

    private fun encodeToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
