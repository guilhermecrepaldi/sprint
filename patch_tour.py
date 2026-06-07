import os
import re

base_dir = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica"

# 1. Update LocalSprintRepository.kt
repo_path = os.path.join(base_dir, "data", "local", "repository", "LocalSprintRepository.kt")
with open(repo_path, "r", encoding="utf-8") as f:
    repo_content = f.read()

repo_patch = """
        val actualSkillTag = if (skillTag == "curriculum_tour") {
            com.strava_matematica.model.MathCurriculum.nodes[pageIndex % com.strava_matematica.model.MathCurriculum.nodes.size].id
        } else {
            skillTag
        }

        if (!config.simuladoRulesJson.isNullOrEmpty()) {"""
repo_content = repo_content.replace("if (!config.simuladoRulesJson.isNullOrEmpty()) {", repo_patch)

repo_patch2 = """
        // Modo Padrão (Fallback)
        if (fields.isEmpty()) {
            val count = if (skillTag == "curriculum_tour") 3 else config.exercisesPerPage.coerceAtLeast(1)
            for (i in 0 until count) {
                val exercise = selectExercise(studentId, actualSkillTag, config)"""
repo_content = re.sub(
    r'// Modo Padrão \(Fallback\)\s*if \(fields\.isEmpty\(\)\) \{\s*val count = config\.exercisesPerPage\.coerceAtLeast\(1\)\s*for \(i in 0 until count\) \{\s*val exercise = selectExercise\(studentId, skillTag, config\)',
    repo_patch2.strip(),
    repo_content
)

with open(repo_path, "w", encoding="utf-8") as f:
    f.write(repo_content)


# 2. Update CurriculumTreeSelector.kt
tree_path = os.path.join(base_dir, "ui", "folha", "CurriculumTreeSelector.kt")
with open(tree_path, "r", encoding="utf-8") as f:
    tree_content = f.read()

tree_patch = """
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (mode == SelectorMode.SINGLE_SELECTION) {
            Button(
                onClick = { onSingleSelected("curriculum_tour") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
            ) {
                Text("🚀 INICIAR TOUR COMPLETO (Linear de 0 a 1000)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        MathCurriculum.tree.forEach { domain ->"""
tree_content = tree_content.replace("""    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MathCurriculum.tree.forEach { domain ->""", tree_patch)

# Auto-expand logic in DomainSelectorNode
auto_expand = """
    val containsSelected = selectedSingleId != null && (domain.id == selectedSingleId || domain.children.any { it.id == selectedSingleId || it.proceduralTag == selectedSingleId })
    var expanded by remember { mutableStateOf(containsSelected) }
    
    // Atualiza o estado se o selectedSingleId mudar (Zoom Out vindo do sprint)
    LaunchedEffect(selectedSingleId) {
        if (containsSelected) expanded = true
    }
"""
tree_content = re.sub(r'var expanded by remember \{ mutableStateOf\(false\) \}', auto_expand.strip(), tree_content)

with open(tree_path, "w", encoding="utf-8") as f:
    f.write(tree_content)


# 3. Update SessionViewModel.kt
svm_path = os.path.join(base_dir, "viewmodel", "SessionViewModel.kt")
with open(svm_path, "r", encoding="utf-8") as f:
    svm_content = f.read()

zoom_out_fun = """
    fun zoomOutToTree(nodeId: String?) {
        _uiState.update { 
            it.copy(
                status = SessionStatus.DASHBOARD,
                selectedSkillTag = nodeId ?: "soma_subtracao"
            ) 
        }
    }

    fun startSessionFromDashboard() {"""
svm_content = svm_content.replace("fun startSessionFromDashboard() {", zoom_out_fun.strip() + "\n")
with open(svm_path, "w", encoding="utf-8") as f:
    f.write(svm_content)


# 4. Update FolhaScreen.kt
folha_path = os.path.join(base_dir, "ui", "folha", "FolhaScreen.kt")
with open(folha_path, "r", encoding="utf-8") as f:
    folha_content = f.read()

folha_patch = """
                    // Ícone 2: Árvore (Abre configurações/árvore atual)
                    IconButton(
                        onClick = { viewModel.zoomOutToTree(currentNode?.id) },
                        modifier = Modifier"""
folha_content = folha_content.replace("""
                    // Ícone 2: Árvore (Abre configurações/árvore atual)
                    IconButton(
                        onClick = { showSprintScrolls.value = true },
                        modifier = Modifier""", folha_patch.strip())

with open(folha_path, "w", encoding="utf-8") as f:
    f.write(folha_content)

print("Patch aplicado com sucesso!")
