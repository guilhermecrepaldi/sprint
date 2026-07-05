package com.sprint.data.local.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["primary_skill", "difficulty"]),
        Index(value = ["template_id"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val statement: String,
    @ColumnInfo(name = "expected_answer") val expectedAnswer: String,
    @ColumnInfo(name = "primary_skill") val primarySkill: String,
    @ColumnInfo(name = "skill_tags_json") val skillTagsJson: String,
    val difficulty: Double,
    @ColumnInfo(name = "estimated_time_ms") val estimatedTimeMs: Int?,
    @ColumnInfo(name = "source_library") val sourceLibrary: String?,
    @ColumnInfo(name = "source_license") val sourceLicense: String?,
    val subject: String,
    @ColumnInfo(name = "canvas_mode") val canvasMode: String,
    @ColumnInfo(name = "validator_type") val validatorType: String,
    @ColumnInfo(name = "validator_config_json") val validatorConfigJson: String?,
    @ColumnInfo(name = "node_id") val nodeId: String?,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "template_version") val templateVersion: Int,
    @ColumnInfo(name = "variant_seed") val variantSeed: Int?,
    @ColumnInfo(name = "answer_type") val answerType: String,
    @ColumnInfo(name = "method_tags_json") val methodTagsJson: String?,
    @ColumnInfo(name = "prerequisite_tags_json") val prerequisiteTagsJson: String?,
    @ColumnInfo(name = "affinity_tags_json") val affinityTagsJson: String?,
    @ColumnInfo(name = "parameter_vector_json") val parameterVectorJson: String?,
    @ColumnInfo(name = "difficulty_vector_json") val difficultyVectorJson: String?,
)

@Entity(tableName = "exam_questions")
data class ExamQuestionEntity(
    @PrimaryKey val id: String, // ex: "enem-2023-matematica-9"
    val title: String,
    val indexNum: Int,
    val discipline: String,
    val year: Int,
    val context: String,
    @ColumnInfo(name = "files_json") val filesJson: String,
    @ColumnInfo(name = "correct_alternative") val correctAlternative: String,
    @ColumnInfo(name = "alternatives_introduction") val alternativesIntroduction: String?
)

@Entity(
    tableName = "exam_alternatives",
    indices = [
        Index(value = ["question_id"])
    ]
)
data class ExamAlternativeEntity(
    @PrimaryKey val id: String, // ex: "enem-2023-matematica-9-A"
    @ColumnInfo(name = "question_id") val questionId: String,
    val letter: String,
    val text: String,
    val file: String?,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean
)
