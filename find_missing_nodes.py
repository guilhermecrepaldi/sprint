import re
import os

def get_leaf_nodes():
    leaves = []
    with open("app/src/main/java/com/strava_matematica/model/MathCurriculum.kt", "r", encoding="utf-8") as f:
        content = f.read()
        # Find all id = "tag"
        nodes = re.findall(r'CurriculumNode\(id = "([^"]+)", name = "([^"]+)"', content)
        # We only want leaf nodes. A leaf node is one that does not appear as a parent.
        # Actually, let's just collect all tags that don't match exactly "fnd", "alg", etc.
        # It's easier: any tag that has at least two underscores or we can just test all tags.
        for node_id, node_name in nodes:
            leaves.append((node_id, node_name))
    return leaves

def get_implemented_tags():
    implemented = set()
    procedural_dir = "app/src/main/java/com/strava_matematica/domain/procedural"
    for filename in os.listdir(procedural_dir):
        if filename.endswith(".kt"):
            with open(os.path.join(procedural_dir, filename), "r", encoding="utf-8") as f:
                content = f.read()
                # Find strings inside quotes that are used in when branches or if branches
                tags = re.findall(r'"([a-z_0-9]+)"', content)
                for t in tags:
                    implemented.add(t)
    return implemented

def main():
    leaves = get_leaf_nodes()
    implemented = get_implemented_tags()
    
    missing = []
    for node_id, name in leaves:
        if node_id not in implemented:
            # Maybe it's handled by a fallback prefix like 'comp_' ?
            # But the user asked to 'crie os exercicios' se faltar 1 de cada nó!
            # Which means they want SPECIFIC implementations, not generic fallbacks.
            missing.append((node_id, name))
            
    print(f"Total Nodes extracted: {len(leaves)}")
    print(f"Total Missing Specific Implementations: {len(missing)}")
    for m in missing:
        print(f"MISSING: {m[0]} - {m[1]}")

if __name__ == "__main__":
    main()
