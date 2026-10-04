package com.aokimasanori.doubutsusensou.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.aokimasanori.doubutsusensou.game.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen { TITLE, INITIAL_SETUP }

data class AppUiState(
    val screen: AppScreen = AppScreen.TITLE,
    val game: GameState? = null,
    val selectedPieceId: Int? = null,
    val error: PlacementError? = null,
    val privacyCovered: Boolean = false,
    val invalidMove: Boolean = false,
)

class GameViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val engine = GameEngine()
    private val mutableUiState = MutableStateFlow(restore())
    val uiState: StateFlow<AppUiState> = mutableUiState.asStateFlow()

    private fun restore(): AppUiState {
        if (savedStateHandle.get<String>("screen") != AppScreen.INITIAL_SETUP.name) return AppUiState()
        val positions = savedStateHandle.get<IntArray>("positions") ?: IntArray(20) { -1 }
        val phase = (GamePhase.entries.find { it.name == savedStateHandle.get<String>("phase") }
            ?: GamePhase.INITIAL_PLACEMENT).let { if (it == GamePhase.READY) GamePhase.TURN_HANDOFF else it }
        val setup = phase in setOf(GamePhase.INITIAL_PLACEMENT, GamePhase.HANDOFF)
        val placements = mutableMapOf<Int, Cell>()
        for (piece in Pieces.all) {
            val encoded = positions.getOrNull(piece.id) ?: -1
            if (encoded == -1) continue
            if (encoded !in 0 until Board.ROWS * Board.COLUMNS) return AppUiState()
            val cell = Board.canonical(Cell(encoded / Board.COLUMNS, encoded % Board.COLUMNS))
            if (cell in placements.values || Board.terrain(cell) == Terrain.RIVER ||
                (piece.kind == PieceKind.BIRD && Board.isHome(cell)) ||
                ((setup || piece.kind == PieceKind.PIT) && engine.placementError(piece, cell) != null)) return AppUiState()
            placements[piece.id] = cell
        }
        val player = Player.entries.find { it.name == savedStateHandle.get<String>("player") } ?: Player.ONE
        val active = Player.entries.find { it.name == savedStateHandle.get<String>("activePlayer") } ?: Player.ONE
        val reason = WinReason.entries.find { it.name == savedStateHandle.get<String>("winReason") }
        val winner = Player.entries.find { it.name == savedStateHandle.get<String>("winner") }
        if (phase == GamePhase.FINISHED && (reason == null || (winner == null && reason != WinReason.DRAW))) return AppUiState()
        val game = GameState(phase, player, placements.toMap(), active,
            (savedStateHandle.get<Int>("turn") ?: 1).coerceAtLeast(1),
            if (reason == null) null else GameOutcome(winner, reason))
        return AppUiState(AppScreen.INITIAL_SETUP, game,
            privacyCovered = game.placements.isNotEmpty() &&
                phase in setOf(GamePhase.INITIAL_PLACEMENT, GamePhase.PLAYING, GamePhase.TURN_RESULT))
    }

    private fun update(state: AppUiState) {
        savedStateHandle["screen"] = state.screen.name
        savedStateHandle["player"] = state.game?.setupPlayer?.name
        savedStateHandle["phase"] = state.game?.phase?.name
        savedStateHandle["activePlayer"] = state.game?.activePlayer?.name
        savedStateHandle["turn"] = state.game?.turnNumber
        savedStateHandle["winner"] = state.game?.outcome?.winner?.name
        savedStateHandle["winReason"] = state.game?.outcome?.reason?.name
        savedStateHandle["positions"] = IntArray(20) { id ->
            state.game?.placements?.get(id)?.let { it.row * Board.COLUMNS + it.column } ?: -1
        }
        mutableUiState.value = state
    }

    fun startNewGame() = update(AppUiState(AppScreen.INITIAL_SETUP, engine.newGame()))
    fun returnToTitle() = update(AppUiState())

    fun selectPiece(id: Int) {
        val state = uiState.value
        val game = state.game ?: return
        val piece = Pieces.find(id) ?: return
        if (state.privacyCovered || piece.owner != game.viewingPlayer) return
        if (game.phase == GamePhase.PLAYING) {
            if (id !in game.placements || piece.kind == PieceKind.PIT) return
        } else if (game.phase != GamePhase.INITIAL_PLACEMENT) return
        update(state.copy(selectedPieceId = if (state.selectedPieceId == id) null else id,
            error = null, invalidMove = false))
    }

    fun tapCell(cell: Cell) {
        val state = uiState.value
        val game = state.game ?: return
        if (state.privacyCovered) return
        val selected = state.selectedPieceId
        if (game.phase == GamePhase.PLAYING) {
            val ownPiece = game.pieceAt(cell)?.takeIf { it.owner == game.activePlayer }
            if (ownPiece != null) {
                selectPiece(ownPiece.id)
            } else if (selected != null) {
                val moved = MatchEngine.move(game, selected, cell)
                update(state.copy(game = moved, selectedPieceId = if (moved != game) null else selected,
                    invalidMove = moved == game))
            }
            return
        }
        if (game.phase != GamePhase.INITIAL_PLACEMENT) return
        if (selected == null) {
            game.pieceAt(cell)?.takeIf { it.owner == game.setupPlayer }?.let { selectPiece(it.id) }
        } else {
            val result = engine.place(game, selected, cell)
            update(state.copy(game = result.state, error = result.error,
                selectedPieceId = if (result.error == null) null else selected))
        }
    }

    fun removeSelected() {
        val state = uiState.value
        val game = state.game ?: return
        val selected = state.selectedPieceId ?: return
        if (state.privacyCovered) return
        update(state.copy(game = engine.remove(game, selected), selectedPieceId = null, error = null))
    }

    fun confirmPlacement() {
        val state = uiState.value
        val game = state.game ?: return
        if (state.privacyCovered) return
        update(state.copy(game = engine.confirm(game), selectedPieceId = null, error = null))
    }

    fun acceptHandoff() {
        val state = uiState.value
        val game = state.game ?: return
        update(state.copy(game = engine.acceptHandoff(game), privacyCovered = false))
    }

    fun endTurn() {
        val state = uiState.value
        val game = state.game ?: return
        if (!state.privacyCovered) update(state.copy(game = MatchEngine.endTurn(game), selectedPieceId = null))
    }

    fun passTurn() {
        val state = uiState.value
        val game = state.game ?: return
        if (!state.privacyCovered) update(state.copy(game = MatchEngine.pass(game), selectedPieceId = null))
    }

    fun hideForPrivacy() {
        val state = uiState.value
        if (state.game?.phase in setOf(GamePhase.INITIAL_PLACEMENT, GamePhase.PLAYING, GamePhase.TURN_RESULT) &&
            state.game?.placements?.isNotEmpty() == true)
            update(state.copy(privacyCovered = true, selectedPieceId = null, error = null, invalidMove = false))
    }
}
