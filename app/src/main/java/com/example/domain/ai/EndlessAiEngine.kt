package com.example.domain.ai

import com.example.domain.model.AiDifficulty
import com.example.domain.model.BoardPosition
import com.example.domain.model.PlayerSymbol
import kotlin.random.Random

object EndlessAiEngine {

    private val WINNING_LINES = listOf(
        // Rows
        listOf(BoardPosition(0, 0), BoardPosition(0, 1), BoardPosition(0, 2)),
        listOf(BoardPosition(1, 0), BoardPosition(1, 1), BoardPosition(1, 2)),
        listOf(BoardPosition(2, 0), BoardPosition(2, 1), BoardPosition(2, 2)),
        // Columns
        listOf(BoardPosition(0, 0), BoardPosition(1, 0), BoardPosition(2, 0)),
        listOf(BoardPosition(0, 1), BoardPosition(1, 1), BoardPosition(2, 1)),
        listOf(BoardPosition(0, 2), BoardPosition(1, 2), BoardPosition(2, 2)),
        // Diagonals
        listOf(BoardPosition(0, 0), BoardPosition(1, 1), BoardPosition(2, 2)),
        listOf(BoardPosition(0, 2), BoardPosition(1, 1), BoardPosition(2, 0))
    )

    fun checkWin(pieces: List<BoardPosition>): List<BoardPosition>? {
        if (pieces.size < 3) return null
        val set = pieces.toSet()
        for (line in WINNING_LINES) {
            if (line.all { it in set }) {
                return line
            }
        }
        return null
    }

    /**
     * Compute simulated pieces after a player places at [targetPos].
     * In Endless TTT, if the player already has 3 pieces, their oldest (first) piece is removed.
     */
    fun simulateMove(
        currentPieces: List<BoardPosition>,
        targetPos: BoardPosition
    ): List<BoardPosition> {
        val updated = currentPieces.toMutableList()
        if (updated.size >= 3) {
            updated.removeAt(0) // Remove oldest piece
        }
        updated.add(targetPos)
        return updated
    }

    /**
     * Calculate legal moves for the current player.
     * Legal moves are all board positions not currently occupied by either player,
     * OR if the current player has 3 pieces, their oldest piece will vanish, so that spot also becomes legal.
     */
    fun getLegalMoves(
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>
    ): List<BoardPosition> {
        val occupiedByHuman = humanPieces.toSet()
        val occupiedByAi = if (aiPieces.size >= 3) {
            // Oldest piece will vanish, so its cell will become free
            aiPieces.drop(1).toSet()
        } else {
            aiPieces.toSet()
        }

        val legal = mutableListOf<BoardPosition>()
        for (r in 0..2) {
            for (c in 0..2) {
                val pos = BoardPosition(r, c)
                if (pos !in occupiedByHuman && pos !in occupiedByAi) {
                    legal.add(pos)
                }
            }
        }
        return legal
    }

    /**
     * Determine best move based on AI difficulty.
     */
    fun getBestMove(
        difficulty: AiDifficulty,
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>
    ): BoardPosition {
        val legalMoves = getLegalMoves(aiPieces, humanPieces)
        if (legalMoves.isEmpty()) {
            return BoardPosition(0, 0)
        }

        return when (difficulty) {
            AiDifficulty.EASY -> getEasyMove(legalMoves, aiPieces)
            AiDifficulty.MEDIUM -> getMediumMove(legalMoves, aiPieces, humanPieces)
            AiDifficulty.HARD -> getHardMove(legalMoves, aiPieces, humanPieces)
        }
    }

    private fun getEasyMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>
    ): BoardPosition {
        // 20% chance to take an immediate winning move if available, otherwise random
        if (Random.nextFloat() < 0.2f) {
            for (move in legalMoves) {
                val simulatedAi = simulateMove(aiPieces, move)
                if (checkWin(simulatedAi) != null) {
                    return move
                }
            }
        }
        return legalMoves.random()
    }

    private fun getMediumMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>
    ): BoardPosition {
        // 1. Check for immediate winning move for AI (100%)
        for (move in legalMoves) {
            val simulatedAi = simulateMove(aiPieces, move)
            if (checkWin(simulatedAi) != null) {
                return move
            }
        }

        // 2. Check for blocking human immediate win (80% chance)
        if (Random.nextFloat() < 0.8f) {
            val humanLegal = getLegalMoves(humanPieces, aiPieces)
            for (move in humanLegal) {
                val simulatedHuman = simulateMove(humanPieces, move)
                if (checkWin(simulatedHuman) != null && move in legalMoves) {
                    return move
                }
            }
        }

        // 3. Prefer Center (1,1)
        val center = BoardPosition(1, 1)
        if (center in legalMoves && Random.nextFloat() < 0.6f) {
            return center
        }

        // 4. Random legal move
        return legalMoves.random()
    }

    private fun getHardMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>
    ): BoardPosition {
        // 1. Immediate Win check (Highest Priority)
        for (move in legalMoves) {
            val simulatedAi = simulateMove(aiPieces, move)
            if (checkWin(simulatedAi) != null) {
                return move
            }
        }

        // 2. Immediate Block check (Block Human from winning on next turn)
        val humanLegal = getLegalMoves(humanPieces, aiPieces)
        val winningHumanMoves = mutableListOf<BoardPosition>()
        for (hMove in humanLegal) {
            val simulatedHuman = simulateMove(humanPieces, hMove)
            if (checkWin(simulatedHuman) != null) {
                winningHumanMoves.add(hMove)
            }
        }
        for (blockMove in winningHumanMoves) {
            if (blockMove in legalMoves) {
                return blockMove
            }
        }

        // 3. Minimax-based position evaluation for Endless TTT
        var bestScore = Int.MIN_VALUE
        var bestMove = legalMoves.first()

        for (move in legalMoves.shuffled()) {
            val nextAi = simulateMove(aiPieces, move)
            val score = evaluateBoardState(nextAi, humanPieces, depth = 0, isAiTurn = false, alpha = Int.MIN_VALUE, beta = Int.MAX_VALUE)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }

    /**
     * Minimax depth search adapted to Endless Tic Tac Toe mechanics.
     */
    private fun evaluateBoardState(
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>,
        depth: Int,
        isAiTurn: Boolean,
        alpha: Int,
        beta: Int
    ): Int {
        if (checkWin(aiPieces) != null) return 100 - depth
        if (checkWin(humanPieces) != null) return depth - 100
        if (depth >= 3) {
            return heuristicScore(aiPieces, humanPieces)
        }

        var curAlpha = alpha
        var curBeta = beta

        if (isAiTurn) {
            var maxEval = Int.MIN_VALUE
            val moves = getLegalMoves(aiPieces, humanPieces)
            for (move in moves) {
                val nextAi = simulateMove(aiPieces, move)
                val eval = evaluateBoardState(nextAi, humanPieces, depth + 1, false, curAlpha, curBeta)
                maxEval = maxOf(maxEval, eval)
                curAlpha = maxOf(curAlpha, eval)
                if (curBeta <= curAlpha) break
            }
            return if (moves.isEmpty()) heuristicScore(aiPieces, humanPieces) else maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val moves = getLegalMoves(humanPieces, aiPieces)
            for (move in moves) {
                val nextHuman = simulateMove(humanPieces, move)
                val eval = evaluateBoardState(aiPieces, nextHuman, depth + 1, true, curAlpha, curBeta)
                minEval = minOf(minEval, eval)
                curBeta = minOf(curBeta, eval)
                if (curBeta <= curAlpha) break
            }
            return if (moves.isEmpty()) heuristicScore(aiPieces, humanPieces) else minEval
        }
    }

    private fun heuristicScore(
        aiPieces: List<BoardPosition>,
        humanPieces: List<BoardPosition>
    ): Int {
        var score = 0
        val aiSet = aiPieces.toSet()
        val humanSet = humanPieces.toSet()

        // Center bonus
        val center = BoardPosition(1, 1)
        if (center in aiSet) score += 5
        if (center in humanSet) score -= 5

        // Corners bonus
        val corners = listOf(BoardPosition(0, 0), BoardPosition(0, 2), BoardPosition(2, 0), BoardPosition(2, 2))
        for (corner in corners) {
            if (corner in aiSet) score += 2
            if (corner in humanSet) score -= 2
        }

        // Line threats evaluation
        for (line in WINNING_LINES) {
            val aiCount = line.count { it in aiSet }
            val humanCount = line.count { it in humanSet }

            if (aiCount == 2 && humanCount == 0) score += 15
            if (humanCount == 2 && aiCount == 0) score -= 20
            if (aiCount == 1 && humanCount == 0) score += 2
            if (humanCount == 1 && aiCount == 0) score -= 2
        }

        return score
    }
}
