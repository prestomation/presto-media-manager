package com.presto.mediamanager.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.presto.mediamanager.PlaceholderVideo
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.data.settings.ShareResolution
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.TrimRange
import com.presto.mediamanager.sampleItem
import com.presto.mediamanager.sampleItems
import com.presto.mediamanager.ui.editor.EditorContent
import com.presto.mediamanager.ui.editor.EditorUiState
import com.presto.mediamanager.ui.feed.ReviewFeedContent
import com.presto.mediamanager.ui.settings.SettingsContent
import com.presto.mediamanager.ui.theme.PrestoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden screenshot tests. `recordRoborazziDebug` writes the PNGs under
 * /screenshots; `verifyRoborazziDebug` (run in CI) fails if the committed
 * goldens are stale — so any UI change must land updated screenshots in the PR.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewFeed_withVideos() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = sampleItems(3),
                    onOpenSettings = {},
                    onDelete = {},
                    onLater = {},
                    onArchive = { _, _ -> },
                    onReview = {},
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun reviewFeed_empty() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = emptyList(),
                    onOpenSettings = {},
                    onDelete = {},
                    onLater = {},
                    onArchive = { _, _ -> },
                    onReview = {},
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun editor_default() {
        composeRule.setContent {
            PrestoTheme {
                EditorContent(
                    state = EditorUiState(
                        item = sampleItem(),
                        durationMs = 12_000,
                        trim = TrimRange(2_000, 9_000),
                        crop = CropRect(0.15f, 0.15f, 0.85f, 0.8f),
                        shareResolution = ShareResolution.P720,
                    ),
                    onBack = {},
                    onTrimChange = {},
                    onCropChange = {},
                    onRemoveAudioChange = {},
                    onShareResolutionChange = {},
                    onExport = { _, _ -> },
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun settings_configured() {
        composeRule.setContent {
            PrestoTheme {
                SettingsContent(
                    state = AppSettings(
                        inputFolderUri = "content://tree/primary:DCIM%2FGlasses",
                        archiveFolderUri = "content://tree/primary:Movies%2FArchive",
                        shareFolderUri = "content://tree/primary:Movies%2FShare",
                        autoDeleteEnabled = true,
                        autoDeleteDays = 14,
                        defaultShareResolution = ShareResolution.P720,
                    ),
                    buildInfo = "v0.1.0 · abc1234 · 2026-06-20 06:00 UTC",
                    onBack = {},
                    onFolderPicked = { _, _ -> },
                    onAutoDeleteEnabled = {},
                    onAutoDeleteDays = {},
                    onDefaultRemoveAudio = {},
                    onDefaultShareResolution = {},
                    onExactScrubbing = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
