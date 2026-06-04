import logging
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.exc import SQLAlchemyError

from api.activity import router as activity_router
from api.calibration import router as calibration_router
from api.drill import router as drill_router
from api.exercise_generation import router as exercise_generation_router
from api.export import router as export_router
from api.health import router as health_router
from api.identify_topic import router as identify_topic_router
from api.ml_data import router as ml_data_router
from api.profile import router as profile_router
from api.ranking import router as ranking_router
from api.rhythm import router as rhythm_router
from api.session import router as session_router
from api.submit import router as submit_router
from api.telemetry import router as telemetry_router
from db import Base, engine, settings
import models  # noqa: F401

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI, run_startup_db: bool = True) -> AsyncGenerator[None, None]:
    if run_startup_db:
        try:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
        except SQLAlchemyError:
            logger.exception("Database startup check failed")
            raise
    yield


def create_app(run_startup_db: bool = True) -> FastAPI:
    app = FastAPI(title="LOVE CLASS API", lifespan=lambda app: lifespan(app, run_startup_db))

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(telemetry_router)
    app.include_router(health_router)
    app.include_router(export_router)
    app.include_router(session_router)
    app.include_router(submit_router)
    app.include_router(activity_router)
    app.include_router(calibration_router)
    app.include_router(drill_router)
    app.include_router(exercise_generation_router)
    app.include_router(identify_topic_router)
    app.include_router(ml_data_router)
    app.include_router(profile_router)
    app.include_router(ranking_router)
    app.include_router(rhythm_router)

    @app.get("/")
    def read_root() -> dict[str, str]:
        return {"status": "Math Ink Vector Engine is running (Thin Client Architecture)"}

    return app


app = create_app(run_startup_db=settings.auto_create_tables)


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
