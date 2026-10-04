package com.aokimasanori.doubutsusensou.ui

import androidx.lifecycle.SavedStateHandle
import com.aokimasanori.doubutsusensou.game.*
import org.junit.Assert.*
import org.junit.Test

class PlacementViewModelTest {
    @Test fun positionsSurviveSavedStateRestoreAndAreCoveredUntilReady() {
        val saved = SavedStateHandle()
        val model = GameViewModel(saved)
        model.startNewGame()
        model.selectPiece(0)
        model.tapCell(Cell(0, 3))
        val snapshot = saved.keys().associateWith { saved.get<Any?>(it) }
        val restored = GameViewModel(SavedStateHandle(snapshot))
        assertEquals(Cell(0, 2), restored.uiState.value.game?.placements?.get(0))
        assertTrue(restored.uiState.value.privacyCovered)
        restored.selectPiece(0)
        restored.tapCell(Cell(1, 0))
        assertEquals(Cell(0, 2), restored.uiState.value.game?.placements?.get(0))
        restored.acceptHandoff()
        restored.selectPiece(0)
        restored.tapCell(Cell(1, 0))
        assertEquals(Cell(1, 0), restored.uiState.value.game?.placements?.get(0))
    }

    @Test fun bridgeErrorKeepsSelectionAndDoesNotPlacePit() {
        val model = GameViewModel(SavedStateHandle())
        model.startNewGame()
        model.selectPiece(8)
        model.tapCell(Cell(3, 1))
        assertEquals(PlacementError.PIT_AT_BRIDGE, model.uiState.value.error)
        assertEquals(8, model.uiState.value.selectedPieceId)
        assertTrue(model.uiState.value.game!!.placements.isEmpty())
        model.tapCell(Cell(0, 2))
        assertNull(model.uiState.value.selectedPieceId)
        assertNull(model.uiState.value.error)
    }

    @Test fun handoffClearsSelectionAndCannotEditConfirmedTeam() {
        val model = GameViewModel(SavedStateHandle())
        model.startNewGame()
        repeat(10) { i -> model.selectPiece(i); model.tapCell(Cell(1 + i / 6, i % 6)) }
        model.selectPiece(0)
        model.confirmPlacement()
        assertEquals(GamePhase.HANDOFF, model.uiState.value.game?.phase)
        assertNull(model.uiState.value.selectedPieceId)
        model.selectPiece(10)
        assertNull(model.uiState.value.selectedPieceId)
        model.acceptHandoff()
        model.selectPiece(0)
        assertNull(model.uiState.value.selectedPieceId)
        model.tapCell(Cell(1, 0))
        assertNull(model.uiState.value.selectedPieceId)
        model.selectPiece(10)
        model.tapCell(Cell(6, 0))
        model.hideForPrivacy()
        assertTrue(model.uiState.value.privacyCovered)
        assertNull(model.uiState.value.selectedPieceId)
        model.returnToTitle()
        model.startNewGame()
        assertTrue(model.uiState.value.game!!.placements.isEmpty())
    }
}
