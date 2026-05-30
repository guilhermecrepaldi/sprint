package com.strava_matematica.data.local.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("student_id"), Index("started_at")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "skill_pin") val skillPin: String,
    val density: String,
    @ColumnInfo(name = "template_pin") val templatePin: String?,
    @ColumnInfo(name = "config_json") val configJson: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
)

@Entity(
    tableName = "exercise_attempts",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id"), Index("student_id"), Index("exercise_id"), Index("skill"), Index("attempt_timestamp")],
)
data class ExerciseAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val skill: String,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean,
    @ColumnInfo(name = "user_response") val userResponse: String,
    @ColumnInfo(name = "expected_answer") val expectedAnswer: String,
    @ColumnInfo(name = "validator_type") val validatorType: String,
    @ColumnInfo(name = "attempt_timestamp") val attemptTimestamp: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
)

@Entity(
    tableName = "pen_events",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attempt_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("attempt_id")],
)
data class PenEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "attempt_id") val attemptId: Long,
    val x: Float,
    val y: Float,
    val pressure: Float?,
    @ColumnInfo(name = "event_type") val eventType: String,
    val timestamp: Long,
)

@Entity(
    tableName = "student_skill_memory",
    primaryKeys = ["student_id", "skill"],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("student_id")],
)
data class StudentSkillMemoryEntity(
    @ColumnInfo(name = "student_id") val studentId: String,
    val skill: String,
    @ColumnInfo(name = "master_score") val masterScore: Double,
    @ColumnInfo(name = "total_attempts") val totalAttempts: Int,
    @ColumnInfo(name = "correct_attempts") val correctAttempts: Int,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long,
)
