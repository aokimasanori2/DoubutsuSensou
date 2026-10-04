package com.aokimasanori.doubutsusensou.game

/** Entry point for game transitions; no Android, persistence or UI dependencies. */
class GameEngine {
    fun newGame(): GameState = GameState()
}
