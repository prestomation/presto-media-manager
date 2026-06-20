package com.presto.mediamanager

import android.content.Context
import com.presto.mediamanager.data.db.AppDatabase
import com.presto.mediamanager.data.repo.MediaRepository
import com.presto.mediamanager.data.settings.SettingsRepository
import com.presto.mediamanager.data.storage.SafStorageGateway
import com.presto.mediamanager.data.storage.StorageGateway
import com.presto.mediamanager.media.ExportManager

/** Lightweight manual DI graph held by the [PrestoApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val storageGateway: StorageGateway by lazy { SafStorageGateway(appContext) }

    private val database by lazy { AppDatabase.get(appContext) }
    private val exportManager by lazy { ExportManager(appContext, storageGateway) }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            dao = database.mediaDao(),
            storage = storageGateway,
            settings = settingsRepository,
            exporter = exportManager,
        )
    }
}
