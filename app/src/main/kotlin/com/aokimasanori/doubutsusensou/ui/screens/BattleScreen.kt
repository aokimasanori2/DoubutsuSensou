package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aokimasanori.doubutsusensou.game.*
import com.aokimasanori.doubutsusensou.ui.AppUiState

@Composable
fun BattleScreen(
    state: AppUiState, onCell: (Cell) -> Unit, onContinue: () -> Unit,
    onEndTurn: () -> Unit, onPass: () -> Unit, onRestart: () -> Unit, onBack: () -> Unit,
) {
    val game = requireNotNull(state.game)
    if (game.phase == GamePhase.FINISHED) {
        val outcome = requireNotNull(game.outcome)
        ScreenLayout {
            PawEmblem()
            Spacer(Modifier.height(24.dp))
            Text(outcome.winner?.let { "${it.teamName()}の かち！" } ?: "ひきわけ！",
                Modifier.testTag("match_result"), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(when (outcome.reason) {
                WinReason.HOME -> "どうぶつが おうちに かえったよ！"
                WinReason.NO_ANIMALS -> "あいての うごける どうぶつが\nいなくなったよ。"
                WinReason.DRAW -> "ふたりとも よく がんばったね！"
            }, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().testTag("play_again")) {
                Text("もういちど あそぶ", Modifier.padding(10.dp))
            }
            TextButton(onClick = onBack, modifier = Modifier.testTag("back_to_title")) { Text("タイトルへ もどる") }
        }
        return
    }
    if (state.privacyCovered || game.phase in setOf(GamePhase.TURN_HANDOFF, GamePhase.READY)) {
        ScreenLayout {
            PawEmblem()
            Spacer(Modifier.height(24.dp))
            Text("${game.activePlayer.teamName()}に\nスマホを わたしてね",
                Modifier.testTag("turn_handoff"), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("あいては がめんを みないでね。", textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("accept_handoff")) {
                Text("じゅんびOK！", Modifier.padding(10.dp))
            }
            TextButton(onClick = onBack) { Text("タイトルへ もどる") }
        }
        return
    }
    var showRules by remember { mutableStateOf(false) }
    val result = game.phase == GamePhase.TURN_RESULT
    val noMoves = !result && !Movement.hasMove(game, game.activePlayer)
    Scaffold(bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("back_to_title")) { Text("もどる") }
                when {
                    result -> Button(onClick = onEndTurn, modifier = Modifier.weight(1f).testTag("end_turn")) {
                        Text("あいてに わたす", Modifier.padding(vertical = 6.dp))
                    }
                    noMoves -> Button(onClick = onPass, modifier = Modifier.weight(1f).testTag("pass_turn")) {
                        Text("このばんは おやすみ")
                    }
                    else -> Text("うごかす こまを 1つ えらぼう", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${game.activePlayer.teamName()}の ばん", Modifier.testTag("turn_title"),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${game.turnNumber}てめ", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { showRules = true }) { Text("あそびかた") }
            Text(when {
                result -> "このての けっかです。\nあいての こまは ひみつだよ。"
                noMoves -> "うごける マスが ないよ。\nこのばんは おやすみしよう。"
                state.selectedPieceId != null -> "みどりの わくの マスに うごけるよ。"
                else -> "じぶんの こま → いきたい マスを タップ"
            }, textAlign = TextAlign.Center, modifier = Modifier.testTag(if (result) "turn_result" else "move_instruction"),
                style = MaterialTheme.typography.bodyMedium)
            if (state.invalidMove) Text("そのマスには うごけないよ。", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("move_error"))
            Spacer(Modifier.height(12.dp))
            BoardView(game, state.selectedPieceId, onCell)
            Spacer(Modifier.height(12.dp))
            Text("とり いがいの どうぶつで、あいてがわの おうちへ！",
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showRules) AlertDialog(
        onDismissRequest = { showRules = false },
        title = { Text("あそびかた") },
        text = {
            Text(
                "1ばんに 1この こまを うごかします。\n\n" +
                "ふつうの どうぶつは たて・よこに 1マス。チーターは まっすぐ なんマスでも。みかたや かわは とびこせません。\n\n" +
                "とりは まっすぐ なんマスでも とべます。みかた・あいて・かわを とびこせますが、かわに とまることと、おうちに はいることは できません。\n\n" +
                "おとしあなは うごきません。\n\n" +
                "つよさは ライオン → トラ → チーター → とり → きつね → うさぎ → もぐら。うさぎは ライオンに かちます。\n\n" +
                "おなじ どうぶつどうしは りょうほう きえます。とり・もぐらは おとしあなに かち、それいがいは おとしあなと いっしょに きえます。\n\n" +
                "おうちに かえるか、あいての うごける どうぶつが いなくなると かち！ あいての しょうたいは さいごまで ひみつです。\n\n" +
                "いきさきが ぜんぶ ふさがった ばんは おやすみ。ふたりとも うごけない ときや、さいごの どうぶつが りょうほう きえた ときは ひきわけです。",
                Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = { showRules = false }) { Text("わかった！") } },
    )
}
