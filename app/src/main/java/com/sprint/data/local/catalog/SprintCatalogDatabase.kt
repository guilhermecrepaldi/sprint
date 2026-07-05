package com.sprint.data.local.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        ExamQuestionEntity::class,
        ExamAlternativeEntity::class
    ],
    version = 2,
    exportSchema = false,
)
abstract class SprintCatalogDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile private var instance: SprintCatalogDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS exam_questions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, indexNum INTEGER NOT NULL, discipline TEXT NOT NULL, year INTEGER NOT NULL, context TEXT NOT NULL, filesJson TEXT NOT NULL, correctAlternative TEXT NOT NULL, alternativesIntroduction TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS exam_alternatives (id TEXT NOT NULL PRIMARY KEY, question_id TEXT NOT NULL, letter TEXT NOT NULL, text TEXT NOT NULL, file TEXT, is_correct INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_exam_alternatives_qid ON exam_alternatives(question_id)")
            }
        }

        fun getInstance(context: Context): SprintCatalogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SprintCatalogDatabase::class.java,
                    "exercise_catalog.db",
                )
                    .createFromAsset("databases/exercise_catalog.db")
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(ExerciseSeedInstaller(context.applicationContext))
                    .build()
                    .also { instance = it }
            }
    }
}
