"""
Unlock Engine — LOVE CLASS
==========================
Define os requisitos para desbloquear o próximo nível de dificuldade
ou o próximo tópico/matéria na árvore de habilidades.

REGRA CANÔNICA (decisão de produto 2026-05-25):
  Para avançar qualquer nó da árvore, o aluno precisa de AMBOS:
    1. Domínio (mastery) ≥ 90%  no(s) pré-requisito(s)
    2. Mínimo de 100 exercícios concluídos naquele tópico

  Nenhuma exceção. Não importa a velocidade ou a pontuação.
  O volume mínimo garante repetição real, não cramming.
"""

from __future__ import annotations

from dataclasses import dataclass, field

# ── Constantes canônicas ──────────────────────────────────────────────────────

MASTERY_THRESHOLD: float = 0.90   # 90% de domínio
MIN_EXERCISES: int = 100           # mínimo de exercícios concluídos por tópico


# ── Árvore de pré-requisitos ─────────────────────────────────────────────────

# Formato: { "tópico_alvo": ["pré-req-1", "pré-req-2", ...] }
# Todos os pré-requisitos devem ter MASTERY_THRESHOLD + MIN_EXERCISES.

PREREQUISITE_TREE: dict[str, list[str]] = {
    # Aritmética → Álgebra
    "multiplicacao_divisao":        ["soma_subtracao"],
    "fracoes_decimais":             ["multiplicacao_divisao"],
    "porcentagem_razao":            ["fracoes_decimais"],
    "potenciacao_radiciacao":       ["multiplicacao_divisao"],

    # Álgebra básica
    "equacoes_lineares":            ["fracoes_decimais", "potenciacao_radiciacao"],
    "sistemas_equacoes":            ["equacoes_lineares"],
    "fatoracao_produtos_notaveis":  ["equacoes_lineares"],
    "equacoes_quadraticas":         ["equacoes_lineares"],      # ← 90% + 100 ex
    "inequacoes":                   ["equacoes_lineares"],

    # Funções
    "funcao_afim":                  ["equacoes_lineares"],
    "funcao_quadratica":            ["equacoes_quadraticas", "funcao_afim"],
    "funcao_exponencial":           ["potenciacao_radiciacao", "funcao_afim"],
    "funcao_logaritmica":           ["funcao_exponencial"],
    "funcao_modular":               ["inequacoes"],

    # Trigonometria
    "trig_razoes":                  ["funcao_quadratica"],
    "trig_seno_cosseno_tangente":   ["trig_razoes"],
    "trig_identidades":             ["trig_seno_cosseno_tangente"],
    "trig_equacoes":                ["trig_identidades"],

    # Geometria
    "geometria_plana":              ["fracoes_decimais"],
    "geometria_espacial":           ["geometria_plana"],
    "geometria_analitica":          ["funcao_afim", "geometria_plana"],

    # Sequências e combinatória
    "progressoes_pa_pg":            ["funcao_afim", "funcao_quadratica"],
    "combinatoria":                 ["fracoes_decimais"],
    "probabilidade":                ["combinatoria"],

    # Pré-Cálculo
    "nocao_de_limite":              [            # ← 90% + 100 ex em AMBOS
        "funcao_logaritmica",
        "trig_identidades",
    ],
    "continuidade":                 ["nocao_de_limite"],

    # Cálculo Diferencial
    "derivadas_basicas":            ["nocao_de_limite"],        # ← 90% + 100 ex
    "derivadas_regra_cadeia":       ["derivadas_basicas"],
    "derivadas_produto_quociente":  ["derivadas_basicas"],
    "aplicacoes_derivadas":         [
        "derivadas_regra_cadeia",
        "derivadas_produto_quociente",
    ],

    # Cálculo Integral
    "integrais_indefinidas":        ["aplicacoes_derivadas"],
    "integrais_definidas":          ["integrais_indefinidas"],
    "aplicacoes_integrais":         ["integrais_definidas"],
}


# ── Dataclass de resultado ────────────────────────────────────────────────────

@dataclass
class UnlockStatus:
    unlocked: bool
    missing_mastery: list[str] = field(default_factory=list)   # pré-reqs abaixo de 90%
    missing_exercises: list[str] = field(default_factory=list) # pré-reqs abaixo de 100 ex
    progress: dict[str, dict] = field(default_factory=dict)    # detalhes por pré-req


# ── Função principal ──────────────────────────────────────────────────────────

def check_unlock(
    target_skill: str,
    skill_memory: dict[str, dict],
) -> UnlockStatus:
    """
    Verifica se o aluno pode desbloquear `target_skill`.

    `skill_memory` é um dict keyed por skill_tag com campos:
        accuracy: float        (mastery 0.0–1.0)
        attempt_count: int     (total de exercícios concluídos)

    Retorna UnlockStatus com detalhes do que falta.
    """
    prereqs = PREREQUISITE_TREE.get(target_skill, [])

    if not prereqs:
        # Tópico raiz (ex: soma_subtracao) — sempre disponível
        return UnlockStatus(unlocked=True)

    missing_mastery: list[str] = []
    missing_exercises: list[str] = []
    progress: dict[str, dict] = {}

    for prereq in prereqs:
        mem = skill_memory.get(prereq, {})
        mastery = mem.get("accuracy", 0.0)
        attempts = mem.get("attempt_count", 0)

        progress[prereq] = {
            "mastery": mastery,
            "mastery_needed": MASTERY_THRESHOLD,
            "exercises": attempts,
            "exercises_needed": MIN_EXERCISES,
            "mastery_ok": mastery >= MASTERY_THRESHOLD,
            "exercises_ok": attempts >= MIN_EXERCISES,
        }

        if mastery < MASTERY_THRESHOLD:
            missing_mastery.append(prereq)
        if attempts < MIN_EXERCISES:
            missing_exercises.append(prereq)

    unlocked = not missing_mastery and not missing_exercises
    return UnlockStatus(
        unlocked=unlocked,
        missing_mastery=missing_mastery,
        missing_exercises=missing_exercises,
        progress=progress,
    )


def get_available_skills(skill_memory: dict[str, dict]) -> list[str]:
    """
    Retorna todos os tópicos que o aluno já pode praticar agora
    (pré-requisitos satisfeitos ou tópico raiz).
    """
    return [
        skill for skill in PREREQUISITE_TREE
        if check_unlock(skill, skill_memory).unlocked
    ]
