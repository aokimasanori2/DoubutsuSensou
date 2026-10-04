package com.aokimasanori.doubutsusensou.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineTest {
    @Test
    fun newGameStartsWithFirstPlayerPreparing() {
        val game = GameEngine().newGame()
        assertEquals(GamePhase.INITIAL_PLACEMENT, game.phase)
        assertEquals(Player.ONE, game.setupPlayer)
    }
}
