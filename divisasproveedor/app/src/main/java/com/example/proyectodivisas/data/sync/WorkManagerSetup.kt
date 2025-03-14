package com.example.proyectodivisas.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerSetup {
    private const val WORK_NAME = "ExchangeRateSyncWork"

    fun scheduleWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(0, TimeUnit.MINUTES) // Para ejecutar inmediatamente y luego cada hora
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
