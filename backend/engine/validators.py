from abc import ABC, abstractmethod

from engine.correction import validate_answer


class ValidatorInterface(ABC):
    @abstractmethod
    async def validate(self, recognized: str | None, expected: str | None) -> dict:
        ...


class SympyValidator(ValidatorInterface):
    async def validate(self, recognized: str | None, expected: str | None) -> dict:
        return validate_answer(recognized, expected or "")


class ExactMatchValidator(ValidatorInterface):
    async def validate(self, recognized: str | None, expected: str | None) -> dict:
        if not recognized:
            return {"is_correct": False, "error_type": "ocr_invalido"}
        is_correct = recognized.strip().casefold() == (expected or "").strip().casefold()
        return {"is_correct": is_correct, "error_type": None if is_correct else "exact_match"}


class AIValidator(ExactMatchValidator):
    pass


class RubricValidator(ExactMatchValidator):
    pass


def get_validator(validator_type: str | None) -> ValidatorInterface:
    validators: dict[str, ValidatorInterface] = {
        "sympy": SympyValidator(),
        "ai": AIValidator(),
        "rubric": RubricValidator(),
        "exact_match": ExactMatchValidator(),
    }
    return validators.get(validator_type or "sympy", validators["sympy"])
