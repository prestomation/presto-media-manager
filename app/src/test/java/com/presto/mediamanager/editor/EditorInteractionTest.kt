package com.presto.mediamanager.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.TrimRange
import com.presto.mediamanager.ui.editor.CropOverlay
import com.presto.mediamanager.ui.editor.TrimBar
import com.presto.mediamanager.ui.theme.PrestoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Interaction tests for the editor's drag controls. These reproduce the
 * "controls inoperable" report: a drag must move the crop/trim by roughly the
 * drag distance, not by a single stale delta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun cropBody_drag_movesCropAcrossTheDrag() {
        var crop by mutableStateOf(CropRect(0.2f, 0.2f, 0.8f, 0.8f))
        rule.setContent {
            PrestoTheme {
                Box(Modifier.size(300.dp)) {
                    CropOverlay(crop = crop, onCropChange = { crop = it })
                }
            }
        }
        val startLeft = crop.left

        rule.onRoot().performTouchInput {
            swipe(start = center, end = Offset(center.x + width * 0.25f, center.y), durationMillis = 200)
        }
        rule.waitForIdle()

        // A quarter-width drag should move the crop substantially, not a hair.
        assertThat(crop.left).isGreaterThan(startLeft + 0.1f)
    }

    @Test
    fun trimStartHandle_drag_movesStartAcrossTheDrag() {
        var trim by mutableStateOf(TrimRange(2_000, 8_000))
        rule.setContent {
            PrestoTheme {
                Box(Modifier.size(300.dp, 56.dp)) {
                    TrimBar(
                        durationMs = 10_000,
                        trim = trim,
                        positionMs = 0,
                        thumbnails = emptyList(),
                        onTrimChange = { trim = it },
                    )
                }
            }
        }
        val startMs = trim.startMs

        // Start handle sits at 20% of the width; drag it well to the right.
        rule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.2f, centerY),
                end = Offset(width * 0.45f, centerY),
                durationMillis = 200,
            )
        }
        rule.waitForIdle()

        assertThat(trim.startMs).isGreaterThan(startMs + 1_500)
    }
}
