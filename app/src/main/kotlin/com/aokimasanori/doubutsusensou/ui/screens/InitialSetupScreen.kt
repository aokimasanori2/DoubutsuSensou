package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aokimasanori.doubutsusensou.game.*
import com.aokimasanori.doubutsusensou.ui.AppUiState

fun Player.teamName() = if (this == Player.ONE) "草チーム" else "土チーム"

@Composable
fun InitialSetupScreen(
    state: AppUiState,
    onSelect: (Int) -> Unit,
    onCell: (Cell) -> Unit,
    onRemove: () -> Unit,
    onConfirm: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val game = requireNotNull(state.game)
    if (game.phase != GamePhase.INITIAL_PLACEMENT || state.privacyCovered) {
        ScreenLayout {
            PawEmblem()
            Spacer(Modifier.height(24.dp))
            Text(
                when {
                    state.privacyCovered -> "${game.setupPlayer.teamName()}の じゅんび"
                    else -> "${game.setupPlayer.teamName()}に\nスマホを わたしてね"
                },
                Modifier.testTag("handoff"),
                textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "あいては がめんを みないでね。",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("accept_handoff")) {
                Text("じゅんびOK！", Modifier.padding(10.dp))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("back_to_title")) {
                Text("タイトルへ もどる", Modifier.padding(8.dp))
            }
        }
        return
    }
    Scaffold(bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.testTag("back_to_title")) { Text("もどる") }
                Button(onClick = onConfirm, enabled = game.placedCount(game.setupPlayer) == 10,
                    modifier = Modifier.weight(1f).testTag("confirm_placement")) {
                    Text("これでOK！  ${game.placedCount(game.setupPlayer)}/10", Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${game.setupPlayer.teamName()}の じゅんび", Modifier.testTag("setup_title"),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("こまを タップ → おきたい マスを タップ", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Pieces.forPlayer(game.setupPlayer).chunked(5).forEach { pieces ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pieces.forEach { piece ->
                        val selected = state.selectedPieceId == piece.id
                        val placed = piece.id in game.placements
                        Surface(
                            onClick = { onSelect(piece.id) },
                            modifier = Modifier.weight(1f).testTag("piece_${piece.id}")
                                .semantics { contentDescription = piece.kind.label() + if (placed) " はいちずみ" else " まだ" },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                PieceArt(piece.kind, Modifier.size(30.dp))
                                Text(piece.kind.label(), fontSize = 10.sp, lineHeight = 12.sp)
                                Text(if (placed) "✓" else "・", fontSize = 10.sp, lineHeight = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 42.dp)) {
                val selected = state.selectedPieceId?.let(Pieces::find)
                Text(selected?.let { "${it.kind.label()}を どこに おく？" } ?: "じぶんの フィールドに ならべよう",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (selected?.id in game.placements) {
                    TextButton(onClick = onRemove, modifier = Modifier.testTag("remove_piece")) { Text("てもちに もどす", fontSize = 11.sp) }
                }
            }
            if (state.error != null) {
                Text(when (state.error) {
                    PlacementError.PIT_AT_BRIDGE -> "はしの でぐちに おとしあなは おけないよ"
                    PlacementError.BIRD_AT_HOME -> "とりは おうちに はいれないよ"
                    else -> "じぶんの フィールドに おこう"
                }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp).testTag("placement_error"))
            }
            BoardView(game, state.selectedPieceId, onCell)
            Text("おうちは ひとつの マス。おける こまは 1こ。\nおいた こまも タップして ならべかえられるよ。",
                Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
