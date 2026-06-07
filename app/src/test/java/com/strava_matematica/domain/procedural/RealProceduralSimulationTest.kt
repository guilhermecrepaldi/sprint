package com.strava_matematica.domain.procedural

import com.strava_matematica.model.MathCurriculum
import com.strava_matematica.model.SessionConfig
import org.junit.Test
import java.io.File
import kotlin.random.Random

class RealProceduralSimulationTest {

    @Test
    fun generateRealSimulationMarkdown() {
        val leaves = MathCurriculum.getFlatNodes().filter { it.children.isEmpty() }
        val sb = StringBuilder()
        sb.append("# Simulação Real do Motor Procedural\n\n")
        sb.append("Este documento contém 3 variações **REAIS** geradas pelo backend Kotlin (com MMR 1000) para cada um dos ${leaves.size} nós da árvore.\n\n")

        val config = SessionConfig()
        
        leaves.forEachIndexed { index, node ->
            val tag = node.proceduralTag ?: node.id
            sb.append("## ${index + 1}. ${node.name} (`$tag`)\n")
            
            for (i in 1..3) {
                // Instanciamos com sementes diferentes (ou apenas Random normal) para forçar variações reais
                val random = Random(System.nanoTime())
                
                try {
                    val exercise = ProceduralEngine.generate(tag, 1000, config)
                    sb.append("- **Variação $i**: ${exercise.statement.replace("\n", " ")} | **Resposta:** ${exercise.expectedAnswer}\n")
                } catch (e: Exception) {
                    sb.append("- **Variação $i**: [FALHA NA GERAÇÃO] ${e.message}\n")
                }
            }
            sb.append("\n")
        }

        // Export artifact
        val outputFile = File("C:/Users/Home/.gemini/antigravity/brain/7a6d2e58-7636-4ebd-9181-a051d07deda7/real_simulado.md")
        outputFile.writeText(sb.toString())
        println("Artifact saved to: ${outputFile.absolutePath}")
    }
}
