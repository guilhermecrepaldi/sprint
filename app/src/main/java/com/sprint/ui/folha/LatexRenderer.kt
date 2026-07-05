package com.sprint.ui.folha

/**
 * Converts common LaTeX notation to readable Unicode text.
 * v2.0: Fixed polynomial handling, equation display, and margin cases.
 */
fun renderLatex(input: String): String {
    var text = input

    // Strip inline math delimiters $...$ and $$...$$
    text = text.replace(Regex("\$\$([^\$]+)\$\$")) { it.groupValues[1] }
    text = text.replace(Regex("\$([^\$]+)\$")) { it.groupValues[1] }

    // \frac{a}{b} → fraction with proper grouping
    while (text.contains(Regex("""\frac\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}"""))) {
        text = text.replace(Regex("""\frac\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""")) { mr ->
            var num = mr.groupValues[1].trim()
            var den = mr.groupValues[2].trim()
            val nWrap = if (num.length > 1 && (num.contains("+") || num.contains("-") || num.contains(" "))) "($num)" else num
            val dWrap = if (den.length > 1 && (den.contains("+") || den.contains("-") || den.contains(" "))) "($den)" else den
            "$nWrap/$dWrap"
        }
    }

    // \sqrt[n]{x} → ⁿ√x (root with index)
    text = text.replace(Regex("""\sqrt\[([^\]]+)\]\{([^{}]*)\}""")) { mr ->
        val idx = mr.groupValues[1].trim()
        val rad = mr.groupValues[2].trim()
        if (idx == "2") "√$rad" else "${superscript(idx[0])}√$rad"
    }
    // \sqrt{x} → √x
    text = text.replace(Regex("""\sqrt\{([^{}]*)\}""")) { "√${it.groupValues[1]}" }

    // Superscript: x^{...}
    text = text.replace(Regex("""\^\{([^{}]*)\}""")) { mr ->
        mr.groupValues[1].map { c -> superscript(c) }.joinToString("")
    }

    // Bare x^n (single char exponent)
    text = text.replace(Regex("""\^([0-9\-n])(?![0-9])""")) { mr ->
        superscript(mr.groupValues[1][0]).toString()
    }

    // Subscript: _{...}
    text = text.replace(Regex("""_\{([^{}]*)\}""")) { mr ->
        mr.groupValues[1].map { c -> subscript(c) }.joinToString("")
    }

    // Bare _x
    text = text.replace(Regex("""_([a-zA-Z0-9])""")) { mr ->
        subscript(mr.groupValues[1][0]).toString()
    }

    // \log_{b} → log_b
    text = text.replace(Regex("""\\log_\{([^{}]*)\}""")) { mr ->
        "log" + mr.groupValues[1].map { c -> subscript(c) }.joinToString("")
    }
    text = text.replace(Regex("""\\log_(\d)""")) { mr ->
        "log" + subscript(mr.groupValues[1][0])
    }

    // \lim_{x \to a} → lim(x->a)
    text = text.replace(Regex("""\\lim_\{([^{}]*)\}""")) { mr ->
        val sub = mr.groupValues[1]
            .replace("\\to", "->")
            .replace("\\infty", "inf")
            .trim()
        "lim($sub)"
    }

    // \binom{n}{k} → C(n,k)
    text = text.replace(Regex("""\\binom\{([^{}]*)\}\{([^{}]*)\}""")) { mr ->
        "C(${mr.groupValues[1]},${mr.groupValues[2]})"
    }

    // \text{...} → literal text
    text = text.replace(Regex("""\\text\{([^{}]*)\}""")) { it.groupValues[1] }
    text = text.replace(Regex("""\\texttt\{([^{}]*)\}""")) { it.groupValues[1] }
    text = text.replace(Regex("""\\mathrm\{([^{}]*)\}""")) { it.groupValues[1] }

    // Common symbols (process before generic command stripping)
    val symbols = mapOf(
        "\\int" to "\u222B", "\\sum" to "\u03A3", "\\prod" to "\u03A0",
        "\\geq" to "\u2265", "\\leq" to "\u2264", "\\neq" to "\u2260", "\\approx" to "\u2248",
        "\\pm" to "\u00B1", "\\mp" to "\u2213",
        "\\to" to "\u2192", "\\rightarrow" to "\u2192", "\\leftarrow" to "\u2190",
        "\\cdot" to "\u00B7", "\\times" to "\u00D7", "\\div" to "\u00F7",
        "\\infty" to "\u221E", "\\partial" to "\u2202", "\\nabla" to "\u2207",
        "\\pi" to "\u03C0",
        "\\alpha" to "\u03B1", "\\beta" to "\u03B2", "\\gamma" to "\u03B3",
        "\\delta" to "\u03B4", "\\Delta" to "\u0394", "\\theta" to "\u03B8",
        "\\lambda" to "\u03BB", "\\mu" to "\u03BC", "\\sigma" to "\u03C3",
        "\\omega" to "\u03C9", "\\Omega" to "\u03A9",
        "\\subset" to "\u2282", "\\supset" to "\u2283", "\\subseteq" to "\u2286",
        "\\supseteq" to "\u2287",
        "\\in" to "\u2208", "\\notin" to "\u2209", "\\forall" to "\u2200",
        "\\exists" to "\u2203", "\\emptyset" to "\u2205",
        "\\angle" to "\u2220", "\\perp" to "\u22A5",
        "\\cong" to "\u2245", "\\sim" to "\u223C",
    )
    for ((cmd, sym) in symbols) {
        text = text.replace(cmd, sym)
    }

    // LaTeX spacing
    text = text.replace("\\,", " ")
    text = text.replace("\\!", "")
    text = text.replace("\\:", " ")
    text = text.replace("\\;", " ")
    text = text.replace("\\quad", "  ")
    text = text.replace("\\qquad", "    ")
    text = text.replace("\\ ", " ")

    // \\ -> newline
    text = text.replace("\\\\\\\\", "\n")

    // Remove decorative braces from LaTeX groups
    text = text.replace(Regex("""\{([^{}]*)\}""")) { it.groupValues[1] }

    // Remove any remaining unknown \commands
    text = text.replace(Regex("""\[a-zA-Z]+"""), "")

    // Clean trailing = signs and ?
    var cleaned = text.trim()
    
    if (cleaned.endsWith("= ?") || cleaned.endsWith("=?")) {
        cleaned = cleaned.dropLast(3).trim()
    } else if (cleaned.endsWith("=?")) {
        cleaned = cleaned.dropLast(2).trim()
    } else if (cleaned.endsWith("?")) {
        cleaned = cleaned.dropLast(1).trim()
    }
    if (cleaned.endsWith("=") && !cleaned.contains("\u2260") && !cleaned.contains("\u2265") && !cleaned.contains("\u2264")) {
        cleaned = cleaned.dropLast(1).trim()
    }
    
    // Clean double spaces
    cleaned = cleaned.replace(Regex("""\s+"""), " ")
    
    return cleaned
}

private fun superscript(c: Char): Char = when (c) {
    '0' -> '\u2070'; '1' -> '\u00B9'; '2' -> '\u00B2'; '3' -> '\u00B3'; '4' -> '\u2074'
    '5' -> '\u2075'; '6' -> '\u2076'; '7' -> '\u2077'; '8' -> '\u2078'; '9' -> '\u2079'
    '-' -> '\u207B'; '+' -> '\u207A'; 'n' -> '\u207F'; 'x' -> '\u02E3'
    else -> c
}

private fun subscript(c: Char): Char = when (c) {
    '0' -> '\u2080'; '1' -> '\u2081'; '2' -> '\u2082'; '3' -> '\u2083'; '4' -> '\u2084'
    '5' -> '\u2085'; '6' -> '\u2086'; '7' -> '\u2087'; '8' -> '\u2088'; '9' -> '\u2089'
    else -> c
}
