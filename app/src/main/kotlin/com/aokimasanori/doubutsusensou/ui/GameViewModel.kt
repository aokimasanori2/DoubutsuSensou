package com.aokimasanori.doubutsusensou.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.aokimasanori.doubutsusensou.game.GameEngine
import com.aokimasanori.doubutsusensou.game.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen { TITLE, INITIAL_SETUP }

data class AppUiState(
    val screen: AppScreen = AppScreen.TITLE,
    val game: GameState? = null,
)

class GameViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val engine = GameEngine()
    private val mutableUiState = MutableStateFlow(
        if (savedStateHandle.get<String>(SCREEN_KEY) == AppScreen.INITIAL_SETUP.name) {
            AppUiState(AppScreen.INITIAL_SETUP, engine.newGame())
        } else {
            AppUiState()
        },
    )
    val uiState: StateFlow<AppUiState> = mutableUiState.asStateFlow()

    fun startNewGame() {
        savedStateHandle[SCREEN_KEY] = AppScreen.INITIAL_SETUP.name
        mutableUiState.value = AppUiState(AppScreen.INITIAL_SETUP, engine.newGame())
    }

    fun returnToTitle() {
        savedStateHandle[SCREEN_KEY] = AppScreen.TITLE.name
        mutableUiState.value = AppUiState()
    }

    private companion object { const val SCREEN_KEY = "screen" }
}
