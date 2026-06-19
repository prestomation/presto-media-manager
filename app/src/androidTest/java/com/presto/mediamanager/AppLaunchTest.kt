package com.presto.mediamanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke test: the app launches and, with no folders configured yet,
 * shows the empty review state. Real video playback/export is exercised
 * manually on-device; this guards against gross startup regressions.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launches_andShowsEmptyReviewState() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("All caught up").assertIsDisplayed()
    }
}
