package com.strava_matematica.data.local.repository

import com.strava_matematica.data.local.catalog.ExamDao
import com.strava_matematica.data.local.catalog.ExamQuestionEntity
import com.strava_matematica.data.local.catalog.ExamAlternativeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExamRepository(private val examDao: ExamDao) {
    
    suspend fun getAvailableYears(): List<Int> = withContext(Dispatchers.IO) {
        examDao.getAvailableYears()
    }

    suspend fun getQuestionsByYear(year: Int): List<Pair<ExamQuestionEntity, List<ExamAlternativeEntity>>> = withContext(Dispatchers.IO) {
        val questions = examDao.getQuestionsByYear(year)
        questions.map { q ->
            val alts = examDao.getAlternativesForQuestion(q.id)
            Pair(q, alts)
        }
    }

    suspend fun getRandomQuestions(limit: Int): List<Pair<ExamQuestionEntity, List<ExamAlternativeEntity>>> = withContext(Dispatchers.IO) {
        val questions = examDao.getRandomQuestions(limit)
        questions.map { q ->
            val alts = examDao.getAlternativesForQuestion(q.id)
            Pair(q, alts)
        }
    }
}
