package com.presto.mediamanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Filenames {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Strip characters that are illegal/awkward in filenames. */
    fun sanitizeLabel(label: String): String =
        label.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), "-")
            .take(60)
            .ifBlank { "clip" }

    /**
     * Build "{captureDate}_{label}.mp4". The date is the video's capture time so
     * the archived/shared file is self-describing for future reference.
     */
    fun dated(label: String, captureMs: Long, extension: String = "mp4"): String {
        val date = dateFormat.format(Date(captureMs))
        return "${date}_${sanitizeLabel(label)}.$extension"
    }
}
