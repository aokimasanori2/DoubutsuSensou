package com.aokimasanori.doubutsusensou.game

enum class RoomPhase { WAITING, SETUP, PLAYING, FINISHED, CLOSED }

/** Shared game state for a private, two-person room. No device-specific UI state is stored here. */
data class OnlineRoom(
    val hostUid: String,
    val guestUid: String = "",
    val phase: RoomPhase = RoomPhase.WAITING,
    val game: GameState = GameState(),
    val readyOne: Boolean = false,
    val readyTwo: Boolean = false,
    val revision: Long = 0,
    val expiresAt: Long,
) {
    fun player(uid: String): Player? = when (uid) {
        hostUid -> Player.ONE
        guestUid -> if (guestUid.isNotEmpty()) Player.TWO else null
        else -> null
    }
    fun ready(player: Player) = if (player == Player.ONE) readyOne else readyTwo
}

class RoomProblem(val code: String) : IllegalStateException(code)

object RoomEngine {
    private fun checkRoom(room: OnlineRoom, now: Long) {
        if (now >= room.expiresAt) throw RoomProblem("expired")
        if (room.phase == RoomPhase.CLOSED) throw RoomProblem("closed")
    }
    private fun participant(room: OnlineRoom, uid: String) =
        room.player(uid) ?: throw RoomProblem("not_member")

    fun join(room: OnlineRoom, uid: String, now: Long): OnlineRoom {
        checkRoom(room, now)
        if (room.player(uid) != null) return room
        if (room.phase != RoomPhase.WAITING || room.guestUid.isNotEmpty()) throw RoomProblem("full")
        if (uid.isBlank()) throw RoomProblem("not_member")
        return room.copy(guestUid = uid, phase = RoomPhase.SETUP, revision = room.revision + 1)
    }

    fun prepare(room: OnlineRoom, uid: String, positions: Map<Int, Cell>, now: Long): OnlineRoom {
        checkRoom(room, now)
        val player = participant(room, uid)
        if (room.phase != RoomPhase.SETUP || room.ready(player)) throw RoomProblem("already_ready")
        val engine = GameEngine()
        val pieces = Pieces.forPlayer(player)
        if (positions.keys != pieces.map { it.id }.toSet() ||
            positions.values.toSet().size != 10 ||
            pieces.any { engine.placementError(it, positions.getValue(it.id)) != null ||
                Board.canonical(positions.getValue(it.id)) != positions.getValue(it.id) })
            throw RoomProblem("placement")
        val one = room.readyOne || player == Player.ONE
        val two = room.readyTwo || player == Player.TWO
        return room.copy(
            readyOne = one, readyTwo = two, revision = room.revision + 1,
            phase = if (one && two) RoomPhase.PLAYING else RoomPhase.SETUP,
            game = room.game.copy(placements = room.game.placements + positions,
                phase = if (one && two) GamePhase.PLAYING else GamePhase.INITIAL_PLACEMENT),
        )
    }

    fun move(room: OnlineRoom, uid: String, revision: Long, id: Int, target: Cell, now: Long): OnlineRoom {
        checkTurn(room, uid, revision, now)
        return afterMove(room, MatchEngine.move(room.game, id, target))
    }

    fun pass(room: OnlineRoom, uid: String, revision: Long, now: Long): OnlineRoom {
        checkTurn(room, uid, revision, now)
        return afterMove(room, MatchEngine.pass(room.game))
    }

    private fun checkTurn(room: OnlineRoom, uid: String, revision: Long, now: Long) {
        checkRoom(room, now)
        if (room.revision != revision) throw RoomProblem("stale")
        if (room.phase != RoomPhase.PLAYING || participant(room, uid) != room.game.activePlayer)
            throw RoomProblem("not_your_turn")
    }

    private fun afterMove(room: OnlineRoom, moved: GameState): OnlineRoom {
        if (moved == room.game) throw RoomProblem("illegal_move")
        val game = GameEngine().acceptHandoff(MatchEngine.endTurn(moved))
        return room.copy(game = game, revision = room.revision + 1,
            phase = if (game.phase == GamePhase.FINISHED) RoomPhase.FINISHED else RoomPhase.PLAYING)
    }

    fun leave(room: OnlineRoom, uid: String, now: Long): OnlineRoom {
        checkRoom(room, now)
        val player = participant(room, uid)
        if (room.phase == RoomPhase.FINISHED) return room
        return if (room.phase == RoomPhase.PLAYING)
            room.copy(phase = RoomPhase.FINISHED, revision = room.revision + 1,
                game = room.game.copy(phase = GamePhase.FINISHED,
                    outcome = GameOutcome(player.opponent(), WinReason.RESIGNED)))
        else room.copy(phase = RoomPhase.CLOSED, revision = room.revision + 1)
    }
}
