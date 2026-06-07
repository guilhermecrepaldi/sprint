import os
import re

base_dir = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica"

# 1. Add getFlatNodes() to MathCurriculum.kt
mc_path = os.path.join(base_dir, "model", "MathCurriculum.kt")
with open(mc_path, "r", encoding="utf-8") as f:
    mc_content = f.read()

if "getFlatNodes" not in mc_content:
    mc_patch = """
    fun getFlatNodes(): List<CurriculumNode> {
        val result = mutableListOf<CurriculumNode>()
        fun recurse(nodes: List<CurriculumNode>) {
            for (node in nodes) {
                result.add(node)
                recurse(node.children)
            }
        }
        recurse(tree)
        return result
    }
}"""
    mc_content = mc_content.replace("}\n", mc_patch, 1) # Note: this might be risky if there are multiple top level classes, but there's just object MathCurriculum
    # Let's do a safer replace
    mc_content = re.sub(r'\}\s*$', mc_patch, mc_content)
    with open(mc_path, "w", encoding="utf-8") as f:
        f.write(mc_content)


# 2. Fix LocalSprintRepository.kt
repo_path = os.path.join(base_dir, "data", "local", "repository", "LocalSprintRepository.kt")
with open(repo_path, "r", encoding="utf-8") as f:
    repo_content = f.read()

repo_content = repo_content.replace(
    "com.strava_matematica.model.MathCurriculum.nodes[pageIndex % com.strava_matematica.model.MathCurriculum.nodes.size].id",
    "com.strava_matematica.model.MathCurriculum.getFlatNodes().let { nodes -> nodes[pageIndex % nodes.size].id }"
)
with open(repo_path, "w", encoding="utf-8") as f:
    f.write(repo_content)


# 3. Fix FolhaScreen.kt & MainActivity.kt
folha_path = os.path.join(base_dir, "ui", "folha", "FolhaScreen.kt")
with open(folha_path, "r", encoding="utf-8") as f:
    folha_content = f.read()

folha_content = folha_content.replace("viewModel.zoomOutToTree(currentNode?.id)", "onZoomOut(currentNode?.id)")
if "onZoomOut: (String?) -> Unit =" not in folha_content:
    folha_content = folha_content.replace("onEndSession: () -> Unit = {},", "onEndSession: () -> Unit = {},\n    onZoomOut: (String?) -> Unit = {},")

with open(folha_path, "w", encoding="utf-8") as f:
    f.write(folha_content)


main_path = os.path.join(base_dir, "MainActivity.kt")
with open(main_path, "r", encoding="utf-8") as f:
    main_content = f.read()

if "onZoomOut = {" not in main_content:
    main_content = main_content.replace("onAdvance = doAdvance,", "onAdvance = doAdvance,\n                                        onZoomOut = { sessionViewModel.zoomOutToTree(it) },")
    with open(main_path, "w", encoding="utf-8") as f:
        f.write(main_content)


# 4. Fix CurriculumTreeSelector.kt
tree_path = os.path.join(base_dir, "ui", "folha", "CurriculumTreeSelector.kt")
with open(tree_path, "r", encoding="utf-8") as f:
    tree_content = f.read()

# Restore var expanded inside DomainSelectorNode
tree_fix = """
    val containsSelected = selectedSingleId != null && (domain.id == selectedSingleId || domain.children.any { child -> child.id == selectedSingleId || child.proceduralTag == selectedSingleId })
    var expanded by remember { mutableStateOf(containsSelected) }
    
    LaunchedEffect(selectedSingleId) {
        if (containsSelected) expanded = true
    }
"""
tree_content = re.sub(
    r'val containsSelected = selectedSingleId != null && \(domain\.id == selectedSingleId \|\| domain\.children\.any \{ it\.id == selectedSingleId \|\| it\.proceduralTag == selectedSingleId \}\)\s*var expanded by remember \{ mutableStateOf\(containsSelected\) \}\s*// Atualiza o estado se o selectedSingleId mudar \(Zoom Out vindo do sprint\)\s*LaunchedEffect\(selectedSingleId\) \{\s*if \(containsSelected\) expanded = true\s*\}',
    tree_fix.strip(),
    tree_content
)
with open(tree_path, "w", encoding="utf-8") as f:
    f.write(tree_content)

print("Fixes applied!")
