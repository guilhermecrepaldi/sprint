package com.strava_matematica.data.local.catalog

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ExerciseDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE primary_skill = :skill")
    suspend fun countBySkill(skill: String): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE primary_skill = :skill AND difficulty >= :minimumDifficulty")
    suspend fun countBySkillDifficulty(skill: String, minimumDifficulty: Double): Int

    @Query("SELECT id FROM exercises WHERE primary_skill = :skill ORDER BY difficulty, statement, id LIMIT 1 OFFSET :offset")
    suspend fun exerciseIdBySkillOffset(skill: String, offset: Int): String?

    @Query(
        """
        SELECT id FROM exercises
        WHERE primary_skill = :skill AND difficulty >= :minimumDifficulty
        ORDER BY difficulty, statement, id
        LIMIT 1 OFFSET :offset
        """,
    )
    suspend fun exerciseIdBySkillDifficultyOffset(
        skill: String,
        minimumDifficulty: Double,
        offset: Int,
    ): String?

    @Query("SELECT id FROM exercises WHERE template_id = :templateId ORDER BY difficulty, statement, id LIMIT 1 OFFSET :offset")
    suspend fun exerciseIdByTemplateOffset(templateId: String, offset: Int): String?

    @Query("SELECT COUNT(*) FROM exercises WHERE template_id = :templateId")
    suspend fun countByTemplate(templateId: String): Int

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Query(
        """
        SELECT primary_skill, COUNT(*) AS available
        FROM exercises
        GROUP BY primary_skill
        """,
    )
    suspend fun availableBySkill(): List<SkillAvailability>
}

data class SkillAvailability(
    @androidx.room.ColumnInfo(name = "primary_skill") val skill: String,
    val available: Int,
)
