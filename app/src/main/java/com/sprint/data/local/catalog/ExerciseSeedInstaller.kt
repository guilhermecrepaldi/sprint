package com.sprint.data.local.catalog

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ExerciseSeedInstaller(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        CoroutineScope(Dispatchers.IO).launch {
            // Insere os rich templates que nǜo estǜo no db asset (legacy do MainActivity).
            db.execSQL("""
                INSERT INTO exercises (
                    id, statement, expected_answer, primary_skill, skill_tags_json, difficulty, 
                    estimated_time_ms, source_library, source_license, subject, canvas_mode, 
                    validator_type, node_id, template_id, template_version, variant_seed, answer_type
                ) VALUES (
                    'rich_trig_001', 'Encontre o valor do cateto oposto x no triǽngulo retǽngulo.\n[fig:right_triangle,angle=30,hyp=10,opp=x]', '5', 'trig_razoes', '["trig_razoes"]', 3.2, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'trig_razoes_rich_1', 'rich_trig_diagram', 1, 1, 'numeric'
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO exercises (
                    id, statement, expected_answer, primary_skill, skill_tags_json, difficulty, 
                    estimated_time_ms, source_library, source_license, subject, canvas_mode, 
                    validator_type, node_id, template_id, template_version, variant_seed, answer_type
                ) VALUES (
                    'rich_trig_002', 'Determine o valor de y (cateto adjacente) no triǽngulo retǽngulo.\n[fig:right_triangle,angle=60,hyp=8,adj=y]', '4', 'trig_razoes', '["trig_razoes"]', 3.5, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'trig_razoes_rich_2', 'rich_trig_diagram', 1, 2, 'numeric'
                )
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO exercises (
                    id, statement, expected_answer, primary_skill, skill_tags_json, difficulty, 
                    estimated_time_ms, source_library, source_license, subject, canvas_mode, 
                    validator_type, node_id, template_id, template_version, variant_seed, answer_type
                ) VALUES (
                    'rich_prob_001', 'No diagrama de Venn, qual a probabilidade de selecionar um aluno que joga APENAS Futebol?\n[fig:venn_2,labelA=Futebol,labelB=Volei,valA=13,valInter=5,valB=10,valOuter=2]', '13/30', 'probabilidade', '["probabilidade"]', 3.6, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'probabilidade_rich_1', 'rich_trig_diagram', 1, 4, 'numeric'
                )
            """.trimIndent())

            // Seed ENEM Math Questions
            try {
                val jsonString = context.assets.open("enem_math.json").bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                val stmtQuestion = db.compileStatement("INSERT INTO exam_questions (id, title, indexNum, discipline, year, context, files_json, correct_alternative, alternatives_introduction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                val stmtAlt = db.compileStatement("INSERT INTO exam_alternatives (id, question_id, letter, text, file, is_correct) VALUES (?, ?, ?, ?, ?, ?)")
                
                db.beginTransaction()
                try {
                    for (i in 0 until jsonArray.length()) {
                        val qObj = jsonArray.getJSONObject(i)
                        val year = qObj.optInt("year")
                        val disc = qObj.optString("discipline")
                        val idx = qObj.optInt("index")
                        val qId = "enem-${year}-${disc}-${idx}"
                        
                        stmtQuestion.bindString(1, qId)
                        stmtQuestion.bindString(2, qObj.optString("title", ""))
                        stmtQuestion.bindLong(3, idx.toLong())
                        stmtQuestion.bindString(4, disc)
                        stmtQuestion.bindLong(5, year.toLong())
                        stmtQuestion.bindString(6, qObj.optString("context", ""))
                        stmtQuestion.bindString(7, qObj.optJSONArray("files")?.toString() ?: "[]")
                        stmtQuestion.bindString(8, qObj.optString("correctAlternative", ""))
                        
                        val altIntro = qObj.optString("alternativesIntroduction", "")
                        if (altIntro.isNotEmpty()) {
                            stmtQuestion.bindString(9, altIntro)
                        } else {
                            stmtQuestion.bindNull(9)
                        }
                        stmtQuestion.executeInsert()
                        
                        val alts = qObj.optJSONArray("alternatives")
                        if (alts != null) {
                            for (j in 0 until alts.length()) {
                                val altObj = alts.getJSONObject(j)
                                val letter = altObj.optString("letter", "")
                                val altId = "${qId}-${letter}"
                                
                                stmtAlt.bindString(1, altId)
                                stmtAlt.bindString(2, qId)
                                stmtAlt.bindString(3, letter)
                                stmtAlt.bindString(4, altObj.optString("text", ""))
                                
                                val file = altObj.optString("file", "")
                                if (file.isNotEmpty()) stmtAlt.bindString(5, file) else stmtAlt.bindNull(5)
                                
                                stmtAlt.bindLong(6, if (altObj.optBoolean("isCorrect", false)) 1L else 0L)
                                stmtAlt.executeInsert()
                            }
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
