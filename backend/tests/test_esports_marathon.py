import pytest
import uuid
from engine.scoring import compute_score
from models.session import SessionConfig
import api.submit as submit_module
from tests.fakes import FakeAsyncSession
from api.session import start_session
from api.submit import submit_folha
from schemas.session import SessionConfigIn, SessionStartIn
from schemas.submit import SubmitIn, FieldSubmit, PenEventIn

def test_time_decay_penalizes_slow_responses():
    fast_score = compute_score(True, 15000, 1000, 2.0, 30000)
    slow_score = compute_score(True, 120000, 50000, 2.0, 30000)
    assert fast_score > slow_score
    assert slow_score < 600

def test_difficulty_multiplier_rewards_hard_problems_above_1000():
    base_score = compute_score(True, 10000, 0, 1.0, 30000)
    hard_score = compute_score(True, 10000, 0, 10.0, 30000)
    assert base_score == 1000
    assert hard_score == 1900

def test_anti_cheat_bot_instant_response():
    bot_score = compute_score(True, 200, 0, 5.0, 30000)
    assert bot_score == 0

async def fake_extract_answer(image_base64: str) -> dict:
    return {"answer_latex": image_base64.split(":", 1)[1], "confidence": 1.0}

@pytest.mark.asyncio
async def test_endurance_marathon_simulated_flow():
    _original_extract_answer = submit_module.extract_answer
    submit_module.extract_answer = fake_extract_answer
    
    try:
        db = FakeAsyncSession()
        db.seed_exercises()
        
        student_id = uuid.uuid4()
        start = await start_session(
            SessionStartIn(
                student_id=student_id,
                config=SessionConfigIn(
                    duration_mode="unlimited",
                    exercises_per_page=3
                )
            ),
            db=db
        )
        
        current_folha_id = start.first_folha.folha_id
        current_fields = start.first_folha.fields
        session_id = start.session_id
        
        for i in range(100):
            body = SubmitIn(
                folha_id=current_folha_id,
                submitted_at_ms=123456,
                fields=[
                    FieldSubmit(
                        field_index=f.field_index,
                        exercise_id=f.exercise_id,
                        image_base64="latex:x=5",
                        total_time_ms=5000,
                        time_to_first_stroke_ms=500,
                        pen_events=[]
                    ) for f in current_fields
                ]
            )
            
            submit_res = await submit_folha(session_id, body, db=db)
            
            assert submit_res.session_status == "active"
            assert submit_res.next_folha is not None
            assert len(submit_res.next_folha.fields) == 3
            
            current_folha_id = submit_res.next_folha.folha_id
            current_fields = submit_res.next_folha.fields
            
    finally:
        submit_module.extract_answer = _original_extract_answer
