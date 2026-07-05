package com.sprint

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sprint.recognizer.StrokeCharacterRecognizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes instrumentados para o StrokeCharacterRecognizer.
 *
 * Gera strokes artificiais simulando escrita caractere por caractere,
 * da esquerda para a direita, com distância X entre cada caractere.
 */
@RunWith(AndroidJUnit4::class)
class StrokeCharacterRecognizerTest {

    companion object {
        private const val TAG = "RecognizerTest"
        // Distância entre caracteres simulados (px)
        private const val CHAR_SPACING = 60f
        // Y central da escrita
        private const val WRITE_Y = 200f
        // Tamanho do traço simulado
        private const val STROKE_SPAN = 30f
    }

    private lateinit var recognizer: StrokeCharacterRecognizer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        recognizer = StrokeCharacterRecognizer(context)
    }

    @Test
    fun testSingleDigit() = runBlocking {
        // Simula escrita do "5" — um traço horizontal e um vertical
        val strokes = listOf(
            listOf(Offset(100f, WRITE_Y), Offset(130f, WRITE_Y)),          // topo do 5
            listOf(Offset(100f, WRITE_Y), Offset(100f, WRITE_Y + STROKE_SPAN)), // descida
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testSingleDigit: '$result'")
        assertNotNull("Deveria reconhecer o dígito 5", result)
    }

    @Test
    fun testMinusSign() = runBlocking {
        // Simula escrita do "-" — um traço horizontal
        val strokes = listOf(
            listOf(Offset(100f, WRITE_Y), Offset(130f, WRITE_Y))
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testMinusSign: '$result'")
        // ML Kit pode retornar "-" ou "—"
        assertNotNull("Deveria reconhecer sinal de menos", result)
    }

    @Test
    fun testNegativeNumber() = runBlocking {
        // Simula "-126": 4 caracteres separados por 60px
        val strokes = listOf(
            // "-" na posição 100
            listOf(Offset(100f, WRITE_Y), Offset(120f, WRITE_Y)),
            // "1" na posição 160
            listOf(Offset(160f, WRITE_Y - STROKE_SPAN), Offset(160f, WRITE_Y + STROKE_SPAN)),
            // "2" na posição 220
            listOf(Offset(220f, WRITE_Y - STROKE_SPAN), Offset(250f, WRITE_Y - STROKE_SPAN),
                   Offset(220f, WRITE_Y), Offset(250f, WRITE_Y + STROKE_SPAN)),
            // "6" na posição 280
            listOf(Offset(280f, WRITE_Y - STROKE_SPAN), Offset(310f, WRITE_Y - STROKE_SPAN),
                   Offset(310f, WRITE_Y), Offset(280f, WRITE_Y + STROKE_SPAN),
                   Offset(310f, WRITE_Y + STROKE_SPAN)),
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testNegativeNumber: '$result'")
        assertNotNull("Deveria reconhecer número negativo", result)
        if (result != null) {
            assertEquals(result.filter { it.isDigit() || it == '-' }.length, 4)
        }
    }

    @Test
    fun testSetNotation() = runBlocking {
        // Simula "S = { x , 10 }"
        val strokes = listOf(
            // "S"
            listOf(Offset(50f, WRITE_Y - STROKE_SPAN), Offset(50f, WRITE_Y),
                   Offset(70f, WRITE_Y), Offset(50f, WRITE_Y + STROKE_SPAN)),
            // "="
            listOf(Offset(110f, WRITE_Y - 8f), Offset(130f, WRITE_Y - 8f)),
            listOf(Offset(110f, WRITE_Y + 8f), Offset(130f, WRITE_Y + 8f)),
            // "{"
            listOf(Offset(170f, WRITE_Y - STROKE_SPAN), Offset(160f, WRITE_Y),
                   Offset(170f, WRITE_Y + STROKE_SPAN)),
            // "x"
            listOf(Offset(220f, WRITE_Y - STROKE_SPAN), Offset(250f, WRITE_Y + STROKE_SPAN)),
            listOf(Offset(250f, WRITE_Y - STROKE_SPAN), Offset(220f, WRITE_Y + STROKE_SPAN)),
            // ","
            listOf(Offset(290f, WRITE_Y), Offset(290f, WRITE_Y + 10f)),
            // "1"
            listOf(Offset(340f, WRITE_Y - STROKE_SPAN), Offset(340f, WRITE_Y + STROKE_SPAN)),
            // "0"
            listOf(Offset(390f, WRITE_Y - STROKE_SPAN), Offset(410f, WRITE_Y - STROKE_SPAN),
                   Offset(410f, WRITE_Y + STROKE_SPAN), Offset(390f, WRITE_Y + STROKE_SPAN),
                   Offset(390f, WRITE_Y - STROKE_SPAN)),
            // "}"
            listOf(Offset(450f, WRITE_Y - STROKE_SPAN), Offset(460f, WRITE_Y),
                   Offset(450f, WRITE_Y + STROKE_SPAN)),
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testSetNotation: '$result'")
        assertNotNull("Deveria reconhecer notação de conjunto", result)
    }

    @Test
    fun testFraction() = runBlocking {
        // Simula "1/2"
        val strokes = listOf(
            // "1"
            listOf(Offset(100f, WRITE_Y - STROKE_SPAN), Offset(100f, WRITE_Y)),
            // "/"
            listOf(Offset(140f, WRITE_Y - STROKE_SPAN), Offset(160f, WRITE_Y + STROKE_SPAN)),
            // "2"
            listOf(Offset(200f, WRITE_Y - STROKE_SPAN), Offset(220f, WRITE_Y - STROKE_SPAN),
                   Offset(200f, WRITE_Y), Offset(220f, WRITE_Y + STROKE_SPAN)),
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testFraction: '$result'")
        assertNotNull("Deveria reconhecer fração", result)
    }

    @Test
    fun testEmptyStrokes() = runBlocking {
        val result = recognizer.recognize(emptyList(), null)
        assertNull("Strokes vazios deve retornar null", result)
    }

    @Test
    fun testSingleStrokeDot() = runBlocking {
        // Simula um ponto ".", traço muito curto
        val strokes = listOf(
            listOf(Offset(100f, WRITE_Y), Offset(102f, WRITE_Y))
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testSingleStrokeDot: '$result'")
        // Ponto minúsculo pode não ser reconhecido — só verificamos que não crasha
    }

    @Test
    fun testPowerNotation() = runBlocking {
        // Simula "n²": "n" na pos 100, "²" na pos 160 (mais alto)
        val strokes = listOf(
            // "n"
            listOf(Offset(100f, WRITE_Y), Offset(100f, WRITE_Y - STROKE_SPAN),
                   Offset(130f, WRITE_Y - STROKE_SPAN), Offset(130f, WRITE_Y)),
            // "²" (pequeno, mais alto)
            listOf(Offset(170f, WRITE_Y - STROKE_SPAN - 20f), Offset(185f, WRITE_Y - STROKE_SPAN - 20f),
                   Offset(185f, WRITE_Y - STROKE_SPAN), Offset(170f, WRITE_Y - STROKE_SPAN)),
        )
        val result = recognizer.recognize(strokes, null)
        Log.d(TAG, "testPowerNotation: '$result'")
        assertNotNull("Deveria reconhecer n²", result)
    }
}
