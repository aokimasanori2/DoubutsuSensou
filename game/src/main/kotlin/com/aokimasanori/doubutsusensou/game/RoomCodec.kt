package com.aokimasanori.doubutsusensou.game

/** Versioned wire format. Identifiers and board positions are shared only with room participants. */
object RoomCodec {
    fun encode(room: OnlineRoom): Map<String, Any> = mapOf(
        "schema" to 1L, "hostUid" to room.hostUid, "guestUid" to room.guestUid,
        "phase" to room.phase.name, "readyOne" to room.readyOne, "readyTwo" to room.readyTwo,
        "revision" to room.revision, "expiresAt" to room.expiresAt,
        "positions" to List(20) { id -> room.game.placements[id]?.let { (it.row * 6 + it.column).toLong() } ?: -1L },
        "activePlayer" to room.game.activePlayer.name, "turn" to room.game.turnNumber.toLong(),
        "winner" to (room.game.outcome?.winner?.name ?: ""),
        "reason" to (room.game.outcome?.reason?.name ?: ""),
    )

    fun decode(data: Map<String, Any?>): OnlineRoom {
        fun text(key: String) = data[key] as? String ?: throw RoomProblem("format")
        fun number(key: String) = (data[key] as? Number)?.toLong() ?: throw RoomProblem("format")
        fun flag(key: String) = data[key] as? Boolean ?: throw RoomProblem("format")
        if (number("schema") != 1L) throw RoomProblem("version")
        try {
            val phase = RoomPhase.valueOf(text("phase"))
            val positions = data["positions"] as? List<*> ?: throw RoomProblem("format")
            if (positions.size != 20) throw RoomProblem("format")
            val placed = mutableMapOf<Int, Cell>()
            positions.forEachIndexed { id, value ->
                val encoded = (value as? Number)?.toInt() ?: throw RoomProblem("format")
                if (encoded != -1) {
                    if (encoded !in 0..53) throw RoomProblem("format")
                    val cell = Cell(encoded / 6, encoded % 6)
                    if (Board.canonical(cell) != cell || cell in placed.values ||
                        Board.terrain(cell) == Terrain.RIVER) throw RoomProblem("format")
                    placed[id] = cell
                }
            }
            val reason = text("reason").takeIf { it.isNotEmpty() }?.let(WinReason::valueOf)
            val winner = text("winner").takeIf { it.isNotEmpty() }?.let(Player::valueOf)
            if (phase == RoomPhase.FINISHED && (reason == null || (reason != WinReason.DRAW && winner == null)))
                throw RoomProblem("format")
            val turn = number("turn")
            if (turn !in 1..Int.MAX_VALUE.toLong() || number("revision") < 0 || text("hostUid").isBlank()) throw RoomProblem("format")
            return OnlineRoom(
                text("hostUid"), text("guestUid"), phase,
                GameState(
                    phase = when (phase) {
                        RoomPhase.PLAYING -> GamePhase.PLAYING
                        RoomPhase.FINISHED -> GamePhase.FINISHED
                        else -> GamePhase.INITIAL_PLACEMENT
                    },
                    placements = placed, activePlayer = Player.valueOf(text("activePlayer")),
                    turnNumber = turn.toInt(), outcome = reason?.let { GameOutcome(winner, it) },
                ),
                flag("readyOne"), flag("readyTwo"), number("revision"), number("expiresAt"),
            )
        } catch (e: IllegalArgumentException) { throw RoomProblem("format") }
    }
}
