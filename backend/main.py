import logging
from contextlib import asynccontextmanager
from collections.abc import AsyncGenerator

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.exc import SQLAlchemyError

from api.session import router as session_router
from api.submit import router as submit_router
from api.telemetry import router as telemetry_router
from db import Base, engine
import models  # noqa: F401

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    try:
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
    except SQLAlchemyError:
        logger.exception("Database startup check failed")
        raise
    yield


app = FastAPI(title="Strava da Matemática API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(telemetry_router)
app.include_router(session_router)
app.include_router(submit_router)


@app.get("/")
def read_root() -> dict[str, str]:
    return {"status": "Math Ink Vector Engine is running (Thin Client Architecture)"}


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
