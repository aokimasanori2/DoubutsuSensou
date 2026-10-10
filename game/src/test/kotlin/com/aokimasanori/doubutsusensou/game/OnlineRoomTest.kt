package com.aokimasanori.doubutsusensou.game

import org.junit.Assert.*
import org.junit.Test

class OnlineRoomTest {
    private val now = 1000L
    private fun setup() = RoomEngine.join(OnlineRoom("host", expiresAt = 100_000), "guest", now)
    private fun positions(player: Player) = Pieces.forPlayer(player).associate {
        val offset = if (player == Player.ONE) 6 else 36
        it.id to Cell((offset + it.id % 10) / 6, (offset + it.id % 10) % 6)
    }
    private fun playing(): OnlineRoom {
        val first = RoomEngine.prepare(setup(), "host", positions(Player.ONE), now)
        return RoomEngine.prepare(first, "guest", positions(Player.TWO), now)
    }
    private fun rejected(code: String, action: () -> Unit) {
        try { action(); fail("Expected rejection") } catch (e: RoomProblem) { assertEquals(code, e.code) }
    }

    @Test fun twoPlayersPrepareAndMoveWithoutHandoff() {
        val room = playing()
        assertEquals(RoomPhase.PLAYING, room.phase)
        val moved = RoomEngine.move(room, "host", room.revision, 0, Cell(0,0), now)
        assertEquals(Player.TWO, moved.game.activePlayer)
        assertEquals(GamePhase.PLAYING, moved.game.phase)
        assertEquals(2, moved.game.turnNumber)
        assertEquals(Cell(0,0), moved.game.placements[0])
        assertEquals(room.game.placements[10], moved.game.placements[10])
    }
    @Test fun staleMoveWrongTurnAndThirdPlayerAreRejected() {
        val room = playing()
        rejected("not_your_turn") { RoomEngine.move(room,"guest",room.revision,10,Cell(5,0),now) }
        val moved = RoomEngine.move(room,"host",room.revision,0,Cell(0,0),now)
        rejected("stale") { RoomEngine.move(moved,"host",room.revision,0,Cell(0,0),now) }
        rejected("full") { RoomEngine.join(room,"third",now) }
        assertEquals(room, RoomEngine.join(room,"host",now))
    }
    @Test fun setupCannotOverwriteOpponentOrChangeAfterReady() {
        val room = setup()
        rejected("placement") { RoomEngine.prepare(room,"host",positions(Player.TWO),now) }
        val ready = RoomEngine.prepare(room,"host",positions(Player.ONE),now)
        rejected("already_ready") { RoomEngine.prepare(ready,"host",positions(Player.ONE),now) }
        assertFalse(ready.readyTwo)
    }
    @Test fun resignationExpiryAndCodecRestoration() {
        val room = playing()
        assertEquals(room, RoomCodec.decode(RoomCodec.encode(room)))
        val ended = RoomEngine.leave(room,"guest",now)
        assertEquals(GameOutcome(Player.ONE,WinReason.RESIGNED),ended.game.outcome)
        assertEquals(ended,RoomCodec.decode(RoomCodec.encode(ended)))
        rejected("expired") { RoomEngine.move(room,"host",room.revision,0,Cell(0,0),100_000) }
        assertEquals(RoomPhase.CLOSED,RoomEngine.leave(setup(),"host",now).phase)
    }
    @Test fun malformedWireDataCannotBeRestored() {
        rejected("version") { RoomCodec.decode(RoomCodec.encode(playing()) + ("schema" to 2L)) }
        rejected("format") { RoomCodec.decode(RoomCodec.encode(playing()) + ("positions" to List(20) { 6L })) }
    }
}
