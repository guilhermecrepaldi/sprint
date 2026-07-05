"""
Seed das 7 trilhas curriculares (Tracks).

Uso:
    cd backend
    python seed/tracks.py

Idempotente: não duplica se já existir.
"""
import asyncio
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from db import AsyncSessionLocal
from models.track import Track
from sqlalchemy import select

TRACKS = [
    {
        "slug": "fundamentos",
        "name": "Trilha de Fundamentos",
        "description": "Aritmética, frações e porcentagem. A base de tudo.",
        "display_order": 1,
        "skill_tags": [
            "soma_subtracao",
            "multiplicacao_divisao",
            "fracoes_decimais",
            "porcentagem_razao",
            "potenciacao_radiciacao",
        ],
    },
    {
        "slug": "algebra",
        "name": "Trilha de Álgebra",
        "description": "Equações, sistemas e fatoração.",
        "display_order": 2,
        "skill_tags": [
            "equacoes_lineares",
            "sistemas_equacoes",
            "fatoracao_produtos_notaveis",
            "inequacoes",
            "equacoes_quadraticas",
        ],
    },
    {
        "slug": "funcoes",
        "name": "Trilha de Funções",
        "description": "Funções afim, quadrática, exponencial, logarítmica e modular.",
        "display_order": 3,
        "skill_tags": [
            "funcao_afim",
            "funcao_quadratica",
            "funcao_exponencial",
            "funcao_logaritmica",
            "funcao_modular",
        ],
    },
    {
        "slug": "geometria",
        "name": "Trilha de Geometria",
        "description": "Geometria plana, espacial e analítica.",
        "display_order": 4,
        "skill_tags": [
            "geometria_plana",
            "geometria_espacial",
            "geometria_analitica",
        ],
    },
    {
        "slug": "combinatoria",
        "name": "Trilha de Combinatória e Probabilidade",
        "description": "PA, PG, análise combinatória e probabilidade clássica.",
        "display_order": 5,
        "skill_tags": [
            "progressoes_pa_pg",
            "combinatoria",
            "probabilidade",
        ],
    },
    {
        "slug": "trigonometria",
        "name": "Trilha de Trigonometria",
        "description": "Razões, ciclo, identidades e equações trigonométricas.",
        "display_order": 6,
        "skill_tags": [
            "trig_razoes",
            "trig_seno_cosseno_tangente",
            "trig_identidades",
            "trig_equacoes",
        ],
    },
    {
        "slug": "calculo",
        "name": "Trilha de Cálculo",
        "description": "Limites, derivadas e integrais.",
        "display_order": 7,
        "skill_tags": [
            "nocao_de_limite",
            "continuidade",
            "derivadas_basicas",
            "derivadas_regra_cadeia",
            "derivadas_produto_quociente",
            "aplicacoes_derivadas",
            "integrais_indefinidas",
            "integrais_definidas",
            "aplicacoes_integrais",
        ],
    },
]


async def seed() -> None:
    async with AsyncSessionLocal() as db:
        existing = await db.execute(select(Track.slug))
        existing_slugs = set(existing.scalars().all())

        new_tracks = [
            Track(**t)
            for t in TRACKS
            if t["slug"] not in existing_slugs
        ]

        if new_tracks:
            db.add_all(new_tracks)
            await db.commit()
            print(f"Inseridas {len(new_tracks)} trilhas.")
        else:
            print("Trilhas ja existem — nada a inserir.")


if __name__ == "__main__":
    asyncio.run(seed())
