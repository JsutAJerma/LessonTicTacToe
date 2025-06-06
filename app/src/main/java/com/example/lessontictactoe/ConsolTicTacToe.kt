package com.example.lessontictactoe

enum class GameState {
    IN_PROGRESS,
    CROSS_WIN,
    NOUGHT_WIN,
    DRAW
}

//зберігання результату перевірки стану гри
data class GameCheckResult(val state: GameState, val winLine: List<Int>? = null)


//стан клітинки на стан перемоги

private fun CellState.toWinState(): GameState {
    return when (this) {
        CellState.CROSS -> GameState.CROSS_WIN
        CellState.NOUGHT -> GameState.NOUGHT_WIN
        else -> GameState.IN_PROGRESS
    }
}


fun checkGameState(field: List<CellState>, dim: Int): GameCheckResult {
    //список усіх можливих виграшних ліній
    val winLines = mutableListOf<List<Int>>()

    for (i in 0 until dim) {
        val row = (0 until dim).map { j -> i * dim + j }
        val col = (0 until dim).map { j -> j * dim + i }
        winLines.add(row)
        winLines.add(col)
    }

    val mainDiagonal = (0 until dim).map { i -> i * dim + i }
    winLines.add(mainDiagonal)

    val antiDiagonal = (0 until dim).map { i -> i * dim + (dim - 1 - i) }
    winLines.add(antiDiagonal)

    for (line in winLines) {
        val firstCellState = field[line[0]]
        if (firstCellState != CellState.EMPTY) {
            val isWin = line.all { index -> field[index] == firstCellState }
            if (isWin) {
                return GameCheckResult(firstCellState.toWinState(), line)
            }
        }
    }

    return if (field.any { it == CellState.EMPTY }) {
        GameCheckResult(GameState.IN_PROGRESS)
    } else {
        GameCheckResult(GameState.DRAW)
    }
}

enum class Player {
    CROSS,
    NOUGHT
}

val Player.mark: CellState
    get() = when (this) {
        Player.CROSS -> CellState.CROSS
        Player.NOUGHT -> CellState.NOUGHT
    }

val CellState.symbol: String
    get() = when(this) {
        CellState.CROSS -> "X"
        CellState.NOUGHT -> "O"
        CellState.EMPTY -> ""
    }

enum class CellState {
    EMPTY,
    CROSS,
    NOUGHT
}