package com.aokimasanori.doubutsusensou.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aokimasanori.doubutsusensou.ui.screens.InitialSetupScreen
import com.aokimasanori.doubutsusensou.ui.screens.TitleScreen

@Composable
fun DoubutsuSensouApp(gameViewModel: GameViewModel = viewModel()) {
    val state by gameViewModel.uiState.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    val onBack = {
        if (state.game?.placements?.isNotEmpty() == true) confirmExit = true
        else gameViewModel.returnToTitle()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) gameViewModel.hideForPrivacy()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = state.screen == AppScreen.INITIAL_SETUP, onBack = onBack)
    when (state.screen) {
        AppScreen.TITLE -> TitleScreen(onPlay = gameViewModel::startNewGame)
        AppScreen.INITIAL_SETUP -> InitialSetupScreen(
            state = state, onSelect = gameViewModel::selectPiece, onCell = gameViewModel::tapCell,
            onRemove = gameViewModel::removeSelected, onConfirm = gameViewModel::confirmPlacement,
            onContinue = gameViewModel::acceptHandoff, onBack = onBack,
        )
    }
    if (confirmExit) {
        AlertDialog(onDismissRequest = { confirmExit = false },
            title = { Text("タイトルに もどる？") },
            text = { Text("いまの ならべかたは リセットされるよ。") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; gameViewModel.returnToTitle() }) { Text("もどる") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("つづける") } })
    }
}
