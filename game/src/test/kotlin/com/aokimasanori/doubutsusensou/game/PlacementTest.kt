package com.aokimasanori.doubutsusensou.game

import org.junit.Assert.*
import org.junit.Test

class PlacementTest {
    private val engine = GameEngine()

    @Test fun mergedHouseOccupiesOneCellFromEitherColumn() {
        assertEquals(52, Board.cells.size)
        var game = engine.place(engine.newGame(), 0, Cell(0, 3)).state
        assertEquals(Cell(0, 2), game.placements[0])
        game = engine.place(game, 8, Cell(0, 2)).state
        assertEquals(1, game.placements.size)
        assertEquals(PieceKind.PIT, game.pieceAt(Cell(0, 3))?.kind)
    }

    @Test fun rosterHasTenPiecesAndTwoMolesAndTwoPitsPerPlayer() {
        Player.entries.forEach { player ->
            val pieces = Pieces.forPlayer(player)
            assertEquals(10, pieces.size)
            assertEquals(2, pieces.count { it.kind == PieceKind.MOLE })
            assertEquals(2, pieces.count { it.kind == PieceKind.PIT })
            assertEquals(8, pieces.map { it.kind }.toSet().size)
        }
        assertEquals(20, Pieces.all.map { it.id }.toSet().size)
    }

    @Test fun everyCellAndPieceUsesCorrectPlacementRestrictions() {
        for (piece in Pieces.all) {
            val game = engine.newGame().copy(setupPlayer = piece.owner)
            for (row in -1..9) for (col in -1..6) {
                val cell = Cell(row, col)
                val expected = row in 0..8 && col in 0..5 &&
                    (if (piece.owner == Player.ONE) row in 0..3 else row in 5..8) &&
                    !(piece.kind == PieceKind.PIT && row in listOf(3, 5) && col in listOf(1, 4)) &&
                    !(piece.kind == PieceKind.BIRD && row in listOf(0, 8) && col in listOf(2, 3))
                val result = engine.place(game, piece.id, cell)
                assertEquals("$piece at $cell", expected, result.error == null)
                if (!expected) assertEquals(game, result.state)
            }
        }
    }

    @Test fun replacingAndSwappingPreserveUniqueCellsAndRejectPitAtExit() {
        var game = engine.place(engine.newGame(), 0, Cell(3, 1)).state
        game = engine.place(game, 8, Cell(2, 1)).state
        val rejected = engine.place(game, 0, Cell(2, 1))
        assertEquals(PlacementError.PIT_AT_BRIDGE, rejected.error)
        assertEquals(game, rejected.state)
        game = engine.place(game, 0, Cell(2, 2)).state
        game = engine.place(game, 0, Cell(2, 1)).state
        assertEquals(Cell(2, 2), game.placements[8])
        assertEquals(Cell(2, 1), game.placements[0])
        game = engine.remove(game, 8)
        assertFalse(8 in game.placements)
        assertEquals(1, game.placements.size)
    }

    @Test fun incompleteAndForeignActionsDoNotChangeState() {
        val game = engine.newGame()
        assertEquals(game, engine.confirm(game))
        assertEquals(game, engine.place(game, 10, Cell(0, 0)).state)
        assertEquals(game, engine.place(game, -1, Cell(0, 0)).state)
        assertEquals(game, engine.remove(game, 10))
    }

    @Test fun bothTeamsMustConfirmAndHandoffBlocksAllPlacement() {
        var game = fill(engine.newGame())
        game = engine.confirm(game)
        assertEquals(GamePhase.HANDOFF, game.phase)
        assertEquals(Player.TWO, game.setupPlayer)
        assertEquals(game, engine.place(game, 10, Cell(6, 0)).state)
        assertEquals(game, engine.remove(game, 0))
        assertEquals(game, engine.confirm(game))
        game = engine.acceptHandoff(game)
        assertEquals(GamePhase.INITIAL_PLACEMENT, game.phase)
        assertEquals(10, game.placedCount(Player.ONE))
        game = engine.confirm(fill(game))
        assertEquals(GamePhase.READY, game.phase)
        assertEquals(20, game.placements.size)
        assertEquals(20, game.placements.values.toSet().size)
        assertEquals(game, engine.place(game, 10, Cell(7, 0)).state)
    }

    private fun fill(state: GameState): GameState {
        var game = state
        val start = if (state.setupPlayer == Player.ONE) 1 else 6
        Pieces.forPlayer(state.setupPlayer).forEachIndexed { i, piece ->
            game = engine.place(game, piece.id, Cell(start + i / 6, i % 6)).state
        }
        return game
    }
}
