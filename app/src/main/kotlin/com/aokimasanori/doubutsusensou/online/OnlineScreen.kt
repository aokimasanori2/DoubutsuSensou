package com.aokimasanori.doubutsusensou.online

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aokimasanori.doubutsusensou.game.*
import com.aokimasanori.doubutsusensou.ui.AppUiState
import com.aokimasanori.doubutsusensou.ui.screens.*

@Composable
fun OnlineRoute(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) { viewModelFactory {
        initializer { OnlineViewModel(FirebaseRoomRepository(context), createSavedStateHandle()) }
    } }
    val model: OnlineViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    DisposableEffect(model) { model.open(); onDispose { model.stop() } }
    BackHandler(onBack = onBack)
    OnlineScreen(state, model::createRoom, model::joinRoom, model::resumeRoom,
        model::select, model::tap, model::remove, model::confirm, model::pass,
        model::leave, model::forgetRoom, onBack)
}

@Composable
fun OnlineScreen(
    state: OnlineUiState, onCreate: () -> Unit, onJoin: (String) -> Unit,
    onResume: () -> Unit, onSelect: (Int) -> Unit, onCell: (Cell) -> Unit,
    onRemove: () -> Unit, onConfirm: () -> Unit, onPass: () -> Unit,
    onLeave: () -> Unit, onForget: () -> Unit, onBack: () -> Unit,
) {
    var leaveDialog by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val room = state.room
    val player = state.player
    if (room == null || player == null) {
        var code by rememberSaveable { mutableStateOf("") }
        ScreenLayout {
            PawEmblem()
            Spacer(Modifier.height(16.dp))
            Text("2だいで つうしんたいせん", Modifier.testTag("online_lobby"),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("ひとりが へやを つくり、もうひとりが\n8けたの ばんごうで はいります。",
                Modifier.padding(vertical = 16.dp), textAlign = TextAlign.Center)
            Button(onClick = onCreate, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().testTag("create_room")) { Text("へやを つくる", Modifier.padding(8.dp)) }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, label = { Text("へやばんごう") },
                enabled = !state.busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("room_code_input"))
            Button(onClick = { onJoin(code) }, enabled = code.length == 8 && !state.busy,
                modifier = Modifier.fillMaxWidth().testTag("join_room")) { Text("へやに はいる", Modifier.padding(8.dp)) }
            if (state.savedCode != null) OutlinedButton(onClick = onResume, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().testTag("resume_room")) { Text("まえの たいせんに もどる") }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("online_error")) }
            TextButton(onClick = onBack) { Text("タイトルへ もどる") }
        }
        return
    }

    val expired = System.currentTimeMillis() >= room.expiresAt
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("タイトル") }
            SelectionContainer { Text(state.code!!.chunked(4).joinToString(" "), Modifier.testTag("room_code"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            TextButton(onClick = { leaveDialog = true }, enabled = !state.busy && state.connected &&
                room.phase !in setOf(RoomPhase.FINISHED, RoomPhase.CLOSED) && !expired) { Text("おわる") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!state.connected && !expired) Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("つうしんを かくにんしています…", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onResume, enabled = !state.busy, modifier = Modifier.testTag("reconnect")) { Text("つなぎなおす") }
        }
        state.error?.let { Text(it, Modifier.padding(horizontal = 12.dp).testTag("online_error"),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        when {
            expired || room.phase == RoomPhase.CLOSED -> ScreenLayout {
                Text(if (expired) "へやの きげんが きれました。" else "このへやは おわりました。",
                    Modifier.testTag("room_closed"), style = MaterialTheme.typography.headlineSmall)
                Button(onClick = onForget, modifier = Modifier.testTag("new_online_room")) { Text("あたらしく あそぶ") }
            }
            room.phase == RoomPhase.WAITING -> ScreenLayout {
                Text("あいてを まっています", Modifier.testTag("waiting_guest"), style = MaterialTheme.typography.headlineSmall)
                Text("うえの 8けたの ばんごうを\nいっしょに あそぶ ひとに つたえてね。",
                    Modifier.padding(16.dp), textAlign = TextAlign.Center)
                Text("へやは 24じかんで おわります。", style = MaterialTheme.typography.bodySmall)
            }
            room.phase == RoomPhase.SETUP && !room.ready(player) -> InitialSetupScreen(
                AppUiState(game = state.draft, selectedPieceId = state.selected, error = state.placementError),
                onSelect, onCell, onRemove, onConfirm, {}, onBack,
                confirmEnabled = state.connected && !state.busy,
            )
            room.phase == RoomPhase.SETUP -> ScreenLayout {
                Text("じゅんびOK！", style = MaterialTheme.typography.headlineMedium)
                Text("あいての じゅんびを まっています。", Modifier.testTag("waiting_ready").padding(16.dp))
            }
            room.phase == RoomPhase.FINISHED -> ScreenLayout {
                val outcome = room.game.outcome!!
                Text(outcome.winner?.let { if (it == player) "あなたの かち！" else "あいての かち！" } ?: "ひきわけ！",
                    Modifier.testTag("online_result"), style = MaterialTheme.typography.headlineLarge)
                Text(when (outcome.reason) {
                    WinReason.HOME -> "どうぶつが おうちに かえったよ！"
                    WinReason.NO_ANIMALS -> "うごける どうぶつが いなくなりました。"
                    WinReason.RESIGNED -> "ひとりが たいせんを おわりました。"
                    WinReason.DRAW -> "ふたりとも よく がんばったね！"
                }, Modifier.padding(16.dp), textAlign = TextAlign.Center)
                Button(onClick = onForget, modifier = Modifier.testTag("new_online_room")) { Text("もういちど あそぶ") }
                Text("あたらしい へやばんごうで はじめます。", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
            else -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                val myTurn = room.game.activePlayer == player
                Text("あなたは ${player.teamName()}", style = MaterialTheme.typography.titleMedium)
                Text(if (myTurn) "あなたの ばん" else "あいての ばん",
                    Modifier.testTag("online_turn"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${room.game.turnNumber}てめ", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { showRules = true }) { Text("あそびかた") }
                if (myTurn) Text(if (state.selected == null) "じぶんの こま → いきたい マスを タップ" else "みどりの わくに うごけるよ。",
                    style = MaterialTheme.typography.bodySmall)
                else Text("あいてが うごかすと、がめんが かわります。", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                BoardView(room.game, state.selected, onCell, viewer = player,
                    interactionEnabled = myTurn && state.connected && !state.busy)
                if (myTurn && !Movement.hasMove(room.game, player)) Button(onClick = onPass,
                    enabled = state.connected && !state.busy, modifier = Modifier.testTag("online_pass")) { Text("このばんは おやすみ") }
                Spacer(Modifier.height(20.dp).navigationBarsPadding())
            }
        }
    }
    if (showRules) GameRulesDialog { showRules = false }
    if (leaveDialog) AlertDialog(
        onDismissRequest = { leaveDialog = false },
        title = { Text("へやを おわる？") },
        text = { Text("たいせんちゅうは、へやを おわると まけに なります。\nあとで もどる ときは「タイトル」を つかってね。") },
        confirmButton = { TextButton(onClick = { leaveDialog = false; onLeave() }) { Text("おわる") } },
        dismissButton = { TextButton(onClick = { leaveDialog = false }) { Text("つづける") } },
    )
}
