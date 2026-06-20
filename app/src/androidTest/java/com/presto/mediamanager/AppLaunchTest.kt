package com.presto.mediamanager

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke test: the app launches and, with no folders configured yet,
 * shows the folder-setup prompt. Real video playback/export is exercised
 * manually on-device; this guards against gross startup regressions.
 *
 * POST_NOTIFICATIONS is pre-granted (order = 0, before the activity launches) so
 * MainActivity never pops the runtime-permission dialog, which would background
 * the activity and hide its Compose hierarchy from the test.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launches_andShowsSetupPrompt() {
        // No folders configured on a fresh install -> the feed prompts for setup.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Set up your folders").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Set up your folders").assertIsDisplayed()
    }
}
