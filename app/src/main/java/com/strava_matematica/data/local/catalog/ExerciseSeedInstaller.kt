package com.strava_matematica.data.local.catalog

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class ExerciseSeedInstaller : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // This is called only when the DB is first created.
        CoroutineScope(Dispatchers.IO).launch {
            // Insere os rich templates que nǜo estǜo no db asset (legacy do MainActivity).
            // A prioridade agora Ǹ o ProceduralEngine, mas mantemos o seed visual 
            // caso a UI precise de um Diagrama para os simulados.
            db.execSQL("""
                INSERT INTO exercise_catalog (
                    id, statement, expectedAnswer, primarySkill, skillTagsJson, difficulty, 
                    estimatedTimeMs, sourceLibrary, sourceLicense, subject, canvasMode, 
                    validatorType, nodeId, templateId, templateVersion, variantSeed, answerType
                ) VALUES (
                    'rich_trig_001', 'Encontre o valor do cateto oposto x no triǽngulo retǽngulo.\n[fig:right_triangle,angle=30,hyp=10,opp=x]', '5', 'trig_razoes', '["trig_razoes"]', 3.2, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'trig_razoes_rich_1', 'rich_trig_diagram', 1, 1, 'numeric'
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO exercise_catalog (
                    id, statement, expectedAnswer, primarySkill, skillTagsJson, difficulty, 
                    estimatedTimeMs, sourceLibrary, sourceLicense, subject, canvasMode, 
                    validatorType, nodeId, templateId, templateVersion, variantSeed, answerType
                ) VALUES (
                    'rich_trig_002', 'Determine o valor de y (cateto adjacente) no triǽngulo retǽngulo.\n[fig:right_triangle,angle=60,hyp=8,adj=y]', '4', 'trig_razoes', '["trig_razoes"]', 3.5, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'trig_razoes_rich_2', 'rich_trig_diagram', 1, 2, 'numeric'
                )
            """.trimIndent())
            
            db.execSQL("""
                INSERT INTO exercise_catalog (
                    id, statement, expectedAnswer, primarySkill, skillTagsJson, difficulty, 
                    estimatedTimeMs, sourceLibrary, sourceLicense, subject, canvasMode, 
                    validatorType, nodeId, templateId, templateVersion, variantSeed, answerType
                ) VALUES (
                    'rich_prob_001', 'No diagrama de Venn, qual a probabilidade de selecionar um aluno que joga APENAS Futebol?\n[fig:venn_2,labelA=Futebol,labelB=Volei,valA=13,valInter=5,valB=10,valOuter=2]', '13/30', 'probabilidade', '["probabilidade"]', 3.6, 
                    45000, 'rich_visual_v1', 'CC-BY', 'math', 'calculation', 
                    'exact', 'probabilidade_rich_1', 'rich_trig_diagram', 1, 4, 'numeric'
                )
            """.trimIndent())
        }
    }
}
