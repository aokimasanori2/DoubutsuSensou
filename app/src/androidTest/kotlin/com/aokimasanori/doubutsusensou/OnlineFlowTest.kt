package com.aokimasanori.doubutsusensou

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.aokimasanori.doubutsusensou.game.*
import com.aokimasanori.doubutsusensou.online.*
import com.aokimasanori.doubutsusensou.ui.theme.DoubutsuSensouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnlineFlowTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun titleOpensRoomLobbyAndOnlyEightDigitsCanJoin() {
        composeRule.onNodeWithTag("online_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("online_lobby").assertIsDisplayed()
        composeRule.onNodeWithTag("join_room").assertIsNotEnabled()
        composeRule.onNodeWithTag("room_code_input").performScrollTo().performTextInput("1234567")
        composeRule.onNodeWithTag("join_room").assertIsNotEnabled()
        composeRule.onNodeWithTag("room_code_input").performTextInput("8")
        composeRule.onNodeWithTag("join_room").assertIsEnabled()
    }

    @Test fun guestKeepsOwnPerspectiveAndOnlyMovesOnConnectedOwnTurn() {
        val room = OnlineRoom("host", "guest", RoomPhase.PLAYING,
            GameState(phase = GamePhase.PLAYING, activePlayer = Player.ONE,
                placements = mapOf(0 to Cell(1,0), 10 to Cell(6,0))),
            readyOne = true, readyTwo = true, expiresAt = System.currentTimeMillis()+86400000)
        val state = mutableStateOf(OnlineUiState(code="12345678", room=room, player=Player.TWO, connected=true))
        var taps=0
        composeRule.runOnUiThread {
            composeRule.activity.setContent { DoubutsuSensouTheme {
                OnlineScreen(state.value, {}, {}, {}, {}, { taps++ }, {}, {}, {}, {}, {}, {})
            } }
        }
        composeRule.onNodeWithTag("cell_1_0").assertContentDescriptionEquals("2だん 1れつ ？")
        composeRule.onNodeWithTag("cell_6_0").assertContentDescriptionEquals("7だん 1れつ ライオン")
        composeRule.onNodeWithTag("cell_6_0").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("handoff").assertDoesNotExist()
        composeRule.runOnUiThread { state.value=state.value.copy(room=room.copy(game=room.game.copy(activePlayer=Player.TWO)),connected=false) }
        composeRule.onNodeWithTag("reconnect").assertIsDisplayed()
        composeRule.onNodeWithTag("cell_6_0").assertIsNotEnabled()
        composeRule.runOnUiThread { state.value=state.value.copy(connected=true) }
        composeRule.onNodeWithTag("cell_6_0").performScrollTo().assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1,taps) }
        composeRule.onNodeWithTag("cell_1_0").assertContentDescriptionEquals("2だん 1れつ ？")
    }

    @Test fun completedRoomShowsRelativeWinnerAndOffersNewRoom() {
        val room=OnlineRoom("host","guest",RoomPhase.FINISHED,
            GameState(phase=GamePhase.FINISHED,outcome=GameOutcome(Player.TWO,WinReason.RESIGNED)),
            expiresAt=System.currentTimeMillis()+86400000)
        composeRule.runOnUiThread { composeRule.activity.setContent { DoubutsuSensouTheme {
            OnlineScreen(OnlineUiState(code="12345678",room=room,player=Player.TWO,connected=true),
                {},{},{},{},{},{},{},{},{},{},{})
        } } }
        composeRule.onNodeWithTag("online_result").assertTextEquals("あなたの かち！")
        composeRule.onNodeWithTag("new_online_room").assertIsDisplayed()
        composeRule.onNodeWithTag("board").assertDoesNotExist()
    }
}
