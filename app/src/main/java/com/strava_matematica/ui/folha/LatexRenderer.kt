package com.strava_matematica.ui.folha

/**
 * Converts common LaTeX notation to readable Unicode text.
 * Handles the patterns present in the exercise seed and AI-generated exercises.
 */
fun renderLatex(input: String): String {
    var text = input

    // Strip inline math delimiters $...$
    text = text.replace(Regex("""\$([^$]+)\$""")) { it.groupValues[1] }

    // \frac{a}{b} → a/b  (with parens when numerator/denominator are compound)
    text = text.replace(Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""")) { mr ->
        val num = mr.groupValues[1].trim()
        val den = mr.groupValues[2].trim()
        val n = if (num.length > 1 && num.any { it in "+-" }) "($num)" else num
        val d = if (den.length > 1 && den.any { it in "+-" }) "($den)" else den
        "$n/$d"
    }

    // \sqrt{x} → √x
    text = text.replace(Regex("""\\sqrt\{([^{}]*)\}""")) { "√${it.groupValues[1]}" }

    // x^{n} → xⁿ  (superscript digits/minus)
    text = text.replace(Regex("""\^\{([^{}]*)\}""")) { mr ->
        mr.groupValues[1].map { c -> superscript(c) }.joinToString("")
    }

    // bare x^n (single char exponent)
    text = text.replace(Regex("""\^([0-9\-n])""")) { mr ->
        superscript(mr.groupValues[1][0]).toString()
    }

    // \log_{b} → log_b (subscript)
    text = text.replace(Regex("""\\log_\{([^{}]*)\}""")) { mr ->
        "log" + mr.groupValues[1].map { c -> subscript(c) }.joinToString("")
    }

    // \log_b (single digit, no braces)
    text = text.replace(Regex("""\\log_(\d)""")) { mr ->
        "log" + subscript(mr.groupValues[1][0])
    }

    // \lim_{x \to a} → lim(x→a)
    text = text.replace(Regex("""\\lim_\{([^{}]*)\}""")) { mr ->
        val sub = mr.groupValues[1]
            .replace("\\to", "→")
            .replace("\\infty", "∞")
            .trim()
        "lim($sub)"
    }

    // Common symbols
    text = text.replace("\\int", "∫")
    text = text.replace("\\geq", "≥")
    text = text.replace("\\leq", "≤")
    text = text.replace("\\neq", "≠")
    text = text.replace("\\pm", "±")
    text = text.replace("\\mp", "∓")
    text = text.replace("\\to", "→")
    text = text.replace("\\cdot", "·")
    text = text.replace("\\times", "×")
    text = text.replace("\\div", "÷")
    text = text.replace("\\infty", "∞")
    text = text.replace("\\pi", "π")
    text = text.replace("\\alpha", "α")
    text = text.replace("\\beta", "β")
    text = text.replace("\\Delta", "Δ")

    // LaTeX spacing / formatting
    text = text.replace("{,}", ",")   // decimal comma \text{,}
    text = text.replace("\\,", " ")  // thin space
    text = text.replace("\\!", "")   // negative thin space

    // Remove any remaining unknown \commands
    text = text.replace(Regex("""\\[a-zA-Z]+"""), "")

    // Remove leftover braces
    text = text.replace("{", "").replace("}", "")

    var cleaned = text.trim()
    if (cleaned.endsWith("=?")) {
        cleaned = cleaned.dropLast(2).trim()
    } else if (cleaned.endsWith("= ?")) {
        cleaned = cleaned.dropLast(3).trim()
    } else if (cleaned.endsWith("?")) {
        cleaned = cleaned.dropLast(1).trim()
    }
    if (cleaned.endsWith("=")) {
        cleaned = cleaned.dropLast(1).trim()
    }

    return cleaned
}

private fun superscript(c: Char): Char = when (c) {
    '0' -> '⁰'; '1' -> '¹'; '2' -> '²'; '3' -> '³'; '4' -> '⁴'
    '5' -> '⁵'; '6' -> '⁶'; '7' -> '⁷'; '8' -> '⁸'; '9' -> '⁹'
    '-' -> '⁻'; '+' -> '⁺'; 'n' -> 'ⁿ'; 'x' -> 'ˣ'
    else -> c
}

private fun subscript(c: Char): Char = when (c) {
    '0' -> '₀'; '1' -> '₁'; '2' -> '₂'; '3' -> '₃'; '4' -> '₄'
    '5' -> '₅'; '6' -> '₆'; '7' -> '₇'; '8' -> '₈'; '9' -> '₉'
    else -> c
}
