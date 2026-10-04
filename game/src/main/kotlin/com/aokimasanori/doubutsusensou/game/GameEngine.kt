package com.aokimasanori.doubutsusensou.game

/** Pure rule transitions. UI must never bypass this validation. */
class GameEngine {
    fun newGame() = GameState()

    fun placementError(piece: Piece, cell: Cell): PlacementError? = when {
        !Board.contains(cell) || Board.territory(cell) != piece.owner -> PlacementError.OUTSIDE_TERRITORY
        piece.kind == PieceKind.PIT && Board.isBridgeExit(cell) -> PlacementError.PIT_AT_BRIDGE
        piece.kind == PieceKind.BIRD && Board.isHome(cell) -> PlacementError.BIRD_AT_HOME
        else -> null
    }

    fun place(state: GameState, pieceId: Int, target: Cell): PlacementResult {
        if (state.phase != GamePhase.INITIAL_PLACEMENT) return PlacementResult(state, PlacementError.NOT_PREPARING)
        val piece = Pieces.find(pieceId)
        if (piece == null || piece.owner != state.setupPlayer) return PlacementResult(state, PlacementError.NOT_YOUR_PIECE)
        val cell = Board.canonical(target)
        placementError(piece, cell)?.let { return PlacementResult(state, it) }
        val occupant = state.pieceAt(cell)
        val previous = state.placements[pieceId]
        if (occupant != null && occupant.id != pieceId && previous != null) {
            // Both ends of a swap must satisfy the piece-specific restrictions.
            placementError(occupant, previous)?.let { return PlacementResult(state, it) }
        }
        val placements = state.placements.toMutableMap()
        if (occupant != null && occupant.id != pieceId) {
            if (previous == null) placements.remove(occupant.id)
            else placements[occupant.id] = previous
        }
        placements[pieceId] = cell
        return PlacementResult(state.copy(placements = placements.toMap()))
    }

    fun remove(state: GameState, pieceId: Int): GameState =
        if (state.phase == GamePhase.INITIAL_PLACEMENT && Pieces.find(pieceId)?.owner == state.setupPlayer)
            state.copy(placements = state.placements - pieceId)
        else state

    fun confirm(state: GameState): GameState {
        if (state.phase != GamePhase.INITIAL_PLACEMENT || state.placedCount(state.setupPlayer) != 10) return state
        return if (state.setupPlayer == Player.ONE)
            state.copy(phase = GamePhase.HANDOFF, setupPlayer = Player.TWO)
        else state.copy(phase = GamePhase.READY)
    }

    fun acceptHandoff(state: GameState): GameState =
        if (state.phase == GamePhase.HANDOFF) state.copy(phase = GamePhase.INITIAL_PLACEMENT) else state
}
