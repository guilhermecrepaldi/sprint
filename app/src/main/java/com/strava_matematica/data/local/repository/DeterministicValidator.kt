package com.strava_matematica.data.local.repository

import kotlin.math.abs

object DeterministicValidator {
    fun evaluate(rawUserAnswer: String, expectedAnswer: String, validatorType: String): Boolean {
        // Handle "x=2, y=3" or "2, 3" by splitting first
        val isListOrSystem = expectedAnswer.contains(",") || expectedAnswer.contains(";")
        
        if (isListOrSystem && validatorType.lowercase() != "regex") {
            return evaluateListOrSystem(rawUserAnswer, expectedAnswer)
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
            "numeric" -> expected.any { numericEquals(user, it) }
            "fraction" -> expected.any { numericEquals(user, it) || user == it }
            "equation" -> expected.any { equationEquals(user, it) }
            else -> expected.any { user == it || equationEquals(user, it) || numericEquals(user, it) }
        }
    }
    
    private fun evaluateListOrSystem(rawUserAnswer: String, expectedAnswer: String): Boolean {
        // Split by comma or semicolon
        val userParts = rawUserAnswer.split(Regex("[,;]")).map { normalize(it) }.filter { it.isNotBlank() }
        val expectedParts = expectedAnswer.split(Regex("[,;]")).map { normalize(it) }.filter { it.isNotBlank() }
        
        if (userParts.size != expectedParts.size) return false
        
        // Se houver "x=" ou "y=", é um sistema de equações. A ordem não deve importar estritamente,
        // mas as chaves (x, y) devem estar corretas. Como normalize remove espaços, teremos "x=2" e "y=3".
        // Para raizes puras "2, 3", verificamos a intersecção/equivalência de cada elemento (sem ordem).
        
        val matchedExpected = mutableSetOf<Int>()
        for (uPart in userParts) {
            val matchIndex = expectedParts.indices.firstOrNull { eIdx -> 
                !matchedExpected.contains(eIdx) && 
                (uPart == expectedParts[eIdx] || equationEquals(uPart, expectedParts[eIdx]) || numericEquals(uPart, expectedParts[eIdx]))
            }
            if (matchIndex != null) {
                matchedExpected.add(matchIndex)
            } else {
                return false // Achou uma parte do usuario que nao bate com nenhuma parte esperada disponivel
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
            // Removemos a conversão de vírgula para ponto AQUI, porque ela quebra decimais vs separadores de lista.
            // Para decimais BR, a conversão é feita internamente no parseNumeric.
            .replace("\\text{sen}", "sen")
            .replace("\\sen", "sen")
            .replace("\\cos", "cos")
            .replace("\\tg", "tg")
            .replace("\\cdot", "*")
            .replace("\\left", "")
            .replace("\\right", "")
            .replace("\\s+".toRegex(), "")
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
        return userRight == expectedRight || numericEquals(userRight, expectedRight)
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
            .replace(",", ".") // Agora a conversão de virgula pra decimal só acontece durante o parse numérico

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
}
