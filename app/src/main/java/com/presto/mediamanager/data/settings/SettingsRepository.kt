package com.presto.mediamanager.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Read-only view of settings, so consumers (e.g. the repository) can be faked in tests. */
interface SettingsProvider {
    suspend fun current(): AppSettings
}

class SettingsRepository(private val context: Context) : SettingsProvider {

    private object Keys {
        val INPUT = stringPreferencesKey("input_folder_uri")
        val ARCHIVE = stringPreferencesKey("archive_folder_uri")
        val SHARE = stringPreferencesKey("share_folder_uri")
        val AUTO_DELETE_ENABLED = booleanPreferencesKey("auto_delete_enabled")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
        val DEFAULT_REMOVE_AUDIO = booleanPreferencesKey("default_remove_audio")
        val DEFAULT_SHARE_HEIGHT = intPreferencesKey("default_share_height")
        val EXACT_SCRUBBING = booleanPreferencesKey("exact_scrubbing")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            inputFolderUri = p[Keys.INPUT],
            archiveFolderUri = p[Keys.ARCHIVE],
            shareFolderUri = p[Keys.SHARE],
            autoDeleteEnabled = p[Keys.AUTO_DELETE_ENABLED] ?: false,
            autoDeleteDays = p[Keys.AUTO_DELETE_DAYS] ?: 14,
            defaultRemoveAudio = p[Keys.DEFAULT_REMOVE_AUDIO] ?: false,
            defaultShareResolution = ShareResolution.fromHeight(p[Keys.DEFAULT_SHARE_HEIGHT] ?: 720),
            exactScrubbing = p[Keys.EXACT_SCRUBBING] ?: false,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    suspend fun setInputFolder(uri: String) = edit { it[Keys.INPUT] = uri }
    suspend fun setArchiveFolder(uri: String) = edit { it[Keys.ARCHIVE] = uri }
    suspend fun setShareFolder(uri: String) = edit { it[Keys.SHARE] = uri }
    suspend fun setAutoDeleteEnabled(enabled: Boolean) = edit { it[Keys.AUTO_DELETE_ENABLED] = enabled }
    suspend fun setAutoDeleteDays(days: Int) = edit { it[Keys.AUTO_DELETE_DAYS] = days.coerceIn(1, 365) }
    suspend fun setDefaultRemoveAudio(value: Boolean) = edit { it[Keys.DEFAULT_REMOVE_AUDIO] = value }
    suspend fun setDefaultShareResolution(res: ShareResolution) =
        edit { it[Keys.DEFAULT_SHARE_HEIGHT] = res.height }
    suspend fun setExactScrubbing(value: Boolean) = edit { it[Keys.EXACT_SCRUBBING] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
