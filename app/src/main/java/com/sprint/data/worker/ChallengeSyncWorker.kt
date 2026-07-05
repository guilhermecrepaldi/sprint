package com.sprint.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sprint.data.local.runtime.SprintRuntimeDatabase

class ChallengeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = SprintRuntimeDatabase.getInstance(applicationContext)
        val dao = database.challengeDao()

        val pendingChallenges = dao.getBySyncStatus("PENDING")

        if (pendingChallenges.isEmpty()) {
            return Result.success()
        }

        return try {
            // Here you would inject your API client or Retrofit interface
            // For now, since there is no API layer established yet in SPRINT,
            // we will simulate the push and update local syncStatus to "SYNCED"
            
            for (challenge in pendingChallenges) {
                // Simulate network call
                // val response = apiClient.postChallengeSync(challenge)
                
                // If success:
                val updated = challenge.copy(syncStatus = "SYNCED")
                dao.insert(updated)
            }
            Result.success()
        } catch (e: Exception) {
            // If the network call fails, we return RETRY so WorkManager will backoff and try again later
            Result.retry()
        }
    }
}
