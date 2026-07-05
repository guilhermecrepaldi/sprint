package com.sprint.data.local.repository

import kotlin.math.abs

/**
 * Validador determinístico de respostas matemáticas.
 *
 * Regras de segurança:
 * - Resposta deve ter pelo menos o mesmo número de dígitos da esperada.
 * - Sempre requer match exato da expressão numérica.
 * - Fallback numérico só aceita depois de passar pela validação de dígitos.
 */
object DeterministicValidator {
    fun evaluate(rawUserAnswer: String, expectedAnswer: String, validatorType: String): Boolean {
        val cleanedUser = stripSetNotation(rawUserAnswer)
        val cleanedExpected = stripSetNotation(expectedAnswer)

        val isListOrSystem = cleanedExpected.contains(",") || cleanedExpected.contains(";")

        if (isListOrSystem && validatorType.lowercase() != "regex") {
            return evaluateListOrSystem(cleanedUser, cleanedExpected)
        }

        val user = normalize(rawUserAnswer)
        if (user.isBlank()) return false

        val expected = expectedAnswer
            .split("|")
            .flatMap { splitPortugueseOptions(it) }
            .map(::normalize)
            .filter { it.isNotBlank() }

        return when (validatorType.lowercase()) {
            "regex" -> expected.any { pattern ->
                runCatching { Regex(pattern, RegexOption.IGNORE_CASE).matches(user) }.getOrDefault(false)
            }
            "numeric" -> expected.any { strictNumericMatch(user, it) }
            "fraction" -> expected.any { strictNumericMatch(user, it) || user == it }
            "equation" -> expected.any { equationEquals(user, it) }
            else -> expected.any { user == it || equationEquals(user, it) || strictNumericMatch(user, it) }
        }
    }

    /**
     * Validação numérica rigorosa:
     * 1. Resposta do usuário deve ter pelo menos o mesmo número de dígitos que a esperada.
     * 2. Valores numéricos precisam ser iguais (tolerância 0.000001).
     */
    private fun strictNumericMatch(user: String, expected: String): Boolean {
        val a = parseNumeric(user) ?: return false
        val b = parseNumeric(expected) ?: return false

        // Regra: resposta deve ter pelo menos o mesmo número de dígitos que a esperada
        val userDigitCount = user.count { it.isDigit() }
        val expectedDigitCount = expected.count { it.isDigit() }
        if (userDigitCount < expectedDigitCount) return false

        // Match numérico com tolerância
        return abs(a - b) < 0.000001
    }

    private fun evaluateListOrSystem(rawUserAnswer: String, expectedAnswer: String): Boolean {
        val userParts = rawUserAnswer.split(Regex("[,;]")).map { normalize(stripSetNotation(it)) }.filter { it.isNotBlank() }
        val expectedParts = expectedAnswer.split(Regex("[,;]")).map { normalize(stripSetNotation(it)) }.filter { it.isNotBlank() }

        if (userParts.size != expectedParts.size) return false

        val matchedExpected = mutableSetOf<Int>()
        for (uPart in userParts) {
            val matchIndex = expectedParts.indices.firstOrNull { eIdx ->
                !matchedExpected.contains(eIdx) &&
                (uPart == expectedParts[eIdx] || equationEquals(uPart, expectedParts[eIdx]) || strictNumericMatch(uPart, expectedParts[eIdx]))
            }
            if (matchIndex != null) {
                matchedExpected.add(matchIndex)
            } else {
                return false
            }
        }
        return matchedExpected.size == expectedParts.size
    }

    fun normalize(value: String): String {
        return value.trim()
            .lowercase()
            .replace("\u2212", "-")
            .replace("\u00D7", "*")
            .replace("\u00F7", "/")
            .replace("\u00BE", "3/4")
            .replace("\u00BC", "1/4")
            .replace("\u00BD", "1/2")
            .replace("\\text{sen}", "sen")
            .replace("\\sen", "sen")
            .replace("\\cos", "cos")
            .replace("\\tg", "tg")
            .replace("\\cdot", "*")
            .replace("\\left", "")
            .replace("\\right", "")
            .replace("\\s+".toRegex(), "")
    }

    fun stripSetNotation(value: String): String {
        var result = value.trim()
        result = result.replace(Regex("""^[a-zA-Z]\s*=\s*\{"""), "")
        result = result.replace(Regex("""^[a-zA-Z]\\s*\\\\in\s*\{"""), "")
        result = result.replace(Regex("""}$"""), "")
        result = result.replace(Regex("""^\{"""), "")
        return result.trim()
    }

    private fun splitPortugueseOptions(value: String): List<String> {
        return value
            .replace(" ou ", "|", ignoreCase = true)
            .split("|")
    }

    private fun equationEquals(user: String, expected: String): Boolean {
        if (user == expected) return true
        val expectedRight = expected.substringAfter("=", missingDelimiterValue = expected)
        val userRight = user.substringAfter("=", missingDelimiterValue = user)
        return userRight == expectedRight || strictNumericMatch(userRight, expectedRight)
    }

    private fun numericEquals(left: String, right: String): Boolean {
        val a = parseNumeric(left) ?: return false
        val b = parseNumeric(right) ?: return false
        return abs(a - b) < 0.000001
    }

    private fun parseNumeric(value: String): Double? {
        val clean = value
            .removePrefix("x=")
            .removePrefix("y=")
            .removePrefix("f(x)=")
            .replace("\\frac", "frac")
            .replace(",", ".")

        Regex("""frac\{(-?\d+(?:\.\d+)?)\}\{(-?\d+(?:\.\d+)?)\}""")
            .matchEntire(clean)
            ?.let { match ->
                val den = match.groupValues[2].toDouble()
                if (den == 0.0) return null
                return match.groupValues[1].toDouble() / den
            }

        if ("/" in clean && clean.count { it == '/' } == 1) {
            val parts = clean.split("/")
            val num = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
            val den = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
            if (den == 0.0) return null
            return num / den
        }

        return clean.toDoubleOrNull()
    }

    fun formatFailedMessage(userAnswer: String, expectedAnswer: String): String? {
        val u = normalize(userAnswer)
        val e = normalize(expectedAnswer)
        if (u.isBlank() || e.isBlank()) return null

        val uNum = u.toDoubleOrNull()
        val eNum = e.toDoubleOrNull()
        if (uNum != null && eNum != null) {
            return null
        }

        val hasSetNotation = expectedAnswer.contains("{") && expectedAnswer.contains("}")
        if (hasSetNotation) {
            val stripped = stripSetNotation(userAnswer)
            if (stripped != userAnswer) return null
            val hint = stripSetNotation(expectedAnswer)
            return "formato esperado: " + hint
        }
        return null
    }
}
