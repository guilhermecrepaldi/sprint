import uuid
from datetime import datetime
from typing import List, Optional

from fastapi import APIRouter, Depends, BackgroundTasks, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from db import get_db
from models.session import Session, SessionConfig
from models.attempt import ExerciseAttempt

router = APIRouter()

# Schema for incoming offline Session
class SessionSync(BaseModel):
    id: str
    student_id: str
    started_at: int
    ended_at: Optional[int] = None
    skill_pin: str
    density: str
    template_pin: Optional[str] = None
    config_json: str

# Schema for incoming offline Attempt
class AttemptSync(BaseModel):
    id: int  # local sqlite id
    session_id: str
    student_id: str
    exercise_id: str
    skill: str
    is_correct: bool
    user_response: str
    expected_answer: str
    validator_type: str
    attempt_timestamp: int
    duration_seconds: int

class SyncPayload(BaseModel):
    sessions: List[SessionSync]
    attempts: List[AttemptSync]

async def process_sync_payload(payload: SyncPayload, db: AsyncSession):
    # Upsert Sessions
    for s_sync in payload.sessions:
        stmt = select(Session).where(Session.id == uuid.UUID(s_sync.id))
        result = await db.execute(stmt)
        existing_session = result.scalar_one_or_none()
        
        # We also need a dummy SessionConfig if not exist, to satisfy FK
        # Since offline app uses arbitrary config JSON, we can just insert a basic one or omit it if config_id is nullable
        # In models/session.py config_id is UUID | None, so we can omit it!
        
        if not existing_session:
            new_session = Session(
                id=uuid.UUID(s_sync.id),
                student_id=uuid.UUID(s_sync.student_id),
                started_at=datetime.fromtimestamp(s_sync.started_at / 1000.0),
                ended_at=datetime.fromtimestamp(s_sync.ended_at / 1000.0) if s_sync.ended_at else None,
                current_difficulty=5.0, # fallback since it's not in payload
                status="completed" if s_sync.ended_at else "active",
            )
            db.add(new_session)
        else:
            if s_sync.ended_at:
                existing_session.ended_at = datetime.fromtimestamp(s_sync.ended_at / 1000.0)
                existing_session.status = "completed"
    
    await db.flush()

    # Insert Attempts
    for a_sync in payload.attempts:
        # We need to guarantee we don't insert the same attempt twice.
        # Since offline attempt doesn't have a UUID, we can generate a deterministic UUID based on session_id and local id
        attempt_uuid = uuid.uuid5(uuid.UUID(a_sync.session_id), str(a_sync.id))
        
        stmt = select(ExerciseAttempt).where(ExerciseAttempt.id == attempt_uuid)
        result = await db.execute(stmt)
        existing_attempt = result.scalar_one_or_none()
        
        if not existing_attempt:
            new_attempt = ExerciseAttempt(
                id=attempt_uuid,
                session_id=uuid.UUID(a_sync.session_id),
                student_id=uuid.UUID(a_sync.student_id),
                exercise_id=uuid.UUID(a_sync.exercise_id),
                field_index=0,
                recognized_answer=a_sync.user_response,
                expected_answer=a_sync.expected_answer,
                is_correct=a_sync.is_correct,
                total_time_ms=a_sync.duration_seconds * 1000,
                created_at=datetime.fromtimestamp(a_sync.attempt_timestamp / 1000.0),
            )
            db.add(new_attempt)
            
    await db.commit()

@router.post("/api/telemetry/sync")
async def sync_telemetry(payload: SyncPayload, background_tasks: BackgroundTasks, db: AsyncSession = Depends(get_db)):
    """
    Receives lean offline telemetry (Sessions and ExerciseAttempts).
    Processes upsert in background to avoid blocking the Android client.
    """
    background_tasks.add_task(process_sync_payload, payload, db)
    return {"status": "ok", "message": "Sync queued successfully"}


# Legacy WebSocket (kept for existing Ghost Racing tests if any)
class ConnectionManager:
    def __init__(self) -> None:
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket) -> None:
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket) -> None:
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

manager = ConnectionManager()

@router.websocket("/api/telemetry/stream")
async def telemetry_stream(websocket: WebSocket) -> None:
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
            await websocket.send_text("Vector received. Ghost Racing active.")
    except WebSocketDisconnect:
        manager.disconnect(websocket)
