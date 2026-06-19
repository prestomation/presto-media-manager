package com.presto.mediamanager

import android.content.Context
import com.presto.mediamanager.data.db.AppDatabase
import com.presto.mediamanager.data.repo.MediaRepository
import com.presto.mediamanager.data.saf.SafManager
import com.presto.mediamanager.data.settings.SettingsRepository
import com.presto.mediamanager.media.ExportManager

/** Lightweight manual DI graph held by the [PrestoApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val safManager: SafManager by lazy { SafManager(appContext) }

    private val database by lazy { AppDatabase.get(appContext) }
    private val exportManager by lazy { ExportManager(appContext, safManager) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            dao = database.mediaDao(),
            saf = safManager,
            settings = settingsRepository,
            exporter = exportManager,
        )
    }
}
