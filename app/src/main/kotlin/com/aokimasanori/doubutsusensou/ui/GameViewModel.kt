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
)

class GameViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val engine = GameEngine()
    private val mutableUiState = MutableStateFlow(restore())
    val uiState: StateFlow<AppUiState> = mutableUiState.asStateFlow()

    private fun restore(): AppUiState {
        if (savedStateHandle.get<String>("screen") != AppScreen.INITIAL_SETUP.name) return AppUiState()
        val positions = savedStateHandle.get<IntArray>("positions") ?: IntArray(20) { -1 }
        var game = engine.newGame()
        // Replay through the domain validator; never restore invalid placements.
        for (player in Player.entries) {
            game = game.copy(setupPlayer = player)
            for (piece in Pieces.forPlayer(player)) {
                val encoded = positions.getOrNull(piece.id) ?: -1
                if (encoded in 0 until Board.ROWS * Board.COLUMNS) {
                    val cell = Cell(encoded / Board.COLUMNS, encoded % Board.COLUMNS)
                    if (game.pieceAt(cell) == null) game = engine.place(game, piece.id, cell).state
                }
            }
        }
        val player = Player.entries.find { it.name == savedStateHandle.get<String>("player") } ?: Player.ONE
        val phase = GamePhase.entries.find { it.name == savedStateHandle.get<String>("phase") } ?: GamePhase.INITIAL_PLACEMENT
        game = game.copy(setupPlayer = player, phase = phase)
        return AppUiState(AppScreen.INITIAL_SETUP, game,
            privacyCovered = game.placements.isNotEmpty() && phase == GamePhase.INITIAL_PLACEMENT)
    }

    private fun update(state: AppUiState) {
        savedStateHandle["screen"] = state.screen.name
        savedStateHandle["player"] = state.game?.setupPlayer?.name
        savedStateHandle["phase"] = state.game?.phase?.name
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
        if (state.privacyCovered || game.phase != GamePhase.INITIAL_PLACEMENT || Pieces.find(id)?.owner != game.setupPlayer) return
        update(state.copy(selectedPieceId = if (state.selectedPieceId == id) null else id, error = null))
    }

    fun tapCell(cell: Cell) {
        val state = uiState.value
        val game = state.game ?: return
        if (state.privacyCovered || game.phase != GamePhase.INITIAL_PLACEMENT) return
        val selected = state.selectedPieceId
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

    fun hideForPrivacy() {
        val state = uiState.value
        if (state.game?.phase == GamePhase.INITIAL_PLACEMENT && state.game.placements.isNotEmpty())
            update(state.copy(privacyCovered = true, selectedPieceId = null, error = null))
    }
}
