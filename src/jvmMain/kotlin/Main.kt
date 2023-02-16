import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application


sealed interface Route {
    class Game(val blue: String, val red: String, val size: Int, val roundCount: Int, val roundResults: MutableList<Cell>): Route
    class EndScreen(val winner: String, val type: Cell): Route
    object PreGame: Route
}

@Composable
fun teamSelect(callback: (String, String, Int, Int) -> Unit) {
    var blueName by remember { mutableStateOf("") }
    var redName by remember { mutableStateOf("") }
    var gridSize by remember { mutableStateOf("5") }
    var roundCount by remember { mutableStateOf("1") }

    Column(verticalArrangement = Arrangement.Center, modifier = androidx.compose.ui.Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Column {
                Text("blue")
                TextField(blueName, { blueName = it })
            }
            Column {
                Text("red")
                TextField(redName, {redName = it})
            }
        }
        Text("grid size")
        TextField(gridSize, {gridSize = it})
        Text("round count")
        TextField(roundCount, {roundCount = it})
        Button({
            if (blueName.isNotBlank() && redName.isNotBlank()) {
                val count = (roundCount.toIntOrNull() ?: 1)
                callback(blueName, redName, gridSize.toIntOrNull() ?: 5, if (count < 1) 1 else count)
            }
        }) {
            Text("start")
        }
    }
}

enum class Cell {
    Empty,
    Red,
    Blue
}

enum class PlayerType {
    Red,
    Blue,
}

fun Cell.getColor(): Color {
    return when (this) {
        Cell.Empty -> Color.Gray
        Cell.Red -> Color.Red
        Cell.Blue -> Color.Blue
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun game(blueName: String, redName: String, size: Int = 7, onWin: (Cell) -> Unit) {
    var board: MutableList<MutableList<Cell>> = remember { mutableListOf() }
    var isBlueTurn by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }

    fun List<Pair<Int, Int>>.checkMatrix(row: Int, col: Int): Cell {
        val res = this.map {
            row+it.first to col+it.second
        }.map {
            board.getOrNull(it.first)?.getOrNull(it.second) ?: return Cell.Empty
        }.map {
            when (it) {
                Cell.Empty -> {
                    return Cell.Empty
                }
                Cell.Red -> PlayerType.Red
                Cell.Blue -> PlayerType.Blue
            }
        }.count { it == PlayerType.Red }


        return when (res) {
            4 -> {
                Cell.Red
            }
            0 -> {
                Cell.Blue
            }
            else -> {
                Cell.Empty
            }
        }
    }

    fun checkWin(rowI: Int, colI: Int): Cell {
        val row = listOf(
            0 to -1,
            0 to 0,
            0 to 1,
            0 to 2
        )
        val row1 = listOf(
            0 to -2,
            0 to -1,
            0 to 0,
            0 to 1
        )

        val col = listOf(
            -2 to 0,
            -1 to 0,
            0 to 0,
            1 to 0
        )

        val col1 = listOf(
            -1 to 0,
            0 to 0,
            1 to 0,
            2 to 0
        )

        val diagA = listOf(
            -1 to -1,
            0 to 0,
            1 to 1,
            2 to 2
        )

        val diagA1 = listOf(
            -2 to -2,
            -1 to -1,
            0 to 0,
            1 to 1
        )

        val diagB = listOf(
            -2 to 2,
            -1 to 1,
            0 to 0,
            1 to -1
        )

        val diagB1 = listOf(
            -1 to 1,
            0 to 0,
            1 to -1,
            2 to -2
        )

        val all = listOf(row, row1, col, col1, diagA, diagA1, diagB, diagB1)

        all.forEach {
            val res = it.checkMatrix(rowI, colI)
            if (res != Cell.Empty)  {
                return res
            }
        }
        return Cell.Empty
    }

    fun checkWinner(): Cell {
        board.forEachIndexed { row, it ->
            it.forEachIndexed { col, cell ->
                val res = checkWin(row, col)
                if (res != Cell.Empty) {
                    return res
                }
            }
        }
        return Cell.Empty
    }

    fun isFull(): Boolean {
        return board.map {
            it.count {
                it != Cell.Empty
            }
        }.sum() == size*size
    }

    if (!isInitialized) {
        repeat(size) { row ->
            board.add(mutableListOf())
            repeat(size) { col ->
                board[row].add(Cell.Empty)
            }
        }
        isInitialized = true
    }

    println(board.size)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(blueName, color = Color.Blue)
            Spacer(Modifier.padding(6.dp))
            Text(redName, color = Color.Red)
        }
        Spacer(Modifier.padding(6.dp))
        if (isBlueTurn) {
            Text("its blue turn", color = Color.Blue)
        }
        else {
            Text("its red turn", color = Color.Red)
        }
        Spacer(Modifier.padding(6.dp))
        Column {
            board.forEachIndexed { row, it ->
                Row {
                    it.forEachIndexed { col, it ->
                        Box(modifier = Modifier.size(50.dp).background(it.getColor()).onClick {
                            println("click $row $col ${board[row][col]} ${board.size}")
                            if (board[row][col] == Cell.Empty) {
                                board[row][col] = if (isBlueTurn) Cell.Blue else Cell.Red
                                isBlueTurn = !isBlueTurn
                                when (checkWinner()) {
                                    Cell.Empty -> {}
                                    Cell.Red -> {
                                        board.clear()
                                        isInitialized = false
                                        onWin(Cell.Red)
                                    }
                                    Cell.Blue -> {
                                        board.clear()
                                        isInitialized = false
                                        onWin(Cell.Blue)
                                    }
                                }
                                if (isFull()) {
                                    board.clear()
                                    isInitialized = false
                                    onWin(Cell.Empty)
                                }
                            }
                        })
                        Spacer(Modifier.padding(4.dp))
                    }
                }
                Spacer(Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }
    var currentRoute: Route by remember { mutableStateOf(Route.PreGame) }

    MaterialTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            when (currentRoute) {
                is Route.EndScreen -> {
                    Column {
                        val r = (currentRoute as Route.EndScreen)
                        if (r.type == Cell.Empty) {
                            Text("its a TIE")
                        }
                        else {
                            Text("${r.type} : ${r.winner} WON!!")
                        }
                        Button({
                            currentRoute = Route.PreGame
                        }) {
                            Text("next game")
                        }
                    }
                }
                is Route.Game -> {
                    val route = (currentRoute as Route.Game)
                    game(route.blue, route.red, route.size) {
                        println("aaa $it")
                        when (it) {
                            Cell.Red -> {
                                route.roundResults.add(Cell.Red)
                            }

                            Cell.Blue -> {
                                route.roundResults.add(Cell.Blue)
                            }

                            Cell.Empty -> {
                                route.roundResults.add(Cell.Empty)
                            }
                        }
                        println("${route.roundCount} ${route.roundResults.size}")
                        if (route.roundCount <= route.roundResults.size) {
                            val b = route.roundResults.count { it == Cell.Blue }
                            val r = route.roundResults.count { it == Cell.Red }

                            val c = if (b == r) {
                                Cell.Empty
                            }
                            else if (b > r) {
                                Cell.Blue
                            }
                            else {
                                Cell.Red
                            }

                            currentRoute = Route.EndScreen(if (r > b) route.red else route.blue , c)
                        }
                    }
                }
                Route.PreGame -> {
                    teamSelect { blue, red, size, round ->
                        currentRoute = Route.Game(blue, red, size, round, mutableListOf())
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
