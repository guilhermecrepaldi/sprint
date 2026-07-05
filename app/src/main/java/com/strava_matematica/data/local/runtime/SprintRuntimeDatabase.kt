package com.strava_matematica.data.local.runtime

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentEntity::class,
        SessionEntity::class,
        ExerciseAttemptEntity::class,
        PenEventEntity::class,
        StudentSkillMemoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SprintRuntimeDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun skillMemoryDao(): SkillMemoryDao

    companion object {
        @Volatile private var instance: SprintRuntimeDatabase? = null

        fun getInstance(context: Context): SprintRuntimeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SprintRuntimeDatabase::class.java,
                    "sprint_runtime.db",
                )
                    .build()
                    .also { instance = it }
            }
    }
}
