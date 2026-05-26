package com.strava_matematica.viewmodel

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SprintNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String?,
    val folhaIndex: Int,
    val exerciseIndex: Int,
    val exerciseStatement: String,
    val timestamp: Long = System.currentTimeMillis(),
    val strokes: List<List<Offset>>,
)

// Versão serializável para persistência — converte Offset em [x, y]
@Serializable
internal data class SprintNoteJson(
    val id: String,
    val sessionId: String? = null,
    val folhaIndex: Int,
    val exerciseIndex: Int,
    val exerciseStatement: String,
    val timestamp: Long,
    val strokes: List<List<List<Float>>>,  // [[[x,y],[x,y]], [[x,y],...]]
)

internal fun SprintNote.toJson(): SprintNoteJson = SprintNoteJson(
    id = id,
    sessionId = sessionId,
    folhaIndex = folhaIndex,
    exerciseIndex = exerciseIndex,
    exerciseStatement = exerciseStatement,
    timestamp = timestamp,
    strokes = strokes.map { stroke -> stroke.map { listOf(it.x, it.y) } },
)

internal fun SprintNoteJson.toSprintNote(): SprintNote = SprintNote(
    id = id,
    sessionId = sessionId,
    folhaIndex = folhaIndex,
    exerciseIndex = exerciseIndex,
    exerciseStatement = exerciseStatement,
    timestamp = timestamp,
    strokes = strokes.map { stroke -> stroke.map { Offset(it[0], it[1]) } },
)

internal val NotesJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
