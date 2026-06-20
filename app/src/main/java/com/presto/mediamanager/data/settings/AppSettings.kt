package com.presto.mediamanager.data.settings

/** Target heights offered for the downscaled "share" copy. */
enum class ShareResolution(val label: String, val height: Int) {
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    ;

    companion object {
        fun fromHeight(height: Int): ShareResolution =
            entries.firstOrNull { it.height == height } ?: P720
    }
}

/**
 * All persisted user configuration. Folder references are SAF tree URIs (as
 * strings) for which the app holds persistable read/write permission.
 */
data class AppSettings(
    val inputFolderUri: String? = null,
    val archiveFolderUri: String? = null,
    val shareFolderUri: String? = null,
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteDays: Int = 14,
    val defaultRemoveAudio: Boolean = false,
    val defaultShareResolution: ShareResolution = ShareResolution.P720,
) {
    val isConfigured: Boolean
        get() = inputFolderUri != null && archiveFolderUri != null && shareFolderUri != null
}
