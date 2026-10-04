package com.aokimasanori.doubutsusensou.game

enum class CombatResult { ATTACKER_WINS, DEFENDER_WINS, BOTH_REMOVED }

/** No combat result or enemy identity is exposed in the UI's turn message. */
object Combat {
    private val strength = mapOf(
        PieceKind.LION to 7, PieceKind.TIGER to 6, PieceKind.CHEETAH to 5,
        PieceKind.BIRD to 4, PieceKind.FOX to 3, PieceKind.RABBIT to 2, PieceKind.MOLE to 1,
    )
    fun resolve(attacker: PieceKind, defender: PieceKind): CombatResult {
        require(attacker != PieceKind.PIT) { "Pits cannot attack" }
        if (attacker == defender) return CombatResult.BOTH_REMOVED
        if (defender == PieceKind.PIT) return if (attacker in setOf(PieceKind.MOLE, PieceKind.BIRD))
            CombatResult.ATTACKER_WINS else CombatResult.BOTH_REMOVED
        if (attacker == PieceKind.RABBIT && defender == PieceKind.LION) return CombatResult.ATTACKER_WINS
        if (attacker == PieceKind.LION && defender == PieceKind.RABBIT) return CombatResult.DEFENDER_WINS
        return if (strength.getValue(attacker) > strength.getValue(defender))
            CombatResult.ATTACKER_WINS else CombatResult.DEFENDER_WINS
    }
}

object Movement {
    private val directions = listOf(Cell(-1, 0), Cell(1, 0), Cell(0, -1), Cell(0, 1))

    /** A merged house has two physical entrances but only one occupancy. Rays never turn. */
    fun destinations(state: GameState, id: Int): Set<Cell> {
        val piece = Pieces.find(id) ?: return emptySet()
        val origin = state.placements[id] ?: return emptySet()
        if (piece.kind == PieceKind.PIT) return emptySet()
        val footprints = if (Board.isHome(origin)) listOf(origin, origin.copy(column = 3)) else listOf(origin)
        val flying = piece.kind == PieceKind.BIRD
        val longRange = flying || piece.kind == PieceKind.CHEETAH
        val result = mutableSetOf<Cell>()
        for (start in footprints) for (direction in directions) {
            var current = Cell(start.row + direction.row, start.column + direction.column)
            while (Board.contains(current)) {
                val target = Board.canonical(current)
                if (target != origin) {
                    val water = Board.terrain(target) == Terrain.RIVER
                    val home = Board.isHome(target)
                    val occupant = state.pieceAt(target)
                    if (!flying && (water || occupant?.owner == piece.owner)) break
                    if (!water && !(flying && home) && occupant?.owner != piece.owner) result += target
                    if (!longRange || (!flying && (occupant != null || (home && Board.territory(target) != piece.owner)))) break
                }
                current = Cell(current.row + direction.row, current.column + direction.column)
            }
        }
        return result
    }

    fun hasMove(state: GameState, player: Player) =
        Pieces.forPlayer(player).any { destinations(state, it.id).isNotEmpty() }
}

object MatchEngine {
    fun move(state: GameState, id: Int, target: Cell): GameState {
        val piece = Pieces.find(id) ?: return state
        if (state.phase != GamePhase.PLAYING || piece.owner != state.activePlayer ||
            !Board.contains(target) || Board.canonical(target) !in Movement.destinations(state, id)) return state
        val cell = Board.canonical(target)
        val defender = state.pieceAt(cell)
        val positions = state.placements.toMutableMap()
        if (defender == null) positions[id] = cell
        else when (Combat.resolve(piece.kind, defender.kind)) {
            CombatResult.ATTACKER_WINS -> { positions.remove(defender.id); positions[id] = cell }
            CombatResult.DEFENDER_WINS -> positions.remove(id)
            CombatResult.BOTH_REMOVED -> { positions.remove(id); positions.remove(defender.id) }
        }
        val after = state.copy(placements = positions.toMap(), phase = GamePhase.TURN_RESULT)
        val homeReached = positions[id] == cell && Board.isHome(cell) && Board.territory(cell) != piece.owner
        val survivors = Player.entries.associateWith { player ->
            Pieces.forPlayer(player).any { it.kind != PieceKind.PIT && it.id in positions }
        }
        val outcome = when {
            homeReached -> GameOutcome(piece.owner, WinReason.HOME)
            survivors.values.none { it } -> GameOutcome(null, WinReason.DRAW)
            !survivors.getValue(Player.ONE) -> GameOutcome(Player.TWO, WinReason.NO_ANIMALS)
            !survivors.getValue(Player.TWO) -> GameOutcome(Player.ONE, WinReason.NO_ANIMALS)
            else -> null
        }
        return if (outcome == null) after else after.copy(phase = GamePhase.FINISHED, outcome = outcome)
    }

    fun endTurn(state: GameState): GameState =
        if (state.phase == GamePhase.TURN_RESULT)
            state.copy(phase = GamePhase.TURN_HANDOFF, activePlayer = state.activePlayer.opponent(),
                turnNumber = state.turnNumber + 1)
        else state

    /** Passing is permitted only when all surviving animals have blocked destinations. */
    fun pass(state: GameState): GameState {
        if (state.phase != GamePhase.PLAYING || Movement.hasMove(state, state.activePlayer)) return state
        return if (!Movement.hasMove(state, state.activePlayer.opponent()))
            state.copy(phase = GamePhase.FINISHED, outcome = GameOutcome(null, WinReason.DRAW))
        else state.copy(phase = GamePhase.TURN_HANDOFF, activePlayer = state.activePlayer.opponent(),
            turnNumber = state.turnNumber + 1)
    }
}
