package com.aokimasanori.doubutsusensou.game

import org.junit.Assert.*
import org.junit.Test

class MatchEngineTest {
    private fun playing(vararg positions: Pair<Int, Cell>) =
        GameState(phase = GamePhase.PLAYING, placements = mapOf(*positions))

    @Test fun allFiftySixCombatPairsMatchTheRuleTable() {
        // Columns: lion, tiger, cheetah, fox, rabbit, bird, mole, pit.
        val expected = listOf("BAAADAAB", "DBAAAAAB", "DDBAAAAB", "DDDBADAB", "ADDDBDAB", "DDDAABAA", "DDDDDDBA")
        PieceKind.entries.filter { it != PieceKind.PIT }.forEachIndexed { row, attacker ->
            PieceKind.entries.forEachIndexed { col, defender ->
                val actual = when (Combat.resolve(attacker, defender)) {
                    CombatResult.ATTACKER_WINS -> 'A'
                    CombatResult.DEFENDER_WINS -> 'D'
                    CombatResult.BOTH_REMOVED -> 'B'
                }
                assertEquals("$attacker against $defender", expected[row][col], actual)
            }
        }
    }

    @Test fun normalAnimalsUseBridgesAndCannotMoveDiagonallyOrThroughFriends() {
        val state = playing(0 to Cell(3, 1), 1 to Cell(3, 0), 10 to Cell(3, 2))
        assertEquals(setOf(Cell(2, 1), Cell(4, 1), Cell(3, 2)), Movement.destinations(state, 0))
        val byWater = playing(0 to Cell(3, 2))
        assertFalse(Cell(4, 2) in Movement.destinations(byWater, 0))
        assertFalse(Cell(5, 2) in Movement.destinations(byWater, 0))
        assertFalse(Cell(2, 3) in Movement.destinations(byWater, 0))
        assertTrue(Movement.destinations(playing(8 to Cell(2, 2)), 8).isEmpty())
    }

    @Test fun cheetahStopsAtFriendsWaterAndTheFirstEnemy() {
        val state = playing(2 to Cell(3, 1), 0 to Cell(1, 1), 10 to Cell(6, 1))
        val moves = Movement.destinations(state, 2)
        assertTrue(moves.containsAll(setOf(Cell(2, 1), Cell(4, 1), Cell(5, 1), Cell(6, 1))))
        assertFalse(Cell(0, 1) in moves)
        assertFalse(Cell(1, 1) in moves)
        assertFalse(Cell(7, 1) in moves)
        assertFalse(Cell(5, 2) in moves)
        assertFalse(Cell(5, 2) in Movement.destinations(playing(2 to Cell(3, 2)), 2))
    }

    @Test fun birdFliesOverBothTeamsAndWaterButCannotLandOnWaterFriendsOrHomes() {
        val state = playing(5 to Cell(2, 3), 0 to Cell(3, 3), 10 to Cell(5, 3))
        val moves = Movement.destinations(state, 5)
        assertTrue(moves.containsAll(setOf(Cell(1, 3), Cell(5, 3), Cell(6, 3), Cell(7, 3))))
        assertFalse(Cell(3, 3) in moves)
        assertFalse(Cell(4, 3) in moves)
        assertFalse(Cell(0, 2) in moves)
        assertFalse(Cell(8, 2) in moves)
        assertFalse(Cell(6, 2) in moves)
    }

    @Test fun mergedHomeHasFourEdgesAndLongMovesNeverTurnAtTheHouse() {
        assertEquals(setOf(Cell(0, 1), Cell(0, 4), Cell(1, 2), Cell(1, 3)),
            Movement.destinations(playing(0 to Cell(0, 2)), 0))
        assertTrue(Cell(8, 2) in Movement.destinations(playing(0 to Cell(7, 3)), 0))
        assertFalse(Cell(8, 2) in Movement.destinations(playing(0 to Cell(7, 1)), 0))
        val cheetah = Movement.destinations(playing(2 to Cell(0, 1)), 2)
        assertTrue(Cell(0, 4) in cheetah)
        assertFalse(Cell(1, 3) in cheetah)
        val enemyHouse = Movement.destinations(playing(2 to Cell(8, 1)), 2)
        assertTrue(Cell(8, 2) in enemyHouse)
        assertFalse(Cell(8, 4) in enemyHouse)
    }

    @Test fun movesDoNotRevealEnemyKindsThroughAvailableDestinations() {
        val base = playing(2 to Cell(5, 0), 10 to Cell(5, 2))
        val withPit = base.copy(placements = mapOf(2 to Cell(5, 0), 18 to Cell(5, 2)))
        assertEquals(Movement.destinations(base, 2), Movement.destinations(withPit, 2))
    }

    @Test fun trapsAreConsumedAndOnlyBirdOrMoleSurvives() {
        for (id in 0..7) {
            val before = playing(id to Cell(5, 2), 18 to Cell(5, 3), 10 to Cell(7, 5), 9 to Cell(1, 0))
            val after = MatchEngine.move(before, id, Cell(5, 3))
            assertFalse(18 in after.placements)
            assertEquals(id in setOf(5, 6, 7), id in after.placements)
            if (id in setOf(5, 6, 7)) assertEquals(Cell(5, 3), after.placements[id])
            assertEquals(Cell(5, 2), before.placements[id])
        }
    }

    @Test fun homeVictoryRequiresSurvivingTheFightAndAcceptsEitherEntrance() {
        for (column in 2..3) {
            val guarded = playing(6 to Cell(7, column), 18 to Cell(8, 2), 10 to Cell(6, 5))
            val winner = MatchEngine.move(guarded, 6, Cell(8, column))
            assertEquals(GameOutcome(Player.ONE, WinReason.HOME), winner.outcome)
            assertEquals(GamePhase.FINISHED, winner.phase)
            assertEquals(Cell(8, 2), winner.placements[6])
            assertEquals(winner, MatchEngine.move(winner, 10, Cell(6, 4)))
        }
        val failed = MatchEngine.move(playing(0 to Cell(7, 2), 18 to Cell(8, 2), 6 to Cell(2, 2), 10 to Cell(6, 5)), 0, Cell(8, 3))
        assertNull(failed.outcome)
        assertFalse(0 in failed.placements)
        assertFalse(18 in failed.placements)
        val bird = playing(5 to Cell(7, 2), 10 to Cell(6, 5))
        assertEquals(bird, MatchEngine.move(bird, 5, Cell(8, 2)))
    }

    @Test fun eliminationIgnoresPitsAndLastMutualCaptureIsADraw() {
        val win = MatchEngine.move(playing(0 to Cell(2, 2), 11 to Cell(2, 3), 18 to Cell(7, 5)), 0, Cell(2, 3))
        assertEquals(GameOutcome(Player.ONE, WinReason.NO_ANIMALS), win.outcome)
        assertTrue(18 in win.placements)
        val loss = MatchEngine.move(playing(0 to Cell(2, 2), 14 to Cell(2, 3), 8 to Cell(1, 5)), 0, Cell(2, 3))
        assertEquals(GameOutcome(Player.TWO, WinReason.NO_ANIMALS), loss.outcome)
        val draw = MatchEngine.move(playing(0 to Cell(2, 2), 10 to Cell(2, 3)), 0, Cell(2, 3))
        assertEquals(GameOutcome(null, WinReason.DRAW), draw.outcome)
    }

    @Test fun illegalOrRepeatedMovesAndWrongPhaseActionsAreNoOps() {
        val before = playing(0 to Cell(2, 2), 10 to Cell(6, 2))
        assertEquals(before, MatchEngine.move(before, 10, Cell(5, 2)))
        assertEquals(before, MatchEngine.move(before, 0, Cell(-1, 2)))
        assertEquals(before, MatchEngine.move(before, 0, Cell(3, 3)))
        assertEquals(before, MatchEngine.move(before, 999, Cell(2, 3)))
        val moved = MatchEngine.move(before, 0, Cell(2, 3))
        assertEquals(GamePhase.TURN_RESULT, moved.phase)
        assertEquals(Player.ONE, moved.activePlayer)
        assertEquals(moved, MatchEngine.move(moved, 0, Cell(2, 4)))
        val covered = MatchEngine.endTurn(moved)
        assertEquals(GamePhase.TURN_HANDOFF, covered.phase)
        assertEquals(Player.TWO, covered.activePlayer)
        assertEquals(2, covered.turnNumber)
        assertEquals(covered, MatchEngine.move(covered, 10, Cell(5, 2)))
        assertEquals(covered, MatchEngine.endTurn(covered))
        assertEquals(GamePhase.PLAYING, GameEngine().acceptHandoff(covered).phase)
    }

    @Test fun onlyBlockedPlayersCanPassAndBothBlockedIsADraw() {
        val blocked = playing(0 to Cell(0, 0), 8 to Cell(0, 1), 9 to Cell(1, 0), 10 to Cell(6, 0))
        val passed = MatchEngine.pass(blocked)
        assertEquals(GamePhase.TURN_HANDOFF, passed.phase)
        assertEquals(Player.TWO, passed.activePlayer)
        assertEquals(blocked.placements, passed.placements)
        val free = blocked.copy(activePlayer = Player.TWO)
        assertEquals(free, MatchEngine.pass(free))
        val both = blocked.copy(placements = blocked.placements + mapOf(10 to Cell(8, 5), 18 to Cell(8, 4), 19 to Cell(7, 5)))
        assertEquals(GameOutcome(null, WinReason.DRAW), MatchEngine.pass(both).outcome)
    }

    @Test fun completeGameFromPlacementThroughAlternatingTurnsToHomeVictory() {
        val setup = GameEngine()
        var game = setup.newGame()
        for (id in 0..9) {
            game = setup.place(game, id, if (id == 6) Cell(3, 1) else Cell(1 + id / 6, id % 6)).state
        }
        game = setup.acceptHandoff(setup.confirm(game))
        val soilCells = listOf(Cell(5,0),Cell(5,2),Cell(5,3),Cell(5,5),Cell(6,2),Cell(6,3),Cell(6,5),Cell(7,0),Cell(7,3),Cell(7,5))
        soilCells.forEachIndexed { i, cell -> game = setup.place(game, 10 + i, cell).state }
        game = setup.acceptHandoff(setup.confirm(game))
        val route = listOf(Cell(4,1),Cell(5,1),Cell(6,1),Cell(7,1),Cell(7,2),Cell(8,2))
        route.forEachIndexed { i, cell ->
            game = MatchEngine.move(game, 6, cell)
            if (i != route.lastIndex) {
                game = setup.acceptHandoff(MatchEngine.endTurn(game))
                game = MatchEngine.move(game, 10, Cell(if (i % 2 == 0) 6 else 5, 0))
                game = setup.acceptHandoff(MatchEngine.endTurn(game))
                assertEquals(Player.ONE, game.activePlayer)
            }
        }
        assertEquals(GameOutcome(Player.ONE, WinReason.HOME), game.outcome)
        assertEquals(20, game.placements.values.toSet().size)
    }
}
