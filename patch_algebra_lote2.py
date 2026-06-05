import os

file_path = r"app\src\main\java\com\strava_matematica\domain\procedural\ProceduralAlgebra.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix the invalid f-strings injected by the previous script
content = content.replace('f"Converta a fração', '"Converta a fração')
content = content.replace('f"Converta o número decimal', '"Converta o número decimal')
content = content.replace('f"/"', '"/"')
content = content.replace('f"Calcule o valor de', '"Calcule o valor de')
content = content.replace('f"Simplifique a razão', '"Simplifique a razão')
content = content.replace('f"/"', '"/"')
content = content.replace('f"Calcule o valor da potência', '"Calcule o valor da potência')
content = content.replace('f"Calcule o valor da raiz quadrada', '"Calcule o valor da raiz quadrada')
content = content.replace('f"Resolva a inequação', '"Resolva a inequação')
content = content.replace('f"x  "', '"x  "')
content = content.replace('f"Dada a função afim', '"Dada a função afim')
content = content.replace('f"Dada a função quadrática', '"Dada a função quadrática')
content = content.replace('f"Resolva a equação exponencial', '"Resolva a equação exponencial')
content = content.replace('f"Calcule o valor do logaritmo', '"Calcule o valor do logaritmo')
content = content.replace('f", "', '", "')
content = content.replace('f"Resolva a equação modular', '"Resolva a equação modular')

# Fix holes that didn't get replaced because of exact match issues:
content = content.replace(
    'statement = "Simplifique a razão para a forma irredutível a/b:\\n\\n\\\\( \\\\frac{}{} \\\\)"',
    'statement = "Simplifique a razão para a forma irredutível a/b:\\n\\n\\\\( \\\\frac{}{} \\\\)"'
)
content = content.replace(
    'expectedAnswer = "/"',
    'expectedAnswer = "/"'
)
content = content.replace(
    'statement = "Calcule o valor da potência:\\n\\n\\\\( ^{{}} \\\\)"',
    'statement = "Calcule o valor da potência:\\n\\n\\\\( ^{} \\\\)"'
)
content = content.replace(
    'statement = "Calcule o valor da raiz quadrada:\\n\\n\\\\( \\\\sqrt{{}} \\\\)"',
    'statement = "Calcule o valor da raiz quadrada:\\n\\n\\\\( \\\\sqrt{} \\\\)"'
)
content = content.replace(
    'statement = "Resolva a inequação para x:\\n\\n\\\\( x {abs(b)}   \\\\)",\n            expectedAnswer = "x  ",',
    'statement = "Resolva a inequação para x:\\n\\n\\\\( x    \\\\)",\n            expectedAnswer = "x  ",'
)
content = content.replace(
    'statement = "Dada a função afim \\\\( f(x) = x {abs(b)} \\\\), calcule o valor de \\\\( f() \\\\)."',
    'statement = "Dada a função afim \\\\( f(x) = x  \\\\), calcule o valor de \\\\( f() \\\\)."'
)
content = content.replace(
    'statement = "Dada a função quadrática \\\\( f(x) = x^2   \\\\), encontre a coordenada \\\\( y \\\\) do vértice (ou seja, o valor máximo/mínimo da função).".replace("  ", " "),',
    'statement = "Dada a função quadrática \\\\( f(x) = x^2   \\\\), encontre a coordenada \\\\( y \\\\) do vértice (ou seja, o valor máximo/mínimo da função).".replace("  ", " "),'
)
content = content.replace(
    'statement = "Resolva a equação exponencial para x:\\n\\n\\\\( ^x =  \\\\)",',
    'statement = "Resolva a equação exponencial para x:\\n\\n\\\\( ^x =  \\\\)",'
)
content = content.replace(
    'statement = "Calcule o valor do logaritmo:\\n\\n\\\\( \\\\log_{{}}() \\\\)",',
    'statement = "Calcule o valor do logaritmo:\\n\\n\\\\( \\\\log_{}() \\\\)",'
)
content = content.replace(
    'val expected = ", "\n        return ProceduralExercise(\n            id = UUID.randomUUID().toString(),\n            statement = "Resolva a equação modular e encontre os valores de x:\\n\\n\\\\( |x {abs(b)}| =  \\\\)",',
    'val expected = ", "\n        return ProceduralExercise(\n            id = UUID.randomUUID().toString(),\n            statement = "Resolva a equação modular e encontre os valores de x:\\n\\n\\\\( |x | =  \\\\)",'
)
content = content.replace(
    'statement = "Converta a fração para número decimal:\\n\\n\\\\( \\\\frac{}{} \\\\)"',
    'statement = "Converta a fração para número decimal:\\n\\n\\\\( \\\\frac{}{} \\\\)"'
)
content = content.replace(
    'statement = "Converta o número decimal para fração irredutível (ex: a/b):\\n\\n\\\\(  \\\\)"',
    'statement = "Converta o número decimal para fração irredutível (ex: a/b):\\n\\n\\\\(  \\\\)"'
)
content = content.replace(
    'statement = "Calcule o valor de:\\n\\n\\\\( \\\\% \\\\text{ de }  \\\\)"',
    'statement = "Calcule o valor de:\\n\\n\\\\( \\\\% \\\\text{ de }  \\\\)"'
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied.")
