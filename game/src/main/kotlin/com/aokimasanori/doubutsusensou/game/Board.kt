package com.aokimasanori.doubutsusensou.game

data class Cell(val row: Int, val column: Int)

enum class Terrain { GRASS, SOIL, RIVER, BRIDGE, HOME }

/** Coordinates use the reference drawing: grass above the river, soil below it. */
object Board {
    const val ROWS = 9
    const val COLUMNS = 6
    val bridgeColumns = setOf(1, 4)

    fun contains(cell: Cell) = cell.row in 0 until ROWS && cell.column in 0 until COLUMNS
    fun canonical(cell: Cell): Cell =
        if (cell.row in listOf(0, 8) && cell.column == 3) cell.copy(column = 2) else cell
    fun isHome(cell: Cell) = canonical(cell).let { it.row in listOf(0, 8) && it.column == 2 }
    fun span(cell: Cell) = if (isHome(cell)) 2 else 1
    val cells: List<Cell> = (0 until ROWS).flatMap { row ->
        (0 until COLUMNS).map { Cell(row, it) }.filter { canonical(it) == it }
    }
    fun territory(cell: Cell): Player? = when (cell.row) {
        in 0..3 -> Player.ONE
        in 5..8 -> Player.TWO
        else -> null
    }
    fun terrain(cell: Cell): Terrain = when {
        isHome(cell) -> Terrain.HOME
        cell.row == 4 && cell.column in bridgeColumns -> Terrain.BRIDGE
        cell.row == 4 -> Terrain.RIVER
        cell.row < 4 -> Terrain.GRASS
        else -> Terrain.SOIL
    }
    fun isBridgeExit(cell: Cell) = cell.row in listOf(3, 5) && cell.column in bridgeColumns
}
