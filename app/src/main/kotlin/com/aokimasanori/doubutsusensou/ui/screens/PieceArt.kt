package com.aokimasanori.doubutsusensou.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.aokimasanori.doubutsusensou.game.PieceKind

fun PieceKind.label(): String = when (this) {
    PieceKind.LION -> "ライオン"
    PieceKind.TIGER -> "トラ"
    PieceKind.CHEETAH -> "チーター"
    PieceKind.FOX -> "きつね"
    PieceKind.RABBIT -> "うさぎ"
    PieceKind.BIRD -> "とり"
    PieceKind.MOLE -> "もぐら"
    PieceKind.PIT -> "おとしあな"
}

/** Small vector portraits inspired by the rounded faces in the reference sheet. */
@Composable
fun PieceArt(kind: PieceKind, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val ink = Color(0xFF543A2A)
        withTransform({ scale(size.width / 100f, size.height / 100f, Offset.Zero) }) {
            fun oval(x: Float, y: Float, w: Float, h: Float, color: Color) {
                drawOval(color, Offset(x, y), Size(w, h))
                drawOval(ink, Offset(x, y), Size(w, h), style = Stroke(3f))
            }
            fun line(x: Float, y: Float, xx: Float, yy: Float) =
                drawLine(ink, Offset(x, y), Offset(xx, yy), 3f)
            fun polygon(points: List<Offset>, color: Color) {
                val p = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(p, color)
                drawPath(p, ink, style = Stroke(3f))
            }
            val gold = Color(0xFFFFCB64)
            val cream = Color(0xFFFFF1CB)
            when (kind) {
                PieceKind.PIT -> {
                    oval(7f, 40f, 86f, 42f, Color(0xFFB67F45))
                    oval(19f, 48f, 62f, 23f, ink)
                    line(12f, 77f, 4f, 88f); line(85f, 74f, 95f, 82f)
                }
                PieceKind.BIRD -> {
                    oval(3f, 49f, 38f, 24f, cream)
                    oval(59f, 49f, 38f, 24f, cream)
                    oval(33f, 43f, 34f, 46f, cream)
                    oval(32f, 18f, 36f, 38f, cream)
                    polygon(listOf(Offset(46f, 38f), Offset(56f, 38f), Offset(51f, 48f)), gold)
                    drawCircle(ink, 2.5f, Offset(43f, 31f)); drawCircle(ink, 2.5f, Offset(58f, 31f))
                    line(44f, 87f, 40f, 98f); line(57f, 87f, 63f, 98f)
                    line(48f, 17f, 43f, 7f)
                }
                else -> {
                    val fur = when (kind) {
                        PieceKind.RABBIT -> Color(0xFFFFE9DE)
                        PieceKind.MOLE -> Color(0xFFB99572)
                        PieceKind.FOX -> Color(0xFFF4A14B)
                        else -> gold
                    }
                    if (kind == PieceKind.LION) {
                        val mane = Path().apply {
                            for (i in 0..23) {
                                val angle = i * Math.PI / 12
                                val r = if (i % 2 == 0) 46 else 37
                                val x = 50 + kotlin.math.cos(angle).toFloat() * r
                                val y = 53 + kotlin.math.sin(angle).toFloat() * r
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        drawPath(mane, Color(0xFFCB8A46)); drawPath(mane, ink, style = Stroke(3f))
                    }
                    when (kind) {
                        PieceKind.RABBIT -> { oval(28f, 3f, 15f, 46f, fur); oval(55f, 3f, 15f, 46f, fur) }
                        PieceKind.FOX -> {
                            polygon(listOf(Offset(23f, 47f), Offset(24f, 9f), Offset(45f, 35f)), fur)
                            polygon(listOf(Offset(55f, 35f), Offset(77f, 9f), Offset(77f, 47f)), fur)
                        }
                        PieceKind.MOLE -> Unit
                        else -> { oval(21f, 18f, 20f, 24f, fur); oval(60f, 18f, 20f, 24f, fur) }
                    }
                    if (kind == PieceKind.FOX)
                        polygon(listOf(Offset(19f, 37f), Offset(81f, 37f), Offset(50f, 91f)), fur)
                    else oval(20f, if (kind == PieceKind.RABBIT) 35f else 30f, 60f, 58f, fur)
                    if (kind == PieceKind.MOLE) {
                        oval(7f, 76f, 86f, 16f, Color(0xFFCDA77D))
                        oval(16f, 70f, 21f, 15f, cream); oval(64f, 70f, 21f, 15f, cream)
                    }
                    if (kind == PieceKind.TIGER) {
                        line(48f, 31f, 49f, 43f)
                        line(23f, 48f, 33f, 52f); line(22f, 64f, 32f, 63f)
                        line(77f, 48f, 67f, 52f); line(78f, 64f, 68f, 63f)
                    }
                    if (kind == PieceKind.CHEETAH) {
                        listOf(Offset(33f,40f),Offset(62f,38f),Offset(27f,62f),Offset(73f,61f),Offset(34f,75f),Offset(65f,76f))
                            .forEach { drawCircle(ink, 3f, it) }
                    }
                    drawCircle(ink, 2.8f, Offset(39f, 54f)); drawCircle(ink, 2.8f, Offset(61f, 54f))
                    oval(44f, 63f, 12f, 7f, ink)
                    if (kind != PieceKind.FOX) { line(50f,70f,46f,75f); line(50f,70f,55f,75f) }
                }
            }
        }
    }
}
