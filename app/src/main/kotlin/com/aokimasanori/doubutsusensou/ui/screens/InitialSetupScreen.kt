package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aokimasanori.doubutsusensou.R
import com.aokimasanori.doubutsusensou.game.GameState
import com.aokimasanori.doubutsusensou.game.Player
import com.aokimasanori.doubutsusensou.ui.theme.DoubutsuSensouTheme

@Composable
fun InitialSetupScreen(gameState: GameState, onBack: () -> Unit) {
    val playerNumber = when (gameState.setupPlayer) { Player.ONE -> 1; Player.TWO -> 2 }
    ScreenLayout {
        Text(stringResource(R.string.setup_title), modifier = Modifier.testTag("setup_title"),
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.setup_description), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
            Text(stringResource(R.string.player_label, playerNumber),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.setup_player_instruction, playerNumber), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PawEmblem()
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.setup_placeholder_title), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.setup_placeholder_body), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.setup_scope_note), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("back_to_title"),
            shape = RoundedCornerShape(20.dp)) {
            Text(stringResource(R.string.back_to_title), Modifier.padding(vertical = 10.dp),
                textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, locale = "ja")
@Composable
private fun SetupPreview() {
    DoubutsuSensouTheme { InitialSetupScreen(GameState(), onBack = {}) }
}
