from pydantic import BaseModel


class CharSample(BaseModel):
    expected_char: str          # "7", "4", "x", "+"
    image_base64: str           # PNG base64 da caixa de resposta


class CalibrationIn(BaseModel):
    samples: list[CharSample]


class CharCalibrationResult(BaseModel):
    char: str
    recognized: str | None
    correct: bool
    confidence: float


class CalibrationOut(BaseModel):
    results: list[CharCalibrationResult]
    weak_chars: list[str]       # chars com correct=False
    overall_score: float        # fração de acertos (0.0–1.0)
