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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActiveReaction
import com.example.domain.model.GameMode
import com.example.domain.model.LocalPlayerRole
import com.example.domain.model.MatchStats
import com.example.domain.model.PlayerSymbol
import com.example.ui.theme.BoardDark
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark

@Composable
fun ScoreCalculator(
    player1Name: String,
    player2Name: String,
    matchStats: MatchStats,
    currentTurn: PlayerSymbol,
    gameMode: GameMode,
    isAiThinking: Boolean,
    localPlayerRole: LocalPlayerRole = LocalPlayerRole.LOCAL_BOTH,
    activeReactions: List<ActiveReaction> = emptyList(),
    onPauseClick: () -> Unit,
    onRestartRoundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isP1Turn = currentTurn == PlayerSymbol.X
    val targetSets = matchStats.targetSets

    // Find latest reaction for player 1 (X) and player 2 (O)
    val p1Reaction = activeReactions.lastOrNull { 
        if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
            if (localPlayerRole == LocalPlayerRole.HOST_X) it.isLocal else !it.isLocal
        } else false
    }
    val p2Reaction = activeReactions.lastOrNull {
        if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
            if (localPlayerRole == LocalPlayerRole.GUEST_O) it.isLocal else !it.isLocal
        } else false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Bar with Bold Typography
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (gameMode == GameMode.ONLINE_MULTIPLAYER) "ONLINE MULTIPLAYER" else "ANDROID EDITION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "ENDLESS TTT",
                    fontSize = 20.sp,
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

        // Bold Typography Scoreboard Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(BoardDark)
                .border(width = 2.dp, color = Color(0xFF1E1E22), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Player 1 Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val p1Label = when {
                        gameMode == GameMode.ONLINE_MULTIPLAYER && localPlayerRole == LocalPlayerRole.HOST_X -> "$player1Name (YOU)"
                        gameMode == GameMode.ONLINE_MULTIPLAYER -> "$player1Name (HOST)"
                        else -> player1Name
                    }
                    Text(
                        text = p1Label.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerXBlue,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
                        Spacer(modifier = Modifier.height(2.dp))
                        PlayerReactionBadge(reaction = p1Reaction, isPlayerX = true)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("%02d", matchStats.player1Wins),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isP1Turn) Color.White else Color.White.copy(alpha = 0.8f),
                        letterSpacing = (-1).sp
                    )
                }

                // Divider and "vs"
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "VS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                // Player 2 Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val p2Label = when {
                        gameMode == GameMode.ONLINE_MULTIPLAYER && localPlayerRole == LocalPlayerRole.GUEST_O -> "$player2Name (YOU)"
                        gameMode == GameMode.ONLINE_MULTIPLAYER -> "$player2Name (GUEST)"
                        gameMode == GameMode.SINGLE_PLAYER -> player2Name
                        else -> player2Name
                    }
                    Text(
                        text = p2Label.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerORed,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
                        Spacer(modifier = Modifier.height(2.dp))
                        PlayerReactionBadge(reaction = p2Reaction, isPlayerX = false)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("%02d", matchStats.player2Wins),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (!isP1Turn) Color.White else Color.White.copy(alpha = 0.8f),
                        letterSpacing = (-1).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Turn Status Pill Banner (Bold Typography design)
        TurnStatusBanner(
            currentTurn = currentTurn,
            player1Name = player1Name,
            player2Name = player2Name,
            isAiThinking = isAiThinking,
            targetSets = targetSets,
            gameMode = gameMode,
            localPlayerRole = localPlayerRole
        )
    }
}

@Composable
private fun TurnStatusBanner(
    currentTurn: PlayerSymbol,
    player1Name: String,
    player2Name: String,
    isAiThinking: Boolean,
    targetSets: Int,
    gameMode: GameMode,
    localPlayerRole: LocalPlayerRole
) {
    val isP1 = currentTurn == PlayerSymbol.X
    val turnText = when {
        isAiThinking -> "AI IS PLANNING..."
        gameMode == GameMode.ONLINE_MULTIPLAYER -> {
            val isMyTurn = (localPlayerRole == LocalPlayerRole.HOST_X && isP1) ||
                    (localPlayerRole == LocalPlayerRole.GUEST_O && !isP1)
            if (isMyTurn) "YOUR TURN (${if (isP1) "X" else "O"})" else "WAITING FOR OPPONENT (${if (isP1) "X" else "O"})..."
        }
        isP1 -> "$player1Name TURN (X)"
        gameMode == GameMode.SINGLE_PLAYER -> "AI TURN (O)"
        else -> "$player2Name TURN (O)"
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
                .padding(horizontal = 22.dp, vertical = 7.dp)
        ) {
            Text(
                text = turnText,
                color = GameYellowVibrant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        // Subtitle line (Set Mode info)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SET MODE: $targetSets WINS",
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
            val modeSubText = when (gameMode) {
                GameMode.SINGLE_PLAYER -> "VS AI OPPONENT"
                GameMode.FRIEND -> "PASS & PLAY"
                GameMode.ONLINE_MULTIPLAYER -> "ONLINE RELAY SYNC"
            }
            Text(
                text = modeSubText,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
