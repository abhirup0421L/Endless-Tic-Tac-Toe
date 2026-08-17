package com.example.domain.ai

import com.example.domain.model.AiDifficulty
import com.example.domain.model.BoardPosition
import kotlin.random.Random

object EndlessAiEngine {

    /**
     * Compute all valid winning lines (3 in a row) for a grid of size [gridSize]x[gridSize].
     */
    fun getWinningLines(gridSize: Int): List<List<BoardPosition>> {
        val lines = mutableListOf<List<BoardPosition>>()
        val winLength = 3 // Universal 3-in-a-row rule

        // Horizontal spans
        for (r in 0 until gridSize) {
            for (c in 0..(gridSize - winLength)) {
                lines.add((0 until winLength).map { BoardPosition(r, c + it) })
            }
        }
        // Vertical spans
        for (c in 0 until gridSize) {
            for (r in 0..(gridSize - winLength)) {
                lines.add((0 until winLength).map { BoardPosition(r + it, c) })
            }
        }
        // Diagonal down-right spans (\)
        for (r in 0..(gridSize - winLength)) {
            for (c in 0..(gridSize - winLength)) {
                lines.add((0 until winLength).map { BoardPosition(r + it, c + it) })
            }
        }
        // Diagonal up-right spans (/)
        for (r in (winLength - 1) until gridSize) {
            for (c in 0..(gridSize - winLength)) {
                lines.add((0 until winLength).map { BoardPosition(r - it, c + it) })
            }
        }
        return lines
    }

    /**
     * Check if [pieces] forms any winning line on a grid of [gridSize].
     */
    fun checkWin(pieces: List<BoardPosition>, gridSize: Int = 3): List<BoardPosition>? {
        if (pieces.size < 3) return null
        val set = pieces.toSet()
        val lines = getWinningLines(gridSize)
        for (line in lines) {
            if (line.all { it in set }) {
                return line
            }
        }
        return null
    }

    /**
     * Compute simulated pieces after placing at [targetPos] with FIFO 3-piece limit.
     */
    fun simulateMove(
        currentPieces: List<BoardPosition>,
        targetPos: BoardPosition
    ): List<BoardPosition> {
        val updated = currentPieces.toMutableList()
        if (updated.size >= 3) {
            updated.removeAt(0) // Oldest piece vanishes
        }
        updated.add(targetPos)
        return updated
    }

    /**
     * Calculate legal moves considering multi-player pieces on board of size [gridSize].
     */
    fun getLegalMoves(
        currentPieces: List<BoardPosition>,
        otherPlayersPieces: List<List<BoardPosition>>,
        gridSize: Int = 3
    ): List<BoardPosition> {
        val occupiedByOthers = otherPlayersPieces.flatten().toSet()
        val occupiedBySelf = if (currentPieces.size >= 3) {
            currentPieces.drop(1).toSet() // Oldest piece is about to vanish
        } else {
            currentPieces.toSet()
        }

        val legal = mutableListOf<BoardPosition>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val pos = BoardPosition(r, c)
                if (pos !in occupiedByOthers && pos !in occupiedBySelf) {
                    legal.add(pos)
                }
            }
        }
        return legal
    }

    /**
     * Determine best move based on AI difficulty, opponent states, and grid size.
     */
    fun getBestMove(
        difficulty: AiDifficulty,
        aiPieces: List<BoardPosition>,
        otherPlayersPieces: List<List<BoardPosition>>,
        gridSize: Int = 3
    ): BoardPosition {
        val legalMoves = getLegalMoves(aiPieces, otherPlayersPieces, gridSize)
        if (legalMoves.isEmpty()) {
            return BoardPosition(0, 0)
        }

        return when (difficulty) {
            AiDifficulty.EASY -> getEasyMove(legalMoves, aiPieces, gridSize)
            AiDifficulty.MEDIUM -> getMediumMove(legalMoves, aiPieces, otherPlayersPieces, gridSize)
            AiDifficulty.HARD -> getHardMove(legalMoves, aiPieces, otherPlayersPieces, gridSize)
        }
    }

    private fun getEasyMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>,
        gridSize: Int
    ): BoardPosition {
        // 25% chance to take immediate win, otherwise random
        if (Random.nextFloat() < 0.25f) {
            for (move in legalMoves) {
                val simulated = simulateMove(aiPieces, move)
                if (checkWin(simulated, gridSize) != null) {
                    return move
                }
            }
        }
        return legalMoves.random()
    }

    private fun getMediumMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>,
        otherPlayersPieces: List<List<BoardPosition>>,
        gridSize: Int
    ): BoardPosition {
        // 1. Check for immediate winning move for AI (100%)
        for (move in legalMoves) {
            val simulated = simulateMove(aiPieces, move)
            if (checkWin(simulated, gridSize) != null) {
                return move
            }
        }

        // 2. Check for blocking any opponent from winning (80% chance)
        if (Random.nextFloat() < 0.80f) {
            for (oppPieces in otherPlayersPieces) {
                for (move in legalMoves) {
                    val simulatedOpp = simulateMove(oppPieces, move)
                    if (checkWin(simulatedOpp, gridSize) != null) {
                        return move
                    }
                }
            }
        }

        // 3. Prefer Center or near-center cells
        val center = BoardPosition(gridSize / 2, gridSize / 2)
        if (center in legalMoves && Random.nextFloat() < 0.5f) {
            return center
        }

        return legalMoves.random()
    }

    private fun getHardMove(
        legalMoves: List<BoardPosition>,
        aiPieces: List<BoardPosition>,
        otherPlayersPieces: List<List<BoardPosition>>,
        gridSize: Int
    ): BoardPosition {
        // 1. Immediate Win check (Highest Priority)
        for (move in legalMoves) {
            val simulated = simulateMove(aiPieces, move)
            if (checkWin(simulated, gridSize) != null) {
                return move
            }
        }

        // 2. Immediate Block check across ALL opponents
        for (oppPieces in otherPlayersPieces) {
            for (move in legalMoves) {
                val simulatedOpp = simulateMove(oppPieces, move)
                if (checkWin(simulatedOpp, gridSize) != null) {
                    return move // Block immediate threat
                }
            }
        }

        // 3. Heuristic scoring across winning lines
        val winningLines = getWinningLines(gridSize)
        var bestScore = Int.MIN_VALUE
        var bestMove = legalMoves.first()

        val allOpponentSet = otherPlayersPieces.flatten().toSet()

        for (move in legalMoves.shuffled()) {
            val nextAiPieces = simulateMove(aiPieces, move)
            val aiSet = nextAiPieces.toSet()
            var score = 0

            // Distance to center bonus
            val centerRow = (gridSize - 1) / 2.0
            val centerCol = (gridSize - 1) / 2.0
            val dist = Math.abs(move.row - centerRow) + Math.abs(move.col - centerCol)
            score += (gridSize - dist.toInt()) * 2

            // Line potentials
            for (line in winningLines) {
                val aiInLine = line.count { it in aiSet }
                val oppInLine = line.count { it in allOpponentSet }

                if (aiInLine == 2 && oppInLine == 0) score += 20
                if (aiInLine == 1 && oppInLine == 0) score += 5
                if (oppInLine == 2 && aiInLine == 1) score += 8 // disruption
            }

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }
}
