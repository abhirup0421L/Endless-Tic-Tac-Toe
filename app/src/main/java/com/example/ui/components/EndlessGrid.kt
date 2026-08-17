package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.BoardPosition
import com.example.domain.model.PlayerSymbol
import com.example.domain.model.WinningLine
import com.example.ui.theme.BoardCardBg
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.CellDarkBorder
import com.example.ui.theme.GridDividerBlack
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerTickGreen
import com.example.ui.theme.PlayerTrianglePurple
import com.example.ui.theme.PlayerXBlue

@Composable
fun EndlessGrid(
    playerPiecesMap: Map<PlayerSymbol, List<BoardPosition>>,
    gridSize: Int,
    currentTurn: PlayerSymbol,
    winningLine: WinningLine?,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Map of oldest piece per player when that player has reached the 3-piece maximum
    val oldestPieces = playerPiecesMap.mapValues { (_, pieces) ->
        if (pieces.size >= 3) pieces.firstOrNull() else null
    }

    val winningPositions = winningLine?.positions?.toSet() ?: emptySet()

    val cellSpacing = when (gridSize) {
        3 -> 8.dp
        4 -> 6.dp
        else -> 5.dp
    }

    val cellCornerRadius = when (gridSize) {
        3 -> 14.dp
        4 -> 11.dp
        else -> 9.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BoardDark)
            .border(width = 3.dp, color = GridDividerBlack, shape = RoundedCornerShape(24.dp))
            .padding(if (gridSize >= 5) 8.dp else 12.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(cellSpacing)
        ) {
            for (row in 0 until gridSize) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                ) {
                    for (col in 0 until gridSize) {
                        val pos = BoardPosition(row, col)

                        // Find if any player occupies this cell
                        var playerAtCell: PlayerSymbol? = null
                        var orderNumber = 0

                        for ((sym, pieces) in playerPiecesMap) {
                            val idx = pieces.indexOf(pos)
                            if (idx >= 0) {
                                playerAtCell = sym
                                orderNumber = idx + 1
                                break
                            }
                        }

                        val isOldest = playerAtCell != null &&
                                pos == oldestPieces[playerAtCell] &&
                                currentTurn == playerAtCell

                        val isWinningCell = pos in winningPositions

                        CellTile(
                            row = row,
                            col = col,
                            player = playerAtCell,
                            isOldest = isOldest,
                            order = orderNumber,
                            isWinningPiece = isWinningCell,
                            cornerRadius = cellCornerRadius,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }

        // Winning line drawing across cells
        if (winningLine != null && winningLine.positions.size >= 3) {
            WinningLineOverlay(
                winningPositions = winningLine.positions,
                winner = winningLine.winner,
                gridSize = gridSize
            )
        }
    }
}

@Composable
fun CellTile(
    row: Int,
    col: Int,
    player: PlayerSymbol?,
    isOldest: Boolean,
    order: Int,
    isWinningPiece: Boolean,
    cornerRadius: androidx.compose.ui.unit.Dp = 14.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellShape = RoundedCornerShape(cornerRadius)

    val playerColor = when (player) {
        PlayerSymbol.X -> PlayerXBlue
        PlayerSymbol.O -> PlayerORed
        PlayerSymbol.TICK -> PlayerTickGreen
        PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
        null -> Color.Transparent
    }

    val borderModifier = if (isWinningPiece) {
        Modifier.border(
            width = 3.dp,
            color = playerColor,
            shape = cellShape
        )
    } else if (isOldest) {
        Modifier.border(
            width = 2.dp,
            color = playerColor.copy(alpha = 0.7f),
            shape = cellShape
        )
    } else {
        Modifier.border(width = 1.dp, color = CellDarkBorder, shape = cellShape)
    }

    Box(
        modifier = modifier
            .testTag("cell_${row}_${col}")
            .clip(cellShape)
            .background(if (isWinningPiece) BoardCardBg else CellDarkBg)
            .then(borderModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            PieceSymbol(
                player = player,
                isOldest = isOldest,
                order = order,
                isWinningPiece = isWinningPiece
            )
        }
    }
}

@Composable
fun WinningLineOverlay(
    winningPositions: List<BoardPosition>,
    winner: PlayerSymbol,
    gridSize: Int = 3
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "winningLine"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val first = winningPositions.first()
        val last = winningPositions.last()

        val startOffset = getCellCenter(first.row, first.col, w, h, gridSize)
        val endOffset = getCellCenter(last.row, last.col, w, h, gridSize)

        val currentEnd = Offset(
            x = startOffset.x + (endOffset.x - startOffset.x) * progress,
            y = startOffset.y + (endOffset.y - startOffset.y) * progress
        )

        val color = when (winner) {
            PlayerSymbol.X -> PlayerXBlue
            PlayerSymbol.O -> PlayerORed
            PlayerSymbol.TICK -> PlayerTickGreen
            PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
        }

        // Outer neon glow
        drawLine(
            color = color.copy(alpha = 0.45f),
            start = startOffset,
            end = currentEnd,
            strokeWidth = 16.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Core bright line
        drawLine(
            color = Color.White,
            start = startOffset,
            end = currentEnd,
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun getCellCenter(row: Int, col: Int, totalW: Float, totalH: Float, gridSize: Int): Offset {
    val cellW = totalW / gridSize.toFloat()
    val cellH = totalH / gridSize.toFloat()
    return Offset(
        x = col * cellW + cellW / 2f,
        y = row * cellH + cellH / 2f
    )
}
