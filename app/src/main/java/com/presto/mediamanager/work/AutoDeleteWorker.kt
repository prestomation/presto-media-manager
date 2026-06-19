package com.presto.mediamanager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.presto.mediamanager.PrestoApp

/** Daily job that purges unreviewed videos older than the configured threshold. */
class AutoDeleteWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PrestoApp).container
        val settings = container.settingsRepository.current()
        if (settings.autoDeleteEnabled) {
            container.mediaRepository.autoDeleteOlderThan(settings.autoDeleteDays)
            BadgeManager.update(applicationContext, container.mediaRepository.reviewCount())
        }
        return Result.success()
    }
}
