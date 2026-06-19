package com.presto.mediamanager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.presto.mediamanager.PrestoApp

/** Periodic + on-demand scan that keeps the review queue and badge in sync. */
class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PrestoApp).container
        return try {
            container.mediaRepository.scanInputFolder()
            BadgeManager.update(applicationContext, container.mediaRepository.reviewCount())
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
