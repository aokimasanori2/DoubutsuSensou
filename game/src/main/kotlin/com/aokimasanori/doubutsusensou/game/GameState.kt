package com.aokimasanori.doubutsusensou.game

enum class Player { ONE, TWO }

enum class GamePhase { INITIAL_PLACEMENT }

/** Immutable domain state. Board, pieces and results will be added with their rules. */
data class GameState(
    val phase: GamePhase = GamePhase.INITIAL_PLACEMENT,
    val setupPlayer: Player = Player.ONE,
)
