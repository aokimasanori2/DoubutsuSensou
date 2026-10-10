package com.aokimasanori.doubutsusensou.online

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aokimasanori.doubutsusensou.game.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnlineUiState(
    val code: String? = null,
    val room: OnlineRoom? = null,
    val player: Player? = null,
    val draft: GameState = GameState(),
    val selected: Int? = null,
    val placementError: PlacementError? = null,
    val error: String? = null,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val savedCode: String? = null,
)

class OnlineViewModel(
    private val repository: RoomRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnlineUiState(savedCode = repository.savedRoom))
    val state = mutableState.asStateFlow()
    private var unsubscribe: (() -> Unit)? = null
    private val engine = GameEngine()

    fun open() {
        stop()
        if (state.value.code != null) {
            mutableState.value = state.value.copy(connected = false)
            resumeRoom()
        } else mutableState.value = OnlineUiState(savedCode = repository.savedRoom)
    }
    fun stop() { unsubscribe?.invoke(); unsubscribe = null }
    override fun onCleared() { stop() }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try { withTimeout(20_000) { block() } }
            catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                mutableState.value = state.value.copy(error = message(error))
            } finally { mutableState.value = state.value.copy(busy = false) }
        }
    }
    fun createRoom() = action {
        val (code, room) = repository.create()
        enter(code, room)
    }
    fun joinRoom(code: String) = action {
        val normalized = code.filterNot(Char::isWhitespace)
        if (!normalized.matches(Regex("[0-9]{8}"))) throw RoomProblem("code")
        enter(normalized, repository.join(normalized))
    }
    fun resumeRoom() {
        val code = repository.savedRoom ?: return
        action { enter(code, repository.resume(code)) }
    }
    private fun enter(code: String, room: OnlineRoom) {
        stop()
        if (room.player(repository.uid ?: "") == null) throw RoomProblem("not_member")
        repository.savedRoom = code
        mutableState.value = state.value.copy(code = code, savedCode = code, room = null)
        receive(room, true)
        unsubscribe = repository.watch(code) { update, online, error ->
            if (state.value.code == code) {
                if (error != null) mutableState.value = state.value.copy(connected = false, error = message(error))
                else if (update != null) receive(update, online)
            }
        }
    }

    private fun receive(room: OnlineRoom, online: Boolean) {
        val current = state.value
        if (room.revision < (current.room?.revision ?: -1)) return
        val player = room.player(repository.uid ?: "") ?: return
        var draft = current.draft
        if (room.phase == RoomPhase.SETUP && !room.ready(player) &&
            (current.room?.phase != RoomPhase.SETUP || draft.setupPlayer != player)) {
            draft = GameState(setupPlayer = player)
            if (saved.get<String>("draft_code") == current.code) {
                val positions = saved.get<IntArray>("draft_positions") ?: intArrayOf()
                Pieces.forPlayer(player).forEach { piece ->
                    val encoded = positions.getOrNull(piece.id) ?: -1
                    if (encoded >= 0) draft = engine.place(draft, piece.id, Cell(encoded / 6, encoded % 6)).state
                }
            }
        }
        mutableState.value = current.copy(
            room = room, player = player, connected = online, draft = draft,
            selected = if (room.revision != current.room?.revision) null else current.selected,
            error = if (online) null else current.error,
        )
    }

    private fun saveDraft(game: GameState) {
        saved["draft_code"] = state.value.code
        saved["draft_positions"] = IntArray(20) { game.placements[it]?.let { cell -> cell.row * 6 + cell.column } ?: -1 }
    }
    fun select(id: Int) {
        val current = state.value
        val room = current.room ?: return
        if (current.busy || room.phase != RoomPhase.SETUP || current.player == null ||
            room.ready(current.player) || Pieces.find(id)?.owner != current.player) return
        mutableState.value = current.copy(selected = if (current.selected == id) null else id,
            placementError = null, error = null)
    }
    fun remove() {
        val current = state.value
        val id = current.selected ?: return
        if (current.busy || current.room?.phase != RoomPhase.SETUP || current.room.ready(current.player ?: return)) return
        val draft = engine.remove(current.draft, id)
        saveDraft(draft)
        mutableState.value = current.copy(draft = draft, selected = null, placementError = null)
    }
    fun tap(cell: Cell) {
        val current = state.value
        val room = current.room ?: return
        val player = current.player ?: return
        if (current.busy) return
        if (room.phase == RoomPhase.SETUP && !room.ready(player)) {
            val selected = current.selected
            if (selected == null) current.draft.pieceAt(cell)?.let { select(it.id) }
            else {
                val result = engine.place(current.draft, selected, cell)
                saveDraft(result.state)
                mutableState.value = current.copy(draft = result.state, placementError = result.error,
                    selected = if (result.error == null) null else selected)
            }
        } else if (room.phase == RoomPhase.PLAYING && room.game.activePlayer == player && current.connected) {
            val own = room.game.pieceAt(cell)?.takeIf { it.owner == player && it.kind != PieceKind.PIT }
            if (own != null) mutableState.value = current.copy(selected = if (current.selected == own.id) null else own.id, error = null)
            else {
                val selected = current.selected ?: return
                if (cell !in Movement.destinations(room.game, selected))
                    mutableState.value = current.copy(error = "そのマスには うごけないよ。")
                else action { repository.move(current.code!!, room.revision, selected, cell) }
            }
        }
    }
    fun confirm() {
        val current = state.value
        if (!current.connected || current.draft.placedCount(current.player ?: return) != 10) return
        action { repository.prepare(current.code!!, current.draft.placements) }
    }
    fun pass() {
        val current = state.value
        val room = current.room ?: return
        if (!current.connected || room.game.activePlayer != current.player) return
        action { repository.pass(current.code!!, room.revision) }
    }
    fun leave() {
        val current = state.value
        action {
            repository.leave(current.code!!)
            forgetRoom()
        }
    }
    fun forgetRoom() {
        stop()
        repository.savedRoom = null
        saved["draft_code"] = null
        saved["draft_positions"] = null
        mutableState.value = OnlineUiState()
    }

    private fun message(error: Throwable): String {
        val problem = generateSequence(error) { it.cause }.filterIsInstance<RoomProblem>().firstOrNull()
        return when (problem?.code) {
            "code" -> "へやばんごうは 8けたの すうじだよ。"
            "not_found" -> "そのへやは みつからないよ。ばんごうを かくにんしてね。"
            "full", "not_member" -> "そのへやには はいれないよ。2人そろっているか、べつのへやです。"
            "expired" -> "このへやの きげんが きれました。あたらしい へやを つくってね。"
            "closed" -> "このへやは おわりました。"
            "stale", "not_your_turn", "already_ready" -> "がめんが こうしんされました。いまの ばんを かくにんしてね。"
            "placement" -> "10この こまを じぶんの ばしょに ならべてね。"
            "illegal_move" -> "そのマスには うごけないよ。"
            "version", "format" -> "アプリを あたらしい ばんに こうしんしてね。"
            else -> if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                "へやに つながりません。ばんごうや へやの きげんを かくにんしてね。"
            else "つうしんを かくにんして、もういちど ためしてね。へやばんごうから もどれます。"
        }
    }
}
