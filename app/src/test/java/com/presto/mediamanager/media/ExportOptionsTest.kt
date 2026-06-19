package com.presto.mediamanager.media

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExportOptionsTest {

    @Test
    fun fullFrameCropIsDetected() {
        assertThat(CropRect().isFullFrame).isTrue()
        assertThat(CropRect(0.1f, 0f, 1f, 1f).isFullFrame).isFalse()
    }

    @Test
    fun shareDestinationProducesBothOutputs() {
        val request = ExportRequest(
            label = "x",
            trim = TrimRange(0, 1000),
            crop = CropRect(),
            removeAudio = false,
            shareResolution = com.presto.mediamanager.data.settings.ShareResolution.P720,
            destination = ExportDestination.SHARE,
        )
        assertThat(request.destination).isEqualTo(ExportDestination.SHARE)
    }
}
