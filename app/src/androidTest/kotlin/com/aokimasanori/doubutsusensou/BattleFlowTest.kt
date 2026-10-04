package com.aokimasanori.doubutsusensou

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.aokimasanori.doubutsusensou.game.*
import com.aokimasanori.doubutsusensou.ui.*
import com.aokimasanori.doubutsusensou.ui.theme.DoubutsuSensouTheme
import org.junit.Rule
import org.junit.Test

class BattleFlowTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private fun start(vararg pieces: Pair<Int, Cell>) {
        val model = GameViewModel(SavedStateHandle(mapOf(
            "screen" to AppScreen.INITIAL_SETUP.name, "phase" to GamePhase.PLAYING.name,
            "activePlayer" to Player.ONE.name,
            "positions" to IntArray(20) { id -> pieces.toMap()[id]?.let { it.row * 6 + it.column } ?: -1 },
        )))
        composeRule.runOnUiThread {
            composeRule.activity.setContent { DoubutsuSensouTheme { DoubutsuSensouApp(model) } }
        }
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("accept_handoff").performScrollTo().performClick()
    }

    @Test fun battleResultAndNextTurnKeepEnemyIdentityPrivate() {
        start(0 to Cell(5, 2), 14 to Cell(5, 3), 6 to Cell(4, 1))
        composeRule.onNodeWithTag("cell_5_3").assertContentDescriptionEquals("6だん 4れつ ？")
        composeRule.onNodeWithTag("cell_5_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_5_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("turn_result").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("cell_5_3").assertContentDescriptionEquals("6だん 4れつ ？")
        composeRule.onNodeWithTag("end_turn").performClick()
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("accept_handoff").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_5_3").assertContentDescriptionEquals("6だん 4れつ うさぎ")
        composeRule.onNodeWithTag("cell_4_1").assertContentDescriptionEquals("5だん 2れつ ？")
    }

    @Test fun survivingTrapAtHomeWinsAndPlayAgainStartsFresh() {
        start(6 to Cell(7, 3), 18 to Cell(8, 2), 10 to Cell(6, 5))
        composeRule.onNodeWithTag("cell_7_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_8_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("match_result").assertTextEquals("草チームの かち！")
        composeRule.onNodeWithTag("board").assertDoesNotExist()
        composeRule.onNodeWithTag("play_again").performScrollTo().performClick()
        composeRule.onNodeWithTag("setup_title").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_placement").assertIsNotEnabled()
    }
}
