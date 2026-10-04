package com.aokimasanori.doubutsusensou.game

enum class Player { ONE, TWO }
enum class GamePhase { INITIAL_PLACEMENT, HANDOFF, READY }
enum class PieceKind { LION, TIGER, CHEETAH, FOX, RABBIT, BIRD, MOLE, PIT }

data class Piece(val id: Int, val owner: Player, val kind: PieceKind)

object Pieces {
    private val kinds = listOf(
        PieceKind.LION, PieceKind.TIGER, PieceKind.CHEETAH, PieceKind.FOX,
        PieceKind.RABBIT, PieceKind.BIRD, PieceKind.MOLE, PieceKind.MOLE,
        PieceKind.PIT, PieceKind.PIT,
    )
    val all = Player.entries.flatMap { player ->
        kinds.mapIndexed { index, kind -> Piece(player.ordinal * 10 + index, player, kind) }
    }
    fun forPlayer(player: Player) = all.filter { it.owner == player }
    fun find(id: Int) = all.find { it.id == id }
}

data class GameState(
    val phase: GamePhase = GamePhase.INITIAL_PLACEMENT,
    val setupPlayer: Player = Player.ONE,
    val placements: Map<Int, Cell> = emptyMap(),
) {
    fun placedCount(player: Player) = Pieces.forPlayer(player).count { it.id in placements }
    fun pieceAt(cell: Cell): Piece? = placements.entries
        .firstOrNull { it.value == Board.canonical(cell) }?.key?.let(Pieces::find)
}

enum class PlacementError { NOT_PREPARING, NOT_YOUR_PIECE, OUTSIDE_TERRITORY, PIT_AT_BRIDGE, BIRD_AT_HOME }
data class PlacementResult(val state: GameState, val error: PlacementError? = null)
