package com.presto.mediamanager.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.presto.mediamanager.PlaceholderVideo
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.sampleItems
import com.presto.mediamanager.ui.feed.ReviewFeedContent
import com.presto.mediamanager.ui.theme.PrestoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deleteButton_invokesCallbackForCurrentItem() {
        var deleted: MediaItem? = null
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = sampleItems(2),
                    onOpenSettings = {},
                    onDelete = { deleted = it },
                    onLater = {},
                    onArchive = { _, _ -> },
                    onReview = {},
                    videoSlot = { PlaceholderVideo() },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        assertThat(deleted).isNotNull()
        assertThat(deleted!!.uri).isEqualTo("content://fake/1")
    }

    @Test
    fun archiveButton_opensLabelDialog() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = sampleItems(1),
                    onOpenSettings = {},
                    onDelete = {},
                    onLater = {},
                    onArchive = { _, _ -> },
                    onReview = {},
                    videoSlot = { PlaceholderVideo() },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Archive").performClick()
        composeRule.onNodeWithText("Archive clip").assertIsDisplayed()
    }

    @Test
    fun emptyQueue_showsAllCaughtUp() {
        composeRule.setContent {
            PrestoTheme {
                ReviewFeedContent(
                    items = emptyList(),
                    onOpenSettings = {},
                    onDelete = {},
                    onLater = {},
                    onArchive = { _, _ -> },
                    onReview = {},
                    videoSlot = { PlaceholderVideo() },
                )
            }
        }
        composeRule.onNodeWithText("All caught up").assertIsDisplayed()
    }
}
