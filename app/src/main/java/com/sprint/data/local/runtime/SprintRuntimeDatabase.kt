package com.sprint.data.local.runtime

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercise_attempts ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS challenge_results (challengeId TEXT NOT NULL PRIMARY KEY, studentId TEXT NOT NULL, payloadContent TEXT NOT NULL, localStartedAt INTEGER, localFinishedAt INTEGER, timeSpentSeconds INTEGER NOT NULL, anomaliesLog TEXT, syncStatus TEXT NOT NULL, localSignature TEXT)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS study_plans (id TEXT NOT NULL PRIMARY KEY, studentId TEXT NOT NULL, title TEXT NOT NULL, targetAmount INTEGER NOT NULL, currentProgress INTEGER NOT NULL, rulesJson TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

@Database(
    entities = [
        StudentEntity::class,
        SessionEntity::class,
        ExerciseAttemptEntity::class,
        PenEventEntity::class,
        StudentSkillMemoryEntity::class,
        ChallengeResultEntity::class,
        StudyPlanEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class SprintRuntimeDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun skillMemoryDao(): SkillMemoryDao
    abstract fun syncDao(): SyncDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun studyPlanDao(): StudyPlanDao

    companion object {
        @Volatile private var instance: SprintRuntimeDatabase? = null

        fun getInstance(context: Context): SprintRuntimeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SprintRuntimeDatabase::class.java,
                    "sprint_runtime.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
