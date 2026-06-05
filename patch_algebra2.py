import os
import re

base_dir = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\domain\procedural"
alg_path = os.path.join(base_dir, "ProceduralAlgebra.kt")

with open(alg_path, "r", encoding="utf-8") as f:
    alg_content = f.read()

new_alg_methods = """
    private fun generateSystems(difficulty: Int): ProceduralExercise {
        // 2x + y = a
        // x - y = b
        val x = ProceduralEngine.randomInstance.nextInt(-5, 5)
        val y = ProceduralEngine.randomInstance.nextInt(-5, 5)
        val a = 2 * x + y
        val b = x - y
        
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Resolva o sistema linear:\\n2x + y = $a\\nx - y = $b\\nQual é o valor de x?",
            expectedAnswer = x.toString(),
            primarySkill = "systems",
            difficulty = difficulty.toDouble(),
            templateId = "alg_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }

    private fun generatePolynomials(difficulty: Int): ProceduralExercise {
        val root = ProceduralEngine.randomInstance.nextInt(1, 5)
        val pStatement = "Dado o polinômio p(x) = x^2 - ${root + 1}x + $root, encontre as raízes reais."
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = pStatement,
            expectedAnswer = "$root, 1",
            primarySkill = "polynomials",
            difficulty = difficulty.toDouble(),
            templateId = "alg_02",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "text"
        )
    }
"""

alg_content = alg_content.rstrip()
if alg_content.endswith("}"):
    alg_content = alg_content[:-1] + new_alg_methods + "}\n"

alg_router = """
            "systems", "sistemas" -> generateSystems(diff)
            "polynomials", "polinomios" -> generatePolynomials(diff)
"""
alg_content = alg_content.replace('else -> generateSomaSubtracao()', alg_router.strip() + '\n            else -> generateSomaSubtracao()')

with open(alg_path, "w", encoding="utf-8") as f:
    f.write(alg_content)

print("ProceduralAlgebra patched!")
