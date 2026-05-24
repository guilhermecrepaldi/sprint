from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from engine.ocr import extract_answer
from schemas.calibration import CalibrationIn, CalibrationOut, CharCalibrationResult

router = APIRouter()


@router.post("/api/student/{student_id}/calibrate", response_model=CalibrationOut)
async def calibrate_handwriting(
    student_id: str,
    body: CalibrationIn,
    db: AsyncSession = Depends(get_db),
):
    results = []
    for sample in body.samples:
        try:
            ocr = await extract_answer(sample.image_base64)
            recognized = ocr.get("answer_latex", "")
            confidence = float(ocr.get("confidence", 0.0))
            correct = (recognized or "").strip() == sample.expected_char.strip()
        except Exception:
            recognized = None
            confidence = 0.0
            correct = False

        results.append(CharCalibrationResult(
            char=sample.expected_char,
            recognized=recognized,
            correct=correct,
            confidence=confidence,
        ))

    weak_chars = [r.char for r in results if not r.correct]
    overall = sum(1 for r in results if r.correct) / len(results) if results else 0.0

    return CalibrationOut(
        results=results,
        weak_chars=weak_chars,
        overall_score=round(overall, 2),
    )
