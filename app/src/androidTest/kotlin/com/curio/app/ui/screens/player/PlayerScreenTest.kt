package com.curio.app.ui.screens.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.Otter.app.data.models.Video
import org.junit.Rule
import org.junit.Test

class PlayerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerScreen_displaysVideoTitle() {
        val video =
            Video(
                id = "test-video-id",
                title = "Test Video Title",
                thumbnailUrl = "https://example.com/thumb.jpg",
            )

        composeTestRule.setContent {
            PlayerScreen(
                video = video,
                onBackClick = {},
            )
        }

        composeTestRule.onNodeWithText("Test Video Title").assertIsDisplayed()
    }

    @Test
    fun playerScreen_displaysBackButton() {
        val video =
            Video(
                id = "test-video-id",
                title = "Test Video",
                thumbnailUrl = "",
            )

        var backClicked = false

        composeTestRule.setContent {
            PlayerScreen(
                video = video,
                onBackClick = { backClicked = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backClicked)
    }
}
