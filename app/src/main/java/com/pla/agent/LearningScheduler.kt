package com.pla.agent

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LearningScheduler {

    private const val WORK_NAME = "PLA_AUTO_LEARN"

    fun startLearning(context: Context) {
        // Syarat: Baterai tidak boleh lemah
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val learnRequest = PeriodicWorkRequestBuilder<LearningWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            learnRequest
        )
    }

    fun stopLearning(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}