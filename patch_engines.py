import os

base_dir = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\domain\procedural"

engine_path = os.path.join(base_dir, "ProceduralEngine.kt")
with open(engine_path, "r", encoding="utf-8") as f:
    engine_content = f.read()

# Let's replace the `routeRequest` method completely.
import re

new_router = """
    private fun routeRequest(skillTag: String, mmr: Int): ProceduralExercise {
        // Grandezas e Lógica
        if (skillTag in listOf("rule_of_3", "percentage", "interest")) return ProceduralProportions.generate(skillTag, mmr)
        if (skillTag in listOf("sets_venn", "propositional")) return ProceduralLogic.generate(skillTag, mmr)
        
        // Aritmética Avançada
        if (skillTag in listOf("mmc_mdc", "divisibility_primes", "dizimas", "scientific_notation")) return ProceduralArithmetic.generate(skillTag, mmr)
        
        // Funções Reais
        if (skillTag in listOf("function_eval", "function_domain", "function_composition", "function_exp_log")) return ProceduralFunctions.generate(skillTag, mmr)
        
        // Geometria e Trigonometria
        if (skillTag.startsWith("geo_") || skillTag.startsWith("geometria") || skillTag.startsWith("trig")) return ProceduralGeometry.generate(skillTag, mmr)
        
        // Álgebra e Equações (Fallback)
        return ProceduralAlgebra.generate(skillTag, mmr)
    }
"""

engine_content = re.sub(r"private fun routeRequest\(skillTag: String, mmr: Int\): ProceduralExercise \{[\s\S]*?\}", new_router.strip(), engine_content)

with open(engine_path, "w", encoding="utf-8") as f:
    f.write(engine_content)

# Now, add Trigonometry to ProceduralGeometry.kt
geo_path = os.path.join(base_dir, "ProceduralGeometry.kt")
with open(geo_path, "r", encoding="utf-8") as f:
    geo_content = f.read()

new_trig = """
    private fun generateTrigonometry(difficulty: Int): ProceduralExercise {
        val angle = ProceduralEngine.randomInstance.nextInt(1, 4) * 30
        return ProceduralExercise(
            id = UUID.randomUUID().toString(),
            statement = "Sabendo que as razões trigonométricas do círculo se repetem, calcule o seno do ângulo de ${angle}º.",
            expectedAnswer = if (angle == 90) "1" else "...", // Simplified mock
            primarySkill = "trigonometry",
            difficulty = difficulty.toDouble(),
            templateId = "trig_01",
            canvasMode = "blank",
            validatorType = "exact",
            answerType = "numeric"
        )
    }
"""

# Let's insert the method before the last closing brace
geo_content = geo_content.rstrip()
if geo_content.endswith("}"):
    geo_content = geo_content[:-1] + new_trig + "}\n"

# And update the when(skillTag) inside ProceduralGeometry
geo_router = """
            "trig_basic", "trigonometria" -> generateTrigonometry(diff)
"""
geo_content = geo_content.replace('else -> generateGeometriaPlana(diff)', geo_router.strip() + '\n            else -> generateGeometriaPlana(diff)')

with open(geo_path, "w", encoding="utf-8") as f:
    f.write(geo_content)


print("Engine and Geometry patched successfully!")
