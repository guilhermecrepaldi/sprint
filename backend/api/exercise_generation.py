import asyncio
import json
import logging
import os

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field, model_validator

from db import settings
from scripts.generate_exercises import (
    DEFAULT_GEMINI_MODEL,
    DEFAULT_OLLAMA_BASE_URL,
    DEFAULT_OLLAMA_MODEL,
    DEFAULT_OPENAI_MODEL,
    GeminiExerciseGenerator,
    OllamaExerciseGenerator,
    OpenAIExerciseGenerator,
    SKILL_DICT,
    ExerciseGenerator,
    insert_exercises,
)

logger = logging.getLogger(__name__)
router = APIRouter()


class ExerciseGenerationRequest(BaseModel):
    skills: list[str] | None = Field(default=None, min_length=1)
    all: bool = False
    count: int = Field(default=10, ge=1, le=200)
    batch_size: int = Field(default=25, ge=1, le=50)
    dry_run: bool = False
    model: str | None = None

    @model_validator(mode="after")
    def validate_skill_selection(self) -> "ExerciseGenerationRequest":
        if self.all and self.skills:
            raise ValueError("Use all=true ou skills, nao ambos")
        if not self.all and not self.skills:
            raise ValueError("Informe skills ou all=true")
        if self.skills:
            unknown = [skill for skill in self.skills if skill not in SKILL_DICT]
            if unknown:
                raise ValueError(f"Skills desconhecidas: {unknown}")
        return self


class GeneratedExercisePreview(BaseModel):
    statement: str
    expected_answer: str
    difficulty: float


class SkillGenerationResult(BaseModel):
    skill: str
    generated: int
    inserted: int = 0
    duplicates: int = 0
    errors: list[str] = Field(default_factory=list)
    preview: list[GeneratedExercisePreview] = Field(default_factory=list)


class ExerciseGenerationResponse(BaseModel):
    provider: str
    model: str
    dry_run: bool
    total_generated: int
    total_inserted: int
    total_duplicates: int
    total_errors: int
    results: list[SkillGenerationResult]


@router.post("/api/exercises/generate/openai", response_model=ExerciseGenerationResponse)
async def generate_openai_exercises(
    body: ExerciseGenerationRequest,
) -> ExerciseGenerationResponse:
    api_key = settings.openai_api_key or os.environ.get("OPENAI_API_KEY", "")
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="OPENAI_API_KEY nao configurada",
        )

    model = body.model or settings.openai_model or DEFAULT_OPENAI_MODEL
    generator = OpenAIExerciseGenerator(api_key=api_key, model=model)
    return await _generate_exercises("openai", model, generator, body)


@router.post("/api/exercises/generate/gemini", response_model=ExerciseGenerationResponse)
async def generate_gemini_exercises(
    body: ExerciseGenerationRequest,
) -> ExerciseGenerationResponse:
    api_key = (
        getattr(settings, "gemini_api_key", "")
        or os.environ.get("GEMINI_API_KEY", "")
        or os.environ.get("GOOGLE_API_KEY", "")
    )
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="GEMINI_API_KEY nao configurada",
        )

    model = body.model or getattr(settings, "gemini_model", "") or DEFAULT_GEMINI_MODEL
    generator = GeminiExerciseGenerator(api_key=api_key, model=model)
    return await _generate_exercises("gemini", model, generator, body)


@router.post("/api/exercises/generate/local", response_model=ExerciseGenerationResponse)
async def generate_local_exercises(
    body: ExerciseGenerationRequest,
) -> ExerciseGenerationResponse:
    model = body.model or settings.ollama_model or DEFAULT_OLLAMA_MODEL
    base_url = settings.ollama_base_url or DEFAULT_OLLAMA_BASE_URL
    generator = OllamaExerciseGenerator(model=model, base_url=base_url)
    return await _generate_exercises("ollama", model, generator, body)


async def _generate_exercises(
    provider: str,
    model: str,
    generator: ExerciseGenerator,
    body: ExerciseGenerationRequest,
) -> ExerciseGenerationResponse:
    skills = list(SKILL_DICT.keys()) if body.all else list(body.skills or [])
    results: list[SkillGenerationResult] = []

    for skill in skills:
        result = SkillGenerationResult(skill=skill, generated=0)
        remaining = body.count
        items: list[dict] = []

        while remaining > 0:
            batch_count = min(body.batch_size, remaining)
            try:
                batch = await asyncio.to_thread(generator.generate, skill, batch_count)
            except (json.JSONDecodeError, KeyError, RuntimeError, ValueError) as exc:
                logger.warning("%s exercise generation failed for %s: %s", provider, skill, exc)
                result.errors.append(str(exc))
                break

            items.extend(batch)
            remaining -= batch_count

        result.generated = len(items)
        if body.dry_run:
            result.preview = [GeneratedExercisePreview(**item) for item in items]
        elif items:
            inserted, duplicates = await insert_exercises(skill, items)
            result.inserted = inserted
            result.duplicates = duplicates

        results.append(result)

    return ExerciseGenerationResponse(
        provider=provider,
        model=model,
        dry_run=body.dry_run,
        total_generated=sum(result.generated for result in results),
        total_inserted=sum(result.inserted for result in results),
        total_duplicates=sum(result.duplicates for result in results),
        total_errors=sum(len(result.errors) for result in results),
        results=results,
    )
