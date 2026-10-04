package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aokimasanori.doubutsusensou.R
import com.aokimasanori.doubutsusensou.ui.theme.DoubutsuSensouTheme

@Composable
fun TitleScreen(onPlay: () -> Unit) {
    ScreenLayout {
        Spacer(Modifier.height(20.dp))
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
            Text(stringResource(R.string.offline_detail), Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(32.dp))
        PawEmblem()
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.title_line_one), style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.title_line_two), style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.title_description), style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().testTag("play_button"),
            shape = RoundedCornerShape(20.dp)) {
            Text(stringResource(R.string.play_two_players), Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.offline_label), style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, locale = "ja")
@Composable
private fun TitlePreview() { DoubutsuSensouTheme { TitleScreen(onPlay = {}) } }
