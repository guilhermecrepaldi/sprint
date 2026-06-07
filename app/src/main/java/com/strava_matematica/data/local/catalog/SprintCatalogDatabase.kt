package com.strava_matematica.data.local.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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

        fun getInstance(context: Context): SprintCatalogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SprintCatalogDatabase::class.java,
                    "exercise_catalog.db",
                )
                    .createFromAsset("databases/exercise_catalog.db")
                    .fallbackToDestructiveMigration()
                    .addCallback(ExerciseSeedInstaller(context.applicationContext))
                    .build()
                    .also { instance = it }
            }
    }
}
