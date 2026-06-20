package com.presto.mediamanager.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.presto.mediamanager.PlaceholderVideo
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.sampleItems
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
 * Accessibility checks (Android Accessibility Test Framework via Roborazzi) on
 * key screens — touch-target size, contrast, and label presence. Reported at
 * LogOnly for now (the feed action buttons and the day stepper have pre-existing
 * touch-target/contrast findings); raise to Error once those are fixed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AccessibilityTests {

    @get:Rule
    val composeRule = createComposeRule()

    private val options = RoborazziATFAccessibilityCheckOptions(
        checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
        failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.LogOnly,
    )

    @Test
    fun reviewFeed_isAccessible() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = sampleItems(2),
                    onOpenSettings = {}, onDelete = {}, onLater = {},
                    onArchive = { _, _ -> }, onReview = {},
                    videoSlot = { PlaceholderVideo() },
                )
            }
        }
        composeRule.onRoot().checkRoboAccessibility(roborazziATFAccessibilityCheckOptions = options)
    }

    @Test
    fun settings_isAccessible() {
        composeRule.setContent {
            PrestoTheme {
                SettingsContent(
                    state = AppSettings(autoDeleteEnabled = true, autoDeleteDays = 14),
                    onBack = {}, onFolderPicked = { _, _ -> }, onAutoDeleteEnabled = {},
                    onAutoDeleteDays = {}, onDefaultRemoveAudio = {}, onDefaultShareResolution = {},
                )
            }
        }
        composeRule.onRoot().checkRoboAccessibility(roborazziATFAccessibilityCheckOptions = options)
    }
}
