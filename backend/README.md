# Strava da Matemática Backend

## Dev Run

```powershell
cd "D:\LOVE CLASS\backend"
python -m pip install -r requirements.txt
docker compose up -d
python -m alembic upgrade head
python seed/exercises.py
python -m uvicorn main:app --reload
```

Then open `http://localhost:8000/api/health`.

## Unit Checks

```powershell
cd "D:\LOVE CLASS\backend"
python -m unittest
```

## Smoke Test

In another terminal, after the API is running:

```powershell
cd "D:\LOVE CLASS\backend"
python scripts/smoke_backend.py
```

The smoke test starts a session, submits one folha using the local `latex:` OCR fallback, and prints the response with results, thermometer, and next folha.
