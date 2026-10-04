package com.aokimasanori.doubutsusensou.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aokimasanori.doubutsusensou.ui.screens.InitialSetupScreen
import com.aokimasanori.doubutsusensou.ui.screens.TitleScreen

@Composable
fun DoubutsuSensouApp(gameViewModel: GameViewModel = viewModel()) {
    val state by gameViewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.screen == AppScreen.INITIAL_SETUP) {
        gameViewModel.returnToTitle()
    }
    when (state.screen) {
        AppScreen.TITLE -> TitleScreen(onPlay = gameViewModel::startNewGame)
        AppScreen.INITIAL_SETUP -> InitialSetupScreen(
            gameState = requireNotNull(state.game),
            onBack = gameViewModel::returnToTitle,
        )
    }
}
