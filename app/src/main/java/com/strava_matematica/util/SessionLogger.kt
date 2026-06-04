package com.strava_matematica.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun clear() {
        _logs.value = emptyList()
    }

    private fun log(message: String) {
        val time = dateFormat.format(Date())
        val logEntry = "[$time] $message"
        _logs.value = _logs.value + logEntry
    }

    fun logSessionStart(mode: String, tags: List<String>, difficulty: String) {
        val tagsStr = tags.joinToString(", ")
        log("🚀 SESSÃO INICIADA: $mode")
        log("📋 Tópicos: $tagsStr | MMR Inicial: $difficulty")
    }

    fun logExerciseAttempt(statement: String, studentAnswer: String, isCorrect: Boolean) {
        val result = if (isCorrect) "✅ ACERTOU" else "❌ ERROU"
        log("📝 Exercício: $statement")
        log("   Resposta do Aluno: $studentAnswer -> $result")
    }
    
    fun logSessionEnd(totalCorrect: Int, totalWrong: Int) {
        log("🏁 SESSÃO FINALIZADA")
        log("📊 Resumo: $totalCorrect acertos | $totalWrong erros")
    }

    fun generateShareableText(): String {
        val header = "--- 📡 LIVE LOG: STRAVA MATEMÁTICA ---\nData: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n\n"
        return header + _logs.value.joinToString("\n") + "\n\n-----------------------------------"
    }
}
