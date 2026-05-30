package com.strava_matematica.data.local.runtime

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncDao {
    @Query("SELECT * FROM sessions WHERE is_synced = 0")
    suspend fun getUnsyncedSessions(): List<SessionEntity>

    @Query("SELECT * FROM exercise_attempts WHERE is_synced = 0")
    suspend fun getUnsyncedAttempts(): List<ExerciseAttemptEntity>

    @Query("UPDATE sessions SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSessionsAsSynced(ids: List<String>)

    @Query("UPDATE exercise_attempts SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAttemptsAsSynced(ids: List<Long>)

    // Deleta os pen_events das tentativas que já foram sincronizadas
    @Query("DELETE FROM pen_events WHERE attempt_id IN (SELECT id FROM exercise_attempts WHERE is_synced = 1)")
    suspend fun purgeSyncedPenEvents()
}
