package com.aokimasanori.doubutsusensou

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun playOpensSetupAndReturnButtonOpensTitle() {
        composeRule.onNodeWithTag("play_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("setup_title").assertIsDisplayed()
        composeRule.onNodeWithTag("back_to_title").performScrollTo().performClick()
        composeRule.onNodeWithTag("play_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun setupSurvivesActivityRecreationAndBackDispatchReturnsToTitle() {
        composeRule.onNodeWithTag("play_button").performScrollTo().performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("setup_title").assertIsDisplayed()
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("play_button").performScrollTo().assertIsDisplayed()
    }
}
