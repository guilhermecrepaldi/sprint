import os

file_path = r"app\src\main\java\com\strava_matematica\domain\procedural\ProceduralAlgebra.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

replacements = [
    (
        '"fatoracao_produtos_notaveis", "polinomios" -> generatePolynomial(mmr, random)',
        '"fatoracao_produtos_notaveis", "polinomios", "alg_elem_poly" -> generatePolynomial(mmr, random)'
    ),
    (
        'statement = "Converta a fração para número decimal:\\n\\n\\\\( \\\\frac{}{} \\\\)",',
        'statement = f"Converta a fração para número decimal:\\n\\n\\\\( \\\\frac{{num}}{{den}} \\\\)",'
    ),
    (
        'statement = "Converta o número decimal para fração irredutível (ex: a/b):\\n\\n\\\\(  \\\\)",\n                expectedAnswer = "/",',
        'statement = f"Converta o número decimal para fração irredutível (ex: a/b):\\n\\n\\\\( {decimalStr} \\\\)",\n                expectedAnswer = f"{numSimp}/{denSimp}",'
    ),
    (
        'statement = "Calcule o valor de:\\n\\n\\\\( \\\\% \\\\text{ de }  \\\\)",',
        'statement = f"Calcule o valor de:\\n\\n\\\\( {pct}\\\\% \\\\text{{ de }} {base} \\\\)",'
    ),
    (
        'val p2 = ratioBase * multiplier',
        'val p2 = ratioBase * multiplier\n            var g1 = 2; var g2 = ratioBase;\n            while(g2 > 0) { val t = g2; g2 = g1 % g2; g1 = t; }\n            val numAns = 2 / g1\n            val denAns = ratioBase / g1'
    ),
    (
        'statement = "Simplifique a razão para a forma irredutível a/b:\\n\\n\\\\( \\\\frac{}{} \\\\)",\n                // GCD\n                expectedAnswer = "2/".replace("2/2", "1/1").replace("2/4", "1/2").replace("2/6", "1/3"),',
        'statement = f"Simplifique a razão para a forma irredutível a/b:\\n\\n\\\\( \\\\frac{{p1}}{{p2}} \\\\)",\n                // GCD\n                expectedAnswer = f"{numAns}/{denAns}",'
    ),
    (
        'statement = "Calcule o valor da potência:\\n\\n\\\\( ^ \\\\)",',
        'statement = f"Calcule o valor da potência:\\n\\n\\\\( {base}^{{{exp}}} \\\\)",'
    ),
    (
        'statement = "Calcule o valor da raiz quadrada:\\n\\n\\\\( \\\\sqrt{} \\\\)",',
        'statement = f"Calcule o valor da raiz quadrada:\\n\\n\\\\( \\\\sqrt{{{inside}}} \\\\)",'
    ),
    (
        'statement = "Resolva a inequação para x:\\n\\n\\\\(  x    \\\\)",\n            expectedAnswer = "x",',
        'statement = f"Resolva a inequação para x:\\n\\n\\\\( {a}x {bStr}{abs(b)} {sign} {c} \\\\)",\n            expectedAnswer = f"x {sign} {x}",'
    ),
    (
        'statement = "Dada a função afim \\\\( f(x) =  x  \\\\), calcule o valor de \\\\( f() \\\\).",',
        'statement = f"Dada a função afim \\\\( f(x) = {a}x {bStr}{abs(b)} \\\\), calcule o valor de \\\\( f({x}) \\\\).",'
    ),
    (
        'val bStr = if (b > 0) "+ x" else if (b < 0) "- x" else ""\n        val cStr = if (c > 0) "+ " else if (c < 0) "- " else ""\n        val signA = if (a == 1) "" else "-"',
        'val bStr = if (b > 0) "+ x" else if (b < 0) "- x" else ""\n        val cStr = if (c > 0) "+ " else if (c < 0) "- " else ""\n        val signA = if (a == 1) "" else "-"'
    ),
    (
        'statement = "Dada a função quadrática \\\\( f(x) = x^2   \\\\), encontre a coordenada \\\\( y \\\\) do vértice (ou seja, o valor máximo/mínimo da função).",',
        'statement = f"Dada a função quadrática \\\\( f(x) = {signA}x^2 {bStr} {cStr} \\\\), encontre a coordenada \\\\( y \\\\) do vértice (ou seja, o valor máximo/mínimo da função).".replace("  ", " "),'
    ),
    (
        'statement = "Resolva a equação exponencial para x:\\n\\n\\\\( ^x =  \\\\)",',
        'statement = f"Resolva a equação exponencial para x:\\n\\n\\\\( {base}^x = {result} \\\\)",'
    ),
    (
        'statement = "Calcule o valor do logaritmo:\\n\\n\\\\( \\\\log_{}() \\\\)",',
        'statement = f"Calcule o valor do logaritmo:\\n\\n\\\\( \\\\log_{{{base}}}({argument}) \\\\)",'
    ),
    (
        'val expected = ", "\n        return ProceduralExercise(\n            id = UUID.randomUUID().toString(),\n            statement = "Resolva a equação modular e encontre os valores de x:\\n\\n\\\\( | x | =  \\\\)",',
        'val expected = f"{num1}, {num2}"\n        return ProceduralExercise(\n            id = UUID.randomUUID().toString(),\n            statement = f"Resolva a equação modular e encontre os valores de x:\\n\\n\\\\( |{a}x {bStr}{abs(b)}| = {c} \\\\)",'
    )
]

new_content = content
for old, new in replacements:
    old = old.replace('f"', '"').replace('{num}', '').replace('{den}', '').replace('{decimalStr}', '').replace('{numSimp}', '').replace('{denSimp}', '')
    old = old.replace('{pct}', '').replace('{{ de }}', '{ de }').replace('{base}', '')
    old = old.replace('{p1}', '').replace('{p2}', '').replace('{numAns}', '').replace('{denAns}', '')
    old = old.replace('{exp}', '').replace('{inside}', '').replace('{a}', '').replace('{bStr}', '').replace('{sign}', '').replace('{c}', '').replace('{x}', '').replace('{result}', '')
    old = old.replace('{argument}', '').replace('{num1}', '').replace('{num2}', '')
    old = old.replace('{signA}', '').replace('{cStr}', '')
    
    new = new.replace('f"', '"').replace('{num}', '').replace('{den}', '').replace('{decimalStr}', '').replace('{numSimp}', '').replace('{denSimp}', '')
    new = new.replace('{pct}', '').replace('{{ de }}', '{ de }').replace('{base}', '')
    new = new.replace('{p1}', '').replace('{p2}', '').replace('{numAns}', '').replace('{denAns}', '')
    new = new.replace('{exp}', '').replace('{inside}', '').replace('{a}', '').replace('{bStr}', '').replace('{sign}', '').replace('{c}', '').replace('{x}', '').replace('{result}', '')
    new = new.replace('{argument}', '').replace('{num1}', '').replace('{num2}', '')
    new = new.replace('{signA}', '').replace('{cStr}', '')
    
    new_content = new_content.replace(old, new)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Patch applied.")
