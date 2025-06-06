package com.example.lessontictactoe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lessontictactoe.ui.theme.LessonTicTacToeTheme
import kotlinx.coroutines.delay

//константы
private const val TIMER_DURATION_SECONDS = 10
private val CELL_SIZE = 80.dp
private const val WINNING_FONT_SIZE = 60f
private const val DEFAULT_FONT_SIZE = 40f
private const val ANIMATION_DURATION_MS = 500

fun Player.next(): Player = if (this == Player.CROSS) Player.NOUGHT else Player.CROSS

enum class AppScreen {
    MAIN_MENU,
    GAME_BOARD,
    LEADERBOARD
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedDim by remember { mutableIntStateOf(0) }
    var gameKey by remember { mutableIntStateOf(0) }
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

    //рахунок перемог, який зберігається між іграми
    var totalScoreX by remember { mutableIntStateOf(0) }
    var totalScoreO by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Хрестики-нулики",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )

        when (currentScreen) {
            AppScreen.MAIN_MENU -> {
                DimensionSelector { newDim ->
                    selectedDim = newDim
                    currentScreen = AppScreen.GAME_BOARD
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { currentScreen = AppScreen.LEADERBOARD }) {
                    Text("Таблиця рекордів")
                }
            }
            AppScreen.GAME_BOARD -> {
                GameBoard(
                    key = gameKey,
                    dim = selectedDim,
                    onNewGame = {
                        gameKey++
                        currentScreen = AppScreen.MAIN_MENU
                    },
                    onScoreUpdate = { winner ->
                        if (winner == Player.CROSS) totalScoreX++ else totalScoreO++
                    }
                )
            }
            AppScreen.LEADERBOARD -> {
                LeaderboardScreen(
                    scoreX = totalScoreX,
                    scoreO = totalScoreO,
                    onBackToMainMenu = { currentScreen = AppScreen.MAIN_MENU }
                )
            }
        }
    }
}

@Composable
fun DimensionSelector(onDimSelected: (Int) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Виберіть розмір поля:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onDimSelected(3) }) { Text("3x3") }
            Button(onClick = { onDimSelected(4) }) { Text("4x4") }
            Button(onClick = { onDimSelected(5) }) { Text("5x5") }
        }
    }
}

@Composable
fun GameBoard(key: Any, dim: Int, onNewGame: () -> Unit, onScoreUpdate: (Player) -> Unit) {
    val field = remember(key) { mutableStateListOf(*Array(dim * dim) { CellState.EMPTY }) }
    var currentPlayer by remember(key) { mutableStateOf(Player.CROSS) }
    var gameState by remember(key) { mutableStateOf(GameState.IN_PROGRESS) }
    var winLine by remember(key) { mutableStateOf<List<Int>?>(null) }

    var roundScoreX by remember(key) { mutableIntStateOf(0) }
    var roundScoreO by remember(key) { mutableIntStateOf(0) }

    var currentTimer by remember { mutableIntStateOf(TIMER_DURATION_SECONDS) }

    LaunchedEffect(key, currentPlayer, gameState) {
        if (gameState == GameState.IN_PROGRESS) {
            currentTimer = TIMER_DURATION_SECONDS
            while (currentTimer > 0) {
                delay(1000L)
                currentTimer--
            }
            if (gameState == GameState.IN_PROGRESS) {
                currentPlayer = currentPlayer.next()
            }
        }
    }

    fun handleMove(index: Int) {
        if (field[index] == CellState.EMPTY && gameState == GameState.IN_PROGRESS) {
            field[index] = currentPlayer.mark
            val checkResult = checkGameState(field, dim)
            gameState = checkResult.state
            winLine = checkResult.winLine

            when (checkResult.state) {
                GameState.CROSS_WIN -> {
                    roundScoreX++
                    onScoreUpdate(Player.CROSS)
                }
                GameState.NOUGHT_WIN -> {
                    roundScoreO++
                    onScoreUpdate(Player.NOUGHT)
                }
                GameState.IN_PROGRESS -> {
                    currentPlayer = currentPlayer.next()
                    currentTimer = TIMER_DURATION_SECONDS
                }
                GameState.DRAW -> {}
            }
        }
    }

    fun resetRound() {
        field.fill(CellState.EMPTY)
        currentPlayer = Player.CROSS
        gameState = GameState.IN_PROGRESS
        winLine = null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ScoreDisplay(roundScoreX, roundScoreO)
        GameStatus(gameState, currentPlayer, currentTimer)

        for (row in 0 until dim) {
            Row {
                for (col in 0 until dim) {
                    val index = row * dim + col
                    Cell(
                        state = field[index],
                        enabled = gameState == GameState.IN_PROGRESS,
                        onClick = { handleMove(index) },
                        isWinningCell = winLine?.contains(index) == true
                    )
                }
            }
        }
        ControlButtons(onResetRound = ::resetRound, onNewGame = onNewGame)
    }
}

@Composable
fun Cell(state: CellState, enabled: Boolean, onClick: () -> Unit, isWinningCell: Boolean) {
    val animatedFontSize by animateFloatAsState(
        targetValue = if (isWinningCell) WINNING_FONT_SIZE else DEFAULT_FONT_SIZE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
    )
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isWinningCell) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
    )

    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .padding(4.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary)
            .background(animatedBackgroundColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.symbol,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = animatedFontSize.sp)
        )
    }
}

@Composable
fun ScoreDisplay(scoreX: Int, scoreO: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text(text = "Рахунок X: $scoreX", style = MaterialTheme.typography.titleMedium)
        Text(text = "Рахунок O: $scoreO", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun GameStatus(gameState: GameState, currentPlayer: Player, timer: Int) {
    val text = when (gameState) {
        GameState.IN_PROGRESS -> "Хід гравця: ${currentPlayer.mark.symbol}"
        GameState.CROSS_WIN -> "Переміг X! 🎉"
        GameState.NOUGHT_WIN -> "Переміг O! 🎉"
        GameState.DRAW -> "Нічия! 🤝"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    if (gameState == GameState.IN_PROGRESS) {
        Text(
            text = "Час на хід: $timer с ⏳",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun ControlButtons(onResetRound: () -> Unit, onNewGame: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Button(onClick = onResetRound) { Text("Новий раунд") }
        Button(onClick = onNewGame) { Text("Вийти в меню") }
    }
}

@Composable
fun LeaderboardScreen(scoreX: Int, scoreO: Int, onBackToMainMenu: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Таблиця Рекордів 🏆",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Text(
            text = "Загальний рахунок за сесію:",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Гравець X:", style = MaterialTheme.typography.bodyLarge)
            Text("$scoreX", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Гравець O:", style = MaterialTheme.typography.bodyLarge)
            Text("$scoreO", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackToMainMenu) {
            Text("Повернутися до меню")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LessonTicTacToeTheme {
        MainScreen()
    }
}