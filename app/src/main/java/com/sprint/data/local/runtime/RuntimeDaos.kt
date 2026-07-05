package com.sprint.data.local.runtime

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(student: StudentEntity)
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("UPDATE sessions SET ended_at = :endedAt WHERE id = :sessionId")
    suspend fun close(sessionId: String, endedAt: Long)

    @Query(
        """
        SELECT
            s.id AS sessionId,
            s.skill_pin AS skill,
            s.density AS density,
            s.template_pin AS template,
            COUNT(a.id) AS exercisesDone,
            CASE WHEN COUNT(a.id) = 0 THEN 0 ELSE CAST((SUM(CASE WHEN a.is_correct THEN 1 ELSE 0 END) * 100) / COUNT(a.id) AS INTEGER) END AS accuracy,
            MAX(1, CAST(((COALESCE(s.ended_at, strftime('%s','now') * 1000) - s.started_at) / 60000) AS INTEGER)) AS durationMin,
            s.started_at AS startedAtMs,
            CASE WHEN s.ended_at IS NULL THEN 1 ELSE 0 END AS isActive
        FROM sessions s
        LEFT JOIN exercise_attempts a ON a.session_id = s.id
        WHERE s.student_id = :studentId
        GROUP BY s.id
        ORDER BY s.started_at DESC
        LIMIT :limit
        """,
    )
    suspend fun history(studentId: String, limit: Int = 80): List<SessionHistoryProjection>
}

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: ExerciseAttemptEntity): Long

    @Insert
    suspend fun insertPenEvents(events: List<PenEventEntity>)

    @Query("SELECT COUNT(*) FROM exercise_attempts WHERE student_id = :studentId")
    suspend fun totalAttempts(studentId: String): Int

    @Query(
        """
        SELECT date(attempt_timestamp / 1000, 'unixepoch', 'localtime') AS date, COUNT(*) AS count
        FROM exercise_attempts
        WHERE student_id = :studentId AND attempt_timestamp >= :fromMs
        GROUP BY date(attempt_timestamp / 1000, 'unixepoch', 'localtime')
        ORDER BY date
        """,
    )
    suspend fun activity(studentId: String, fromMs: Long): List<ActivityProjection>

    @Query("SELECT * FROM exercise_attempts WHERE student_id = :studentId AND skill = :skill ORDER BY attempt_timestamp DESC LIMIT :limit")
    suspend fun getRecentAttemptsForSkill(studentId: String, skill: String, limit: Int): List<ExerciseAttemptEntity>
}

@Dao
interface SkillMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: StudentSkillMemoryEntity)

    @Query("SELECT * FROM student_skill_memory WHERE student_id = :studentId AND skill = :skill LIMIT 1")
    suspend fun get(studentId: String, skill: String): StudentSkillMemoryEntity?

    @Query("SELECT * FROM student_skill_memory WHERE student_id = :studentId")
    suspend fun all(studentId: String): List<StudentSkillMemoryEntity>
}

data class SessionHistoryProjection(
    val sessionId: String,
    val skill: String,
    val density: String,
    val template: String?,
    val exercisesDone: Int,
    val accuracy: Int,
    val durationMin: Int,
    val startedAtMs: Long,
    val isActive: Boolean,
)

data class ActivityProjection(
    val date: String,
    val count: Int,
)

@Dao
interface StudyPlanDao {
    @Insert
    suspend fun insert(plan: StudyPlanEntity)

    @Update
    suspend fun update(plan: StudyPlanEntity)

    @Query("SELECT * FROM study_plans WHERE studentId = :studentId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActivePlan(studentId: String): StudyPlanEntity?
}
