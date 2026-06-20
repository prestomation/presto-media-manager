package com.presto.mediamanager.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val SCAN_PERIODIC = "scan-periodic"
    private const val SCAN_ONCE = "scan-once"
    private const val AUTO_DELETE = "auto-delete"

    fun schedulePeriodic(context: Context) {
        val wm = WorkManager.getInstance(context)

        val scan = PeriodicWorkRequestBuilder<ScanWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        wm.enqueueUniquePeriodicWork(SCAN_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, scan)

        val autoDelete = PeriodicWorkRequestBuilder<AutoDeleteWorker>(1, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(AUTO_DELETE, ExistingPeriodicWorkPolicy.KEEP, autoDelete)
    }

    /** Kick an immediate scan (e.g. app open or pull-to-refresh). */
    fun scanNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SCAN_ONCE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ScanWorker>().build(),
        )
    }
}
