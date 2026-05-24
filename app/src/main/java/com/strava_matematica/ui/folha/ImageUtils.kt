package com.strava_matematica.ui.folha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun exportBitmap(strokes: List<List<Offset>>, width: Int = 800, height: Int = 300): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Fundo branco exigido pelo OCR

        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.size == 1) {
                val point = stroke.first()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(point.x, point.y, paint.strokeWidth / 2, paint)
                paint.style = Paint.Style.STROKE
            } else if (stroke.size > 1) {
                val path = android.graphics.Path()
                path.moveTo(stroke.first().x, stroke.first().y)
                for (i in 1 until stroke.size) {
                    val p = stroke[i]
                    path.lineTo(p.x, p.y)
                }
                canvas.drawPath(path, paint)
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
