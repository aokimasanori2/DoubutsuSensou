package com.aokimasanori.doubutsusensou.ui

import androidx.lifecycle.SavedStateHandle
import com.aokimasanori.doubutsusensou.game.*
import org.junit.Assert.*
import org.junit.Test

class BattleViewModelTest {
    private fun saved(vararg pieces: Pair<Int, Cell>) = SavedStateHandle(mapOf(
        "screen" to AppScreen.INITIAL_SETUP.name, "phase" to GamePhase.PLAYING.name,
        "activePlayer" to Player.ONE.name, "turn" to 7,
        "positions" to IntArray(20) { id -> pieces.toMap()[id]?.let { it.row * 6 + it.column } ?: -1 },
    ))

    @Test fun battleRestoreKeepsCrossRiverPositionsAndRemovedPieces() {
        val handle = saved(0 to Cell(5, 2), 14 to Cell(5, 3), 6 to Cell(4, 1))
        val model = GameViewModel(handle)
        assertTrue(model.uiState.value.privacyCovered)
        model.acceptHandoff()
        model.tapCell(Cell(5, 2))
        model.tapCell(Cell(5, 3))
        assertEquals(GamePhase.TURN_RESULT, model.uiState.value.game?.phase)
        assertFalse(0 in model.uiState.value.game!!.placements)
        val restored = GameViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertTrue(restored.uiState.value.privacyCovered)
        assertEquals(model.uiState.value.game, restored.uiState.value.game)
        restored.endTurn()
        assertEquals(GamePhase.TURN_RESULT, restored.uiState.value.game?.phase)
        restored.acceptHandoff()
        restored.endTurn()
        assertEquals(GamePhase.TURN_HANDOFF, restored.uiState.value.game?.phase)
        assertEquals(Player.TWO, restored.uiState.value.game?.activePlayer)
        assertEquals(8, restored.uiState.value.game?.turnNumber)
        restored.tapCell(Cell(5, 3))
        assertNull(restored.uiState.value.selectedPieceId)
        restored.acceptHandoff()
        restored.tapCell(Cell(5, 3))
        assertEquals(14, restored.uiState.value.selectedPieceId)
    }

    @Test fun gameOverRestoresAndNewGameClearsEverything() {
        val handle = saved(6 to Cell(7, 3), 18 to Cell(8, 2), 10 to Cell(6, 5))
        val model = GameViewModel(handle)
        model.acceptHandoff()
        model.tapCell(Cell(7, 3))
        model.tapCell(Cell(8, 2))
        val restored = GameViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(GameOutcome(Player.ONE, WinReason.HOME), restored.uiState.value.game?.outcome)
        assertEquals(GamePhase.FINISHED, restored.uiState.value.game?.phase)
        restored.startNewGame()
        assertEquals(GameState(), restored.uiState.value.game)
        assertNull(restored.uiState.value.selectedPieceId)
    }

    @Test fun backgroundCoverAndIllegalMovementDoNotChangeTheBoard() {
        val model = GameViewModel(saved(0 to Cell(2, 2), 10 to Cell(6, 2)))
        model.acceptHandoff()
        model.tapCell(Cell(2, 2))
        model.tapCell(Cell(3, 3))
        assertTrue(model.uiState.value.invalidMove)
        model.hideForPrivacy()
        assertNull(model.uiState.value.selectedPieceId)
        val before = model.uiState.value.game
        model.tapCell(Cell(2, 3))
        model.passTurn()
        assertEquals(before, model.uiState.value.game)
        assertTrue(model.uiState.value.privacyCovered)
    }

    @Test fun invalidSavedOccupancyIsRejectedInsteadOfStartingACorruptMatch() {
        val model = GameViewModel(saved(0 to Cell(2, 2), 10 to Cell(2, 2)))
        assertEquals(AppScreen.TITLE, model.uiState.value.screen)
    }
}
