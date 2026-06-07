import sys

file_path = 'app/src/main/java/com/strava_matematica/ui/folha/ExerciseField.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import androidx.compose.foundation.layout.Spacer\n', '')
content = content.replace('import androidx.compose.ui.draw.clip\n', '')

parser_block = """    // ── Parser Múltipla Escolha ─────────────────────────────────────────────
    val isMultipleChoice = field.answerType == "multiple_choice"
    val lines = cleanStatement.split("\\n")
    val mcQuestionText = remember(cleanStatement) {
        if (isMultipleChoice) {
            lines.filter { !it.trim().matches(Regex("^[0-9]+\\\\).*")) }.joinToString("\\n").trim()
        } else cleanStatement
    }
    val mcOptions = remember(cleanStatement) {
        if (isMultipleChoice) {
            lines.filter { it.trim().matches(Regex("^[0-9]+\\\\).*")) }.map { it.trim() }
        } else emptyList()
    }
    // ────────────────────────────────────────────────────────────────────────"""

content = content.replace(parser_block + "\n", '')
content = content.replace(parser_block + "\n\n", '')
content = content.replace(parser_block, '')

content = content.replace('text = renderLatex(mcQuestionText),', 'text = renderLatex(cleanStatement),')
content = content.replace('text = "${field.fieldIndex + 1}. " + renderLatex(mcQuestionText),', 'text = "${field.fieldIndex + 1}. " + renderLatex(cleanStatement),')

compact_mc_box = """            // Direita: Caixa de Resposta (Canvas Isolado ou Múltipla Escolha)
            if (isMultipleChoice) {
                Box(modifier = Modifier.weight(0.7f)) {
                    MultipleChoiceButtons(
                        options = mcOptions,
                        typedAnswer = typedAnswer,
                        onSelect = { onTypedAnswerChange(it) },
                        isCorrect = isCorrect,
                        hasAnswer = hasAnswer,
                        ink = ink
                    )
                }
            } else {
                Box("""
content = content.replace(compact_mc_box, """            // Direita: Caixa de Resposta (Canvas Isolado)
            Box(""")

full_mc_box = """                // Rodapé Inferior Direito: Quadrado de Resposta
                if (isMultipleChoice) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        MultipleChoiceButtons(
                            options = mcOptions,
                            typedAnswer = typedAnswer,
                            onSelect = { onTypedAnswerChange(it) },
                            isCorrect = isCorrect,
                            hasAnswer = hasAnswer,
                            ink = ink
                        )
                    }
                } else {
                    Box("""
content = content.replace(full_mc_box, """                // Rodapé Inferior Direito: Quadrado de Resposta
                Box(""")

content = content.replace('            } // Fechamento do else (isMultipleChoice)\n', '')

if '@Composable\nfun MultipleChoiceButtons(' in content:
    content = content.split('@Composable\nfun MultipleChoiceButtons(')[0]

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content.strip() + '\n')
print('Done')
