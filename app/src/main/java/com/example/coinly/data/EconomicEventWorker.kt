package com.example.coinly.data

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class EconomicEventWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.success()
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()
        return try {
            EconomicEvents.processDue(uid); Result.success()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Result.retry()
        }
    }
}

object EconomicEventWork {
    fun configure(context: Context, uid: String?) {
        val manager = WorkManager.getInstance(context)
        if (uid == null) { manager.cancelUniqueWork("coinly-economic-events"); return }
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val data = workDataOf("uid" to uid)
        manager.enqueueUniquePeriodicWork("coinly-economic-events", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<EconomicEventWorker>(15, TimeUnit.MINUTES).setInputData(data).setConstraints(constraints).build())
        manager.enqueueUniqueWork("coinly-economic-events-now-$uid", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EconomicEventWorker>().setInputData(data).setConstraints(constraints).build())
    }
}
