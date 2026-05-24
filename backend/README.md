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
If Docker Desktop is open but the engine is unavailable, start the Docker Desktop Service from an elevated Windows terminal and rerun `docker compose up -d`.
Set `AUTO_CREATE_TABLES=false` outside local development when Alembic owns schema changes.

## Unit Checks

```powershell
cd "D:\LOVE CLASS\backend"
python -m unittest
```

The suite includes user workflow tests for config -> first folha -> submit -> next folha, incomplete page rejection, duplicate submit rejection, and page-limit session finish.
It also includes HTTP-level workflow tests using FastAPI `TestClient` with an in-memory fake DB.

## Smoke Test

In another terminal, after the API is running:

```powershell
cd "D:\LOVE CLASS\backend"
python scripts/smoke_backend.py
```

The smoke test starts a session, submits one folha using the local `latex:` OCR fallback, and prints the response with results, thermometer, and next folha.
