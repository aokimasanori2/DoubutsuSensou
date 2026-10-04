package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aokimasanori.doubutsusensou.game.*

@Composable
fun BoardView(game: GameState, selected: Int?, onCell: (Cell) -> Unit) {
    val engine = GameEngine()
    Column(Modifier.fillMaxWidth().testTag("board")) {
        repeat(Board.ROWS) { row ->
            Row(Modifier.fillMaxWidth().height(51.dp)) {
                Board.cells.filter { it.row == row }.forEach { cell ->
                    val terrain = Board.terrain(cell)
                    val piece = game.pieceAt(cell)
                    val own = piece?.owner == game.setupPlayer
                    val selectedPiece = selected?.let(Pieces::find)
                    val allowed = selectedPiece != null && engine.placementError(selectedPiece, cell) == null
                    val selectedHere = selected != null && selected == piece?.id
                    val label = if (piece == null) "" else if (own) piece.kind.label() else "？"
                    val home = terrain == Terrain.HOME
                    val cellDescription = "${row + 1}だん ${cell.column + 1}れつ" +
                        (if (home) " おうち" else "") + (if (label.isEmpty()) "" else " $label")
                    val background = when (terrain) {
                        Terrain.GRASS -> Color(0xFFDFECC5)
                        Terrain.SOIL -> Color(0xFFF1D4AD)
                        Terrain.RIVER -> Color(0xFFBAE3EC)
                        Terrain.BRIDGE -> Color(0xFFC6A374)
                        Terrain.HOME -> Color(0xFFFFEDC9)
                    }
                    Column(
                        Modifier.weight(Board.span(cell).toFloat()).fillMaxHeight()
                            .background(background)
                            .border(if (selectedHere) 3.dp else 1.dp,
                                if (selectedHere) Color(0xFF314D27) else Color(0xFF9FAD8B))
                            .clickable(enabled = Board.territory(cell) == game.setupPlayer) { onCell(cell) }
                            .testTag("cell_${row}_${cell.column}")
                            .semantics(mergeDescendants = true) { contentDescription = cellDescription },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (home) {
                            Box(Modifier.fillMaxWidth().height(3.dp).background(
                                if (row == 0) Color(0xFFC85950) else Color(0xFF5581B7)))
                        }
                        when {
                            piece != null && own -> {
                                PieceArt(piece.kind, Modifier.size(30.dp))
                                Text(label, color = Color(0xFF41382A), fontSize = 9.sp, lineHeight = 10.sp)
                            }
                            piece != null -> Text("？", color = Color(0xFF53432E), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            terrain == Terrain.RIVER -> Text("〜", color = Color(0xFF397F99))
                            terrain == Terrain.BRIDGE -> Text("はし", color = Color(0xFF5E4228), fontSize = 11.sp)
                            home -> Text("おうち", color = Color(0xFF6F5236), fontSize = 12.sp)
                            allowed -> Text("・", color = Color(0xFF567B3B), fontSize = 24.sp)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
