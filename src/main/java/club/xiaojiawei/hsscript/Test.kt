package club.xiaojiawei.hsscript

/**
 * @author 肖嘉威
 * @date 2025/7/26 1:08
 */

import java.util.*

data class Block(val x: Int, val y: Int, val width: Int, val height: Int, val id: Int) {
    fun occupiesPosition(px: Int, py: Int): Boolean {
        return px in x until (x + width) && py in y until (y + height)
    }

    fun getAllPositions(): List<Pair<Int, Int>> {
        return (0 until width).flatMap { dx ->
            (0 until height).map { dy -> Pair(x + dx, y + dy) }
        }
    }
}

data class GameState(
    val blocks: List<Block>,
    val playerPos: Pair<Int, Int>,
    val moves: List<String> = emptyList()
) {
    fun toKey(): String {
        val sortedBlocks = blocks.sortedBy { it.id }
        val blockKey = sortedBlocks.joinToString("|") { "${it.id}:${it.x},${it.y}" }
        return "$blockKey|P:${playerPos.first},${playerPos.second}"
    }
}

class HuaRongDaoSolver {
    private val mapWidth = 4
    private val mapHeight = 5
    private val targetPosition = Pair(1, 0)

    private fun createInitialState(): GameState {
        val initialBlocks = listOf(
            Block(0, 0, 2, 1, 1),
            Block(0, 1, 1, 2, 2),
            Block(0, 3, 2, 1, 3),
            Block(2, 1, 1, 2, 4),
            Block(3, 4, 1, 1, 5)
        )
        val playerStart = Pair(3, 2) // 玩家起始位置
        return GameState(initialBlocks, playerStart)
    }

    private fun printMap(state: GameState, title: String = "") {
        val map = Array(mapHeight) { Array(mapWidth) { '.' } }

        for (block in state.blocks) {
            for ((bx, by) in block.getAllPositions()) {
                map[by][bx] = block.id.toString().first()
            }
        }

        val (px, py) = state.playerPos
        if (map[py][px] == '.') {
            map[py][px] = 'P'
        }

        if (map[targetPosition.second][targetPosition.first] == '.') {
            map[targetPosition.second][targetPosition.first] = 'T'
        }

        if (title.isNotEmpty()) println(title)
        println("地图状态 (T=目标, P=玩家):")
        for (y in 0 until mapHeight) {
            print("$y: ")
            for (x in 0 until mapWidth) {
                print("${map[y][x]} ")
            }
            println()
        }
        print("   ")
        for (x in 0 until mapWidth) print("$x ")
        println("\n")
    }

    private fun isPositionOccupied(x: Int, y: Int, blocks: List<Block>, excludeId: Int = -1): Boolean {
        if (x !in 0 until mapWidth || y !in 0 until mapHeight) return true
        return blocks.any { it.id != excludeId && it.occupiesPosition(x, y) }
    }

    private fun canMoveBlock(block: Block, dir: String, blocks: List<Block>): Boolean {
        val (dx, dy) = when (dir) {
            "上" -> 0 to -1
            "下" -> 0 to 1
            "左" -> -1 to 0
            "右" -> 1 to 0
            else -> return false
        }

        for ((bx, by) in block.getAllPositions()) {
            val nx = bx + dx
            val ny = by + dy
            if (isPositionOccupied(nx, ny, blocks, block.id)) return false
        }
        return true
    }

    private fun playerCanPushBlock(block: Block, dir: String, player: Pair<Int, Int>): Boolean {
        val (dx, dy) = when (dir) {
            "上" -> 0 to 1
            "下" -> 0 to -1
            "左" -> 1 to 0
            "右" -> -1 to 0
            else -> return false
        }

        val behindPositions = when (dir) {
            "上", "下" -> (0 until block.width).map { i -> Pair(block.x + i, block.y + if (dy == 1) +block.height else -1) }
            "左", "右" -> (0 until block.height).map { i -> Pair(block.x + if (dx == 1) +block.width else -1, block.y + i) }
            else -> emptyList()
        }

        return player in behindPositions
    }

    private fun moveBlock(block: Block, dir: String): Block {
        val (dx, dy) = when (dir) {
            "上" -> 0 to -1
            "下" -> 0 to 1
            "左" -> -1 to 0
            "右" -> 1 to 0
            else -> return block
        }
        return block.copy(x = block.x + dx, y = block.y + dy)
    }

    private fun getNextStates(state: GameState): List<GameState> {
        val nextStates = mutableListOf<GameState>()
        val directions = listOf("上", "下", "左", "右")

        // 玩家推动方块
        for (block in state.blocks) {
            for (dir in directions) {
                if (canMoveBlock(block, dir, state.blocks) &&
                    playerCanPushBlock(block, dir, state.playerPos)) {

                    val movedBlock = moveBlock(block, dir)
                    val newBlocks = state.blocks.map { if (it.id == block.id) movedBlock else it }

                    val (dx, dy) = when (dir) {
                        "上" -> 0 to -1
                        "下" -> 0 to 1
                        "左" -> -1 to 0
                        "右" -> 1 to 0
                        else -> 0 to 0
                    }

                    val newPlayerPos = Pair(state.playerPos.first + dx, state.playerPos.second + dy)

                    val desc = "玩家推动方块${block.id}从(${block.x},${block.y})向${dir}到(${movedBlock.x},${movedBlock.y})"
                    nextStates.add(GameState(newBlocks, newPlayerPos, state.moves + desc))
                }
            }
        }

        // 玩家自己移动
        for ((dx, dy) in listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)) {
            val nx = state.playerPos.first + dx
            val ny = state.playerPos.second + dy
            if (nx in 0 until mapWidth && ny in 0 until mapHeight &&
                !isPositionOccupied(nx, ny, state.blocks)) {
                val dir = when {
                    dx == -1 -> "左"
                    dx == 1 -> "右"
                    dy == -1 -> "上"
                    dy == 1 -> "下"
                    else -> ""
                }
                val desc = "玩家从(${state.playerPos.first},${state.playerPos.second})向${dir}走到($nx,$ny)"
                nextStates.add(GameState(state.blocks, Pair(nx, ny), state.moves + desc))
            }
        }

        return nextStates
    }

    private fun isGoalAchieved(state: GameState): Boolean {
        return state.playerPos == targetPosition
    }

    fun solve(): List<String>? {
        val initialState = createInitialState()
        printMap(initialState, "初始状态")

        if (isGoalAchieved(initialState)) {
            println("目标已达成")
            return emptyList()
        }

        val queue = LinkedList<GameState>()
        val visited = mutableSetOf<String>()

        queue.offer(initialState)
        visited.add(initialState.toKey())

        var steps = 0
        val maxSteps = 200_000

        while (queue.isNotEmpty() && steps < maxSteps) {
            val current = queue.poll()
            steps++

            if (steps % 10_000 == 0) {
                println("已搜索 $steps 状态，队列中 ${queue.size} 个...")
            }

            for (next in getNextStates(current)) {
                val key = next.toKey()
                if (key !in visited) {
                    visited.add(key)
                    if (isGoalAchieved(next)) {
                        println("🎉 找到解答！总步数：${next.moves.size}")
                        printMap(next, "最终状态")
                        next.moves.forEachIndexed { i, step -> println("${i + 1}. $step") }
                        return next.moves
                    }
                    queue.offer(next)
                }
            }
        }

        println("❌ 未能在步数限制内找到解答")
        return null
    }
}

fun main() {
    println("华容道解密器：玩家需走到目标位置")
    HuaRongDaoSolver().solve()
}
