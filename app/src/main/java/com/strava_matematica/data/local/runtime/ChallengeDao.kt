package com.strava_matematica.data.local.runtime

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: ChallengeResultEntity)

    @Query("SELECT * FROM challenge_results WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: String): List<ChallengeResultEntity>

    @Query("SELECT * FROM challenge_results WHERE challengeId = :id")
    suspend fun getById(id: String): ChallengeResultEntity?
}
