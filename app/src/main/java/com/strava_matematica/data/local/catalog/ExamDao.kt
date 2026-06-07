package com.strava_matematica.data.local.catalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ExamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<ExamQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlternatives(alternatives: List<ExamAlternativeEntity>)

    @Query("SELECT * FROM exam_questions WHERE year = :year")
    suspend fun getQuestionsByYear(year: Int): List<ExamQuestionEntity>

    @Query("SELECT * FROM exam_questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<ExamQuestionEntity>

    @Query("SELECT * FROM exam_alternatives WHERE question_id = :questionId ORDER BY letter ASC")
    suspend fun getAlternativesForQuestion(questionId: String): List<ExamAlternativeEntity>

    @Query("SELECT DISTINCT year FROM exam_questions ORDER BY year DESC")
    suspend fun getAvailableYears(): List<Int>
}
