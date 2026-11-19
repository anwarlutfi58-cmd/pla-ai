package com.pla.agent

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LearningScheduler {

    private const val WORK_NAME = "PLA_AUTO_LEARN"

    // Menyalakan jadwal belajar (Setiap 15 menit)
    fun startLearning(context: Context) {
        val learnRequest = PeriodicWorkRequestBuilder<LearningWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            learnRequest
        )
    }

    // Mematikan jadwal belajar
    fun stopLearning(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}