package com.strava_matematica.data.local.repository

import kotlin.math.abs

object DeterministicValidator {
    fun evaluate(rawUserAnswer: String, expectedAnswer: String, validatorType: String): Boolean {
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

    fun normalize(value: String): String {
        return value.trim()
            .lowercase()
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace(",", ".")
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
