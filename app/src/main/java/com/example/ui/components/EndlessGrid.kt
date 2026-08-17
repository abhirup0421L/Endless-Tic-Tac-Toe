package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import com.example.ui.theme.PlayerXBlue

@Composable
fun EndlessGrid(
    player1Pieces: List<BoardPosition>,
    player2Pieces: List<BoardPosition>,
    currentTurn: PlayerSymbol,
    winningLine: WinningLine?,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Oldest piece for each player when count is 3
    val oldestP1 = if (player1Pieces.size == 3) player1Pieces.first() else null
    val oldestP2 = if (player2Pieces.size == 3) player2Pieces.first() else null

    val winningPositions = winningLine?.positions?.toSet() ?: emptySet()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(BoardDark)
            .border(width = 3.dp, color = GridDividerBlack, shape = RoundedCornerShape(24.dp))
            .padding(12.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..2) {
                        val pos = BoardPosition(row, col)

                        val p1Index = player1Pieces.indexOf(pos)
                        val p2Index = player2Pieces.indexOf(pos)

                        val playerAtCell = when {
                            p1Index >= 0 -> PlayerSymbol.X
                            p2Index >= 0 -> PlayerSymbol.O
                            else -> null
                        }

                        val isOldest = (pos == oldestP1 && currentTurn == PlayerSymbol.X) ||
                                (pos == oldestP2 && currentTurn == PlayerSymbol.O)

                        val orderNumber = when {
                            p1Index >= 0 -> p1Index + 1
                            p2Index >= 0 -> p2Index + 1
                            else -> 0
                        }

                        val isWinningCell = pos in winningPositions

                        CellTile(
                            row = row,
                            col = col,
                            player = playerAtCell,
                            isOldest = isOldest,
                            order = orderNumber,
                            isWinningPiece = isWinningCell,
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
                winner = winningLine.winner
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellShape = RoundedCornerShape(14.dp)

    val borderModifier = if (isWinningPiece) {
        Modifier.border(
            width = 3.dp,
            color = if (player == PlayerSymbol.X) PlayerXBlue else PlayerORed,
            shape = cellShape
        )
    } else if (isOldest) {
        Modifier.border(
            width = 2.dp,
            color = if (player == PlayerSymbol.X) PlayerXBlue.copy(alpha = 0.6f) else PlayerORed.copy(alpha = 0.6f),
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
    winner: PlayerSymbol
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "winningLine"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val first = winningPositions.first()
        val last = winningPositions.last()

        val startOffset = getCellCenter(first.row, first.col, w, h)
        val endOffset = getCellCenter(last.row, last.col, w, h)

        val currentEnd = Offset(
            x = startOffset.x + (endOffset.x - startOffset.x) * progress,
            y = startOffset.y + (endOffset.y - startOffset.y) * progress
        )

        val color = if (winner == PlayerSymbol.X) PlayerXBlue else PlayerORed

        // Outer glow
        drawLine(
            color = color.copy(alpha = 0.4f),
            start = startOffset,
            end = currentEnd,
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Core bright line
        drawLine(
            color = Color.White,
            start = startOffset,
            end = currentEnd,
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun getCellCenter(row: Int, col: Int, totalW: Float, totalH: Float): Offset {
    val cellW = totalW / 3f
    val cellH = totalH / 3f
    return Offset(
        x = col * cellW + cellW / 2f,
        y = row * cellH + cellH / 2f
    )
}
