package com.presto.mediamanager.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.presto.mediamanager.PlaceholderVideo
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.data.settings.ShareResolution
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.TrimRange
import com.presto.mediamanager.sampleItem
import com.presto.mediamanager.sampleItems
import com.presto.mediamanager.ui.components.FeedScrubber
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

/** Additional states and rendering configurations (light theme, large font, RTL). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MoreScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun feed_lightTheme() {
        composeRule.setContent {
            PrestoTheme(darkTheme = false) {
                ReviewFeedContent(
                    items = sampleItems(2),
                    onOpenSettings = {}, onDelete = {}, onLater = {},
                    onArchive = { _, _ -> }, onReview = {},
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feed_largeFont() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 1.5f),
            ) {
                PrestoTheme {
                    ReviewFeedContent(
                        items = sampleItems(2),
                        onOpenSettings = {}, onDelete = {}, onLater = {},
                        onArchive = { _, _ -> }, onReview = {},
                        videoSlot = { _, _ -> PlaceholderVideo() },
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feed_undoSnackbar() {
        composeRule.setContent {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                host.showSnackbar("Video deleted", actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
            }
            PrestoTheme {
                ReviewFeedContent(
                    items = sampleItems(2),
                    snackbarHostState = host,
                    onOpenSettings = {}, onDelete = {}, onLater = {},
                    onArchive = { _, _ -> }, onReview = {},
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feedScrubber() {
        composeRule.setContent {
            PrestoTheme {
                Box(Modifier.fillMaxWidth().background(Color.Black).padding(16.dp)) {
                    FeedScrubber(
                        positionMs = 4_200,
                        durationMs = 12_000,
                        onScrubStart = {},
                        onSeek = {},
                        onScrubEnd = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun feed_unconfigured() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = emptyList(),
                    configured = false,
                    onOpenSettings = {}, onDelete = {}, onLater = {},
                    onArchive = { _, _ -> }, onReview = {},
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun editor_exporting() {
        composeRule.setContent {
            PrestoTheme {
                EditorContent(
                    state = EditorUiState(
                        item = sampleItem(),
                        durationMs = 12_000,
                        trim = TrimRange(1_000, 8_000),
                        crop = CropRect(0.1f, 0.1f, 0.9f, 0.85f),
                        exporting = true,
                    ),
                    onBack = {}, onTrimChange = {}, onCropChange = {},
                    onRemoveAudioChange = {}, onShareResolutionChange = {}, onExport = { _, _ -> },
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun editor_error() {
        composeRule.setContent {
            PrestoTheme {
                EditorContent(
                    state = EditorUiState(
                        item = sampleItem(),
                        durationMs = 12_000,
                        trim = TrimRange(0, 12_000),
                        error = "Export failed: codec not supported",
                    ),
                    onBack = {}, onTrimChange = {}, onCropChange = {},
                    onRemoveAudioChange = {}, onShareResolutionChange = {}, onExport = { _, _ -> },
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun editor_trimFilmstrip() {
        composeRule.setContent {
            PrestoTheme {
                EditorContent(
                    state = EditorUiState(
                        item = sampleItem(),
                        durationMs = 12_000,
                        trim = TrimRange(2_500, 9_500),
                        crop = CropRect(0.15f, 0.15f, 0.85f, 0.8f),
                        positionMs = 6_000,
                        thumbnails = fakeThumbnails(8),
                    ),
                    onBack = {}, onTrimChange = {}, onCropChange = {},
                    onRemoveAudioChange = {}, onShareResolutionChange = {}, onExport = { _, _ -> },
                    videoSlot = { _, _ -> PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** Solid-color stand-ins for extracted video frames, so the filmstrip is screenshot-stable. */
    private fun fakeThumbnails(n: Int): List<ImageBitmap> = (0 until n).map { i ->
        val bmp = Bitmap.createBitmap(48, 56, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.rgb(40 + i * 18, 70, 110 - i * 6))
        bmp.asImageBitmap()
    }

    @Test
    fun settings_unconfigured() {
        composeRule.setContent {
            PrestoTheme {
                SettingsContent(
                    state = AppSettings(),
                    onBack = {}, onFolderPicked = { _, _ -> }, onAutoDeleteEnabled = {},
                    onAutoDeleteDays = {}, onDefaultRemoveAudio = {}, onDefaultShareResolution = {},
                    onExactScrubbing = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun settings_rtl() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PrestoTheme {
                    SettingsContent(
                        state = AppSettings(
                            inputFolderUri = "content://tree/primary:DCIM",
                            archiveFolderUri = "content://tree/primary:Archive",
                            shareFolderUri = "content://tree/primary:Share",
                            autoDeleteEnabled = true,
                            autoDeleteDays = 7,
                            defaultShareResolution = ShareResolution.P1080,
                        ),
                        onBack = {}, onFolderPicked = { _, _ -> }, onAutoDeleteEnabled = {},
                        onAutoDeleteDays = {}, onDefaultRemoveAudio = {}, onDefaultShareResolution = {},
                        onExactScrubbing = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
