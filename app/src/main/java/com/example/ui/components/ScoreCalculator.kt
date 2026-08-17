package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActiveReaction
import com.example.domain.model.GameMode
import com.example.domain.model.LocalPlayerRole
import com.example.domain.model.MatchStats
import com.example.domain.model.PlayerCount
import com.example.domain.model.PlayerSymbol
import com.example.ui.theme.BoardDark
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerTickGreen
import com.example.ui.theme.PlayerTrianglePurple
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark

@Composable
fun ScoreCalculator(
    player1Name: String,
    player2Name: String,
    player3Name: String = "Player 3",
    player4Name: String = "Player 4",
    playerCount: PlayerCount = PlayerCount.TWO,
    matchStats: MatchStats,
    currentTurn: PlayerSymbol,
    gameMode: GameMode,
    isAiThinking: Boolean,
    localPlayerRole: LocalPlayerRole = LocalPlayerRole.LOCAL_ALL,
    activeReactions: List<ActiveReaction> = emptyList(),
    onPauseClick: () -> Unit,
    onRestartRoundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetSets = matchStats.targetSets
    val symbols = playerCount.symbols

    val playerNamesMap = mapOf(
        PlayerSymbol.X to player1Name,
        PlayerSymbol.O to player2Name,
        PlayerSymbol.TICK to player3Name,
        PlayerSymbol.TRIANGLE to player4Name
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Bar with Bold Typography
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (gameMode == GameMode.ONLINE_MULTIPLAYER) "ONLINE MULTIPLAYER" else "ENDLESS EDITION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "ENDLESS TTT (${playerCount.count} PLAYERS)",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp,
                    color = TextDark
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRestartRoundClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.08f))
                        .testTag("restart_round_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Round",
                        tint = TextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.08f))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = TextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bold Typography Scoreboard Container (Supports 2, 3, or 4 players)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(BoardDark)
                .border(width = 2.dp, color = Color(0xFF1E1E22), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                symbols.forEachIndexed { index, symbol ->
                    val isThisTurn = currentTurn == symbol
                    val playerName = playerNamesMap[symbol] ?: "Player ${index + 1}"
                    val wins = matchStats.getWins(symbol)

                    val playerColor = when (symbol) {
                        PlayerSymbol.X -> PlayerXBlue
                        PlayerSymbol.O -> PlayerORed
                        PlayerSymbol.TICK -> PlayerTickGreen
                        PlayerSymbol.TRIANGLE -> PlayerTrianglePurple
                    }

                    val playerReaction = activeReactions.lastOrNull {
                        it.playerSymbol == symbol
                    }

                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = symbol.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = playerColor
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = playerName.uppercase(),
                                fontSize = if (playerCount.count > 2) 9.5.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = playerColor,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
                            Spacer(modifier = Modifier.height(2.dp))
                            PlayerReactionBadge(reaction = playerReaction, playerSymbol = symbol)
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = String.format("%02d", wins),
                            fontSize = if (playerCount.count > 2) 30.sp else 36.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isThisTurn) Color.White else Color.White.copy(alpha = 0.75f),
                            letterSpacing = (-1).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Turn Status Pill Banner
        TurnStatusBanner(
            currentTurn = currentTurn,
            playerNamesMap = playerNamesMap,
            isAiThinking = isAiThinking,
            targetSets = targetSets,
            playerCount = playerCount,
            gameMode = gameMode,
            localPlayerRole = localPlayerRole
        )
    }
}

@Composable
private fun TurnStatusBanner(
    currentTurn: PlayerSymbol,
    playerNamesMap: Map<PlayerSymbol, String>,
    isAiThinking: Boolean,
    targetSets: Int,
    playerCount: PlayerCount,
    gameMode: GameMode,
    localPlayerRole: LocalPlayerRole
) {
    val activePlayerName = playerNamesMap[currentTurn] ?: "Player"
    val turnSymbol = currentTurn.displayName

    val turnText = when {
        isAiThinking -> "AI IS PLANNING NEXT MOVE..."
        gameMode == GameMode.ONLINE_MULTIPLAYER -> {
            val isMyTurn = when (localPlayerRole) {
                LocalPlayerRole.HOST_X -> currentTurn == PlayerSymbol.X
                LocalPlayerRole.GUEST_O -> currentTurn == PlayerSymbol.O
                LocalPlayerRole.GUEST_TICK -> currentTurn == PlayerSymbol.TICK
                LocalPlayerRole.GUEST_TRIANGLE -> currentTurn == PlayerSymbol.TRIANGLE
                LocalPlayerRole.LOCAL_ALL -> true
            }
            if (isMyTurn) "YOUR TURN ($turnSymbol)" else "WAITING FOR $activePlayerName ($turnSymbol)..."
        }
        else -> "$activePlayerName'S TURN ($turnSymbol)"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // High impact black status pill with yellow text
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = turnText,
                color = GameYellowVibrant,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        // Subtitle line (Set Mode info)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FIRST TO $targetSets WINS",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TextDark.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${playerCount.gridSize}×${playerCount.gridSize} GRID • 3-PIECE LIMIT",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
