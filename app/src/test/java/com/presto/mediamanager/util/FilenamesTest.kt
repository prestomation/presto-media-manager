package com.presto.mediamanager.util

import com.google.common.truth.Truth.assertThat
import com.presto.mediamanager.FIXED_CAPTURE_MS
import org.junit.Test

class FilenamesTest {

    @Test
    fun sanitize_stripsIllegalCharsAndSpaces() {
        assertThat(Filenames.sanitizeLabel("near miss / 5th & Main"))
            .isEqualTo("near-miss-5th-&-Main")
    }

    @Test
    fun sanitize_blankFallsBackToClip() {
        assertThat(Filenames.sanitizeLabel("   ")).isEqualTo("clip")
    }

    @Test
    fun dated_includesDateAndLabelAndExtension() {
        val name = Filenames.dated("close call", FIXED_CAPTURE_MS)
        assertThat(name).matches("\\d{4}-\\d{2}-\\d{2}_close-call\\.mp4")
    }
}
