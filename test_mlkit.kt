package com.strava_matematica.recognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier

fun main() {
    try {
        val id1 = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zmath-x-math")
        println("zxx-Zmath-x-math worked: " + id1?.languageTag)
    } catch(e: Exception) { println("zxx-Zmath-x-math error: " + e.message) }

    try {
        val id2 = DigitalInkRecognitionModelIdentifier.fromLanguageTag("math")
        println("math worked: " + id2?.languageTag)
    } catch(e: Exception) { println("math error: " + e.message) }
    
    try {
        val id3 = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-math")
        println("zxx-Zsym-x-math worked: " + id3?.languageTag)
    } catch(e: Exception) { println("zxx-Zsym-x-math error: " + e.message) }
}
