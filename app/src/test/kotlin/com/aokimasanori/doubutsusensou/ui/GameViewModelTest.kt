package com.aokimasanori.doubutsusensou.ui

import androidx.lifecycle.SavedStateHandle
import com.aokimasanori.doubutsusensou.game.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameViewModelTest {
    @Test
    fun playAndReturnClearTheSessionAndAllowAnotherGame() {
        val model = GameViewModel(SavedStateHandle())
        assertEquals(AppScreen.TITLE, model.uiState.value.screen)
        assertNull(model.uiState.value.game)
        model.startNewGame()
        assertEquals(AppScreen.INITIAL_SETUP, model.uiState.value.screen)
        assertEquals(Player.ONE, model.uiState.value.game?.setupPlayer)
        model.returnToTitle()
        assertEquals(AppScreen.TITLE, model.uiState.value.screen)
        assertNull(model.uiState.value.game)
        model.startNewGame()
        assertEquals(Player.ONE, model.uiState.value.game?.setupPlayer)
    }

    @Test
    fun savedScreenRestoresSetupAfterProcessRecreation() {
        val saved = SavedStateHandle()
        GameViewModel(saved).startNewGame()
        val restored = GameViewModel(SavedStateHandle(mapOf("screen" to saved.get<String>("screen"))))
        assertEquals(AppScreen.INITIAL_SETUP, restored.uiState.value.screen)
        assertEquals(Player.ONE, restored.uiState.value.game?.setupPlayer)
    }

    @Test
    fun unknownSavedScreenFallsBackToTitle() {
        val model = GameViewModel(SavedStateHandle(mapOf("screen" to "unknown")))
        assertEquals(AppScreen.TITLE, model.uiState.value.screen)
        assertNull(model.uiState.value.game)
    }
}
