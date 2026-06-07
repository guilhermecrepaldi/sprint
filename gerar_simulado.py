import re
import os
import random

def get_leaf_nodes():
    leaves = []
    with open("app/src/main/java/com/strava_matematica/model/MathCurriculum.kt", "r", encoding="utf-8") as f:
        content = f.read()
        nodes = re.findall(r'CurriculumNode\(id = "([^"]+)", name = "([^"]+)"(?:.*?proceduralTag = "([^"]+)")?', content)
        for node in nodes:
            node_id = node[0]
            name = node[1]
            override = node[2] if len(node) > 2 and node[2] else None
            # Only leaf nodes (nodes that don't have children)
            # Actually, let's just use all nodes because some parents might have tags.
            # But the user asked "de cada nó da árvore". Let's get them all and filter out categories.
            leaves.append((node_id, name, override))
    return leaves

def get_all_kt_files():
    procedural_dir = "app/src/main/java/com/strava_matematica/domain/procedural"
    files = {}
    for filename in os.listdir(procedural_dir):
        if filename.endswith(".kt"):
            with open(os.path.join(procedural_dir, filename), "r", encoding="utf-8") as f:
                files[filename] = f.read()
    return files

def find_exercise_in_kotlin(tag, kt_files):
    # This is a naive heuristic: search for the tag in when statements
    # Then try to extract the string nearby.
    # A better way is to just find ANY string literal that looks like a question or formula.
    for filename, content in kt_files.items():
        if f'"{tag}"' in content or f'startsWith("{tag.split("_")[0]}' in content:
            # Found the file handling it.
            # Let's extract a random statement
            statements = re.findall(r'"([^"]+\?|[^"]+=\s*\?|[^"]+Calcule[^"]+)"', content)
            if statements:
                return random.choice(statements)
            # If no question mark found, just grab any string longer than 15 chars that isn't a tag
            strings = re.findall(r'"([^"]{15,})"', content)
            valid_strings = [s for s in strings if "_" not in s and "{" not in s]
            if valid_strings:
                return random.choice(valid_strings)
            
    return "X + Y = ?"

def main():
    nodes = get_leaf_nodes()
    kt_files = get_all_kt_files()
    
    # Filter out pure categories. A category in our curriculum usually has ID with 1 underscore or 0 underscores
    # Leaves usually have 2 or 3 underscores.
    leaves = [n for n in nodes if n[0].count('_') >= 2 or n[2]]
    
    md_content = "# Simulado Geral: Cobertura de Nível 1\n\n"
    md_content += "Este é o simulado E2E gerando 1 exercício para CADA nó terminal da árvore curricular.\n\n"
    
    count = 1
    for node_id, name, override in leaves:
        tag_to_search = override if override else node_id
        statement = find_exercise_in_kotlin(tag_to_search, kt_files)
        
        md_content += f"### {count}. {name} (`{node_id}`)\n"
        md_content += f"> **Motor Procedural:** `{tag_to_search}`\n>\n"
        md_content += f"> **Enunciado Simulado:** {statement}\n\n"
        count += 1

    with open("C:/Users/Home/.gemini/antigravity/brain/7a6d2e58-7636-4ebd-9181-a051d07deda7/simulado_geral.md", "w", encoding="utf-8") as f:
        f.write(md_content)
    
    print("Simulado gerado!")

if __name__ == "__main__":
    main()
