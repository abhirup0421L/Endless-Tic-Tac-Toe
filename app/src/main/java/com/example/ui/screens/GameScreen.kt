package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ActiveReaction
import com.example.domain.model.BoardPosition
import com.example.domain.model.GameMode
import com.example.domain.model.LocalPlayerRole
import com.example.domain.model.MatchStats
import com.example.domain.model.NetworkConnectionStatus
import com.example.domain.model.PlayerCount
import com.example.domain.model.PlayerSymbol
import com.example.domain.model.WinningLine
import com.example.ui.components.EndlessGrid
import com.example.ui.components.ReactionBar
import com.example.ui.components.ScoreCalculator
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.CellDarkBorder
import com.example.ui.theme.GameYellowBackground
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerTickGreen
import com.example.ui.theme.PlayerTrianglePurple
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLightSecondary
import com.example.ui.viewmodel.RoundStatus

@Composable
fun GameScreen(
    player1Name: String,
    player2Name: String,
    player3Name: String = "Player 3",
    player4Name: String = "Player 4",
    playerCount: PlayerCount = PlayerCount.TWO,
    player1Pieces: List<BoardPosition>,
    player2Pieces: List<BoardPosition>,
    player3Pieces: List<BoardPosition> = emptyList(),
    player4Pieces: List<BoardPosition> = emptyList(),
    currentTurn: PlayerSymbol,
    matchStats: MatchStats,
    roundStatus: RoundStatus,
    winningLine: WinningLine?,
    matchWinner: PlayerSymbol?,
    gameMode: GameMode,
    localPlayerRole: LocalPlayerRole,
    networkStatus: NetworkConnectionStatus,
    activeReactions: List<ActiveReaction>,
    isAiThinking: Boolean,
    showPauseDialog: Boolean,
    showVictoryDialog: Boolean,
    showOpponentLeftDialog: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit,
    onSendReaction: (emoji: String) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartRoundClick: () -> Unit,
    onRestartMatchClick: () -> Unit,
    onExitToHomeClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseOpponentLeftDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerNamesMap = mapOf(
        PlayerSymbol.X to player1Name,
        PlayerSymbol.O to player2Name,
        PlayerSymbol.TICK to player3Name,
        PlayerSymbol.TRIANGLE to player4Name
    )

    val piecesMap = mutableMapOf<PlayerSymbol, List<BoardPosition>>().apply {
        put(PlayerSymbol.X, player1Pieces)
        put(PlayerSymbol.O, player2Pieces)
        if (playerCount.count >= 3) put(PlayerSymbol.TICK, player3Pieces)
        if (playerCount.count >= 4) put(PlayerSymbol.TRIANGLE, player4Pieces)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameYellowBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Score Calculator at the top of the screen (supports 2, 3, 4 players)
            ScoreCalculator(
                player1Name = player1Name,
                player2Name = player2Name,
                player3Name = player3Name,
                player4Name = player4Name,
                playerCount = playerCount,
                matchStats = matchStats,
                currentTurn = currentTurn,
                gameMode = gameMode,
                localPlayerRole = localPlayerRole,
                isAiThinking = isAiThinking,
                activeReactions = activeReactions,
                onPauseClick = onPauseClick,
                onRestartRoundClick = onRestartRoundClick,
                modifier = Modifier.testTag("score_calculator")
            )

            // 2. Dynamic Grid at Center (3x3, 4x4, or 5x5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EndlessGrid(
                    playerPiecesMap = piecesMap,
                    gridSize = playerCount.gridSize,
                    currentTurn = currentTurn,
                    winningLine = winningLine,
                    onCellClick = onCellClick
                )

                // Point celebration popup overlay
                if (roundStatus == RoundStatus.POINT_SCORED && winningLine != null) {
                    PointScoredBanner(
                        winner = winningLine.winner,
                        winnerName = playerNamesMap[winningLine.winner] ?: "Player"
                    )
                }
            }

            // 3. Online Live Reaction Bar (Only in Online Multiplayer)
            if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
                ReactionBar(
                    onSendReaction = onSendReaction,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 4. Bottom Action Controls & Rules Helper Tip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reset Round Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .clickable(onClick = onRestartRoundClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESET ROUND",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Main Menu / Pause Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                            .clickable(onClick = onPauseClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MENU / PAUSE",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Rule helper tip
                Text(
                    text = "ENDLESS RULE: 3-IN-A-ROW • MAX 3 PIECES PER PLAYER • OLDEST VANISHES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.55f),
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Pause Menu Dialog
        if (showPauseDialog) {
            PauseMenuDialog(
                onResume = onResumeClick,
                onRestartMatch = onRestartMatchClick,
                onOpenSettings = onOpenSettings,
                onExitToHome = onExitToHomeClick
            )
        }

        // Victory / Match Championship Dialog
        if (showVictoryDialog && matchWinner != null) {
            VictoryDialog(
                winner = matchWinner,
                playerNamesMap = playerNamesMap,
                matchStats = matchStats,
                playerCount = playerCount,
                onRestartMatch = onRestartMatchClick,
                onExitToHome = onExitToHomeClick
            )
        }

        // Opponent Left Dialog (Online Mode)
        if (showOpponentLeftDialog) {
            AlertDialog(
                onDismissRequest = onCloseOpponentLeftDialog,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = PlayerORed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opponent Disconnected",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Text(
                        text = "A player left the online match or lost connection.",
                        color = TextLightSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onCloseOpponentLeftDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = GameYellowVibrant)
                    ) {
                        Text(
                            text = "RETURN TO HOME",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = BoardDark,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun PointScoredBanner(
    winner: PlayerSymbol,
    winnerName: String
) {
    val color = when (winner) {
        PlayerSymbol.X -> PlayerXBlue
        PlayerSymbol.O -> PlayerORed
        PlayerSymbol.TICK -> PlayerTickGreen
        PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F0F11))
            .border(2.5.dp, color, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = winner.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "$winnerName SCORES +1 POINT!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Starting next round...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PauseMenuDialog(
    onResume: () -> Unit,
    onRestartMatch: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitToHome: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BoardDark,
            border = androidx.compose.foundation.BorderStroke(2.dp, CellDarkBorder),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MATCH PAUSED",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )

                // Resume
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameYellowVibrant)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME GAME",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                // Restart Match
                Button(
                    onClick = onRestartMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222226))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTART MATCH",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Settings
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222226))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SETTINGS & RECORDS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Exit to Home
                OutlinedButton(
                    onClick = onExitToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayerORed),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PlayerORed.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = PlayerORed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXIT TO HOME",
                        color = PlayerORed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VictoryDialog(
    winner: PlayerSymbol,
    playerNamesMap: Map<PlayerSymbol, String>,
    matchStats: MatchStats,
    playerCount: PlayerCount,
    onRestartMatch: () -> Unit,
    onExitToHome: () -> Unit
) {
    val winnerColor = when (winner) {
        PlayerSymbol.X -> PlayerXBlue
        PlayerSymbol.O -> PlayerORed
        PlayerSymbol.TICK -> PlayerTickGreen
        PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
    }
    val winnerName = playerNamesMap[winner] ?: "Player"
    val targetSets = matchStats.targetSets

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BoardDark,
            border = androidx.compose.foundation.BorderStroke(2.dp, winnerColor),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trophy Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GameYellowVibrant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "MATCH CHAMPION!",
                    color = GameYellowVibrant,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "$winnerName (${winner.displayName}) WINS THE MATCH!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                // Scoreboard Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CellDarkBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    playerCount.symbols.forEach { symbol ->
                        val name = playerNamesMap[symbol] ?: "Player"
                        val score = matchStats.getWins(symbol)
                        val color = when (symbol) {
                            PlayerSymbol.X -> PlayerXBlue
                            PlayerSymbol.O -> PlayerORed
                            PlayerSymbol.TICK -> PlayerTickGreen
                            PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = name,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "$score",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = onRestartMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameYellowVibrant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY AGAIN",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onExitToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN MENU",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
