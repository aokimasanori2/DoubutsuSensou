package com.aokimasanori.doubutsusensou

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bothTeamsPlaceTenPiecesWithPrivateHandoff() {
        composeRule.onNodeWithTag("play_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirm_placement").assertIsNotEnabled()
        repeat(10) { i ->
            composeRule.onNodeWithTag("piece_$i").performScrollTo().performClick()
            composeRule.onNodeWithTag("cell_${1 + i / 6}_${i % 6}").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("confirm_placement").performClick()
        composeRule.onNodeWithTag("handoff").assertIsDisplayed()
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("accept_handoff").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_1_0").assertContentDescriptionEquals("2だん 1れつ ？")
        repeat(10) { i ->
            composeRule.onNodeWithTag("piece_${10 + i}").performScrollTo().performClick()
            composeRule.onNodeWithTag("cell_${6 + i / 6}_${i % 6}").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("confirm_placement").performClick()
        composeRule.onNodeWithTag("turn_handoff").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("accept_handoff").performScrollTo().performClick()
        composeRule.onNodeWithTag("turn_title").assertIsDisplayed()
        composeRule.onNodeWithTag("cell_6_0").assertContentDescriptionEquals("7だん 1れつ ？")
    }

    @Test
    fun invalidPitPlacementAndRecreationKeepThePositionPrivate() {
        composeRule.onNodeWithTag("play_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("piece_8").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_3_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("placement_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("cell_0_2").performScrollTo().performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("handoff").assertIsDisplayed()
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("accept_handoff").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_0_2").assertContentDescriptionEquals("1だん 3れつ おうち おとしあな")
    }

    @Test
    fun playOpensSetupAndReturnButtonOpensTitle() {
        composeRule.onNodeWithTag("play_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("setup_title").assertIsDisplayed()
        composeRule.onNodeWithTag("back_to_title").performClick()
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
