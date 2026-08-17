package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AiDifficulty
import com.example.domain.model.GameMode
import com.example.domain.model.PlayerCount
import com.example.domain.model.TargetSets
import com.example.ui.theme.BoardCardBg
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.CellDarkBorder
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLightSecondary

@Composable
fun ModeSelectDialog(
    gameMode: GameMode,
    onDismiss: () -> Unit,
    onStartGame: (playerCount: PlayerCount, difficulty: AiDifficulty, targetSets: TargetSets) -> Unit
) {
    var selectedPlayerCount by remember { mutableStateOf(PlayerCount.TWO) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var selectedSets by remember { mutableStateOf(TargetSets.FIVE) }

    val isSinglePlayer = gameMode == GameMode.SINGLE_PLAYER

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BoardDark,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(2.dp, Color(0xFF27272A), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSinglePlayer) PlayerXBlue.copy(alpha = 0.2f) else PlayerORed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSinglePlayer) Icons.Default.SmartToy else Icons.Default.People,
                                contentDescription = null,
                                tint = if (isSinglePlayer) PlayerXBlue else PlayerORed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isSinglePlayer) "SINGLE PLAYER (AI)" else "PASS & PLAY (LOCAL)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_mode_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextLightSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Player Count Selection (2, 3, 4 Players)
                Text(
                    text = "SELECT NUMBER OF PLAYERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = GameYellowVibrant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerCount.entries.forEach { count ->
                        val isSelected = selectedPlayerCount == count
                        val gridLabel = "${count.gridSize}×${count.gridSize}"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) GameYellowVibrant else CellDarkBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GameYellowVibrant else CellDarkBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedPlayerCount = count }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("player_count_${count.count}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${count.count} PLAYERS",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "($gridLabel grid)",
                                    color = if (isSelected) Color.Black.copy(alpha = 0.75f) else TextLightSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }

                // If Single Player: AI Difficulty Selection
                if (isSinglePlayer) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT AI DIFFICULTY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = GameYellowVibrant,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) PlayerXBlue else CellDarkBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PlayerXBlue else CellDarkBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedDifficulty = diff }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("difficulty_${diff.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.title.uppercase(),
                                    color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sets Selection (5, 10, 15)
                Text(
                    text = "SELECT MATCH SETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = GameYellowVibrant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetSets.entries.forEach { target ->
                        val isSelected = selectedSets == target
                        val setsDesc = when (target) {
                            TargetSets.FIVE -> "First to 5 clear wins"
                            TargetSets.TEN -> "First to 10 clear wins"
                            TargetSets.FIFTEEN -> "First to 15 clear wins"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF27272A) else CellDarkBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GameYellowVibrant else CellDarkBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedSets = target }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("sets_${target.count}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = target.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) GameYellowVibrant else Color.White
                                )
                                Text(
                                    text = setsDesc,
                                    fontSize = 11.sp,
                                    color = TextLightSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GameYellowVibrant else Color.Transparent)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) GameYellowVibrant else Color(0xFF52525B),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BoardDark)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Start Game Button
                Button(
                    onClick = {
                        onStartGame(selectedPlayerCount, selectedDifficulty, selectedSets)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_game_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSinglePlayer) PlayerXBlue else PlayerORed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START MATCH",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
