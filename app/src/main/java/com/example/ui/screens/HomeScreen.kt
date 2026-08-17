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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GameMode
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.GameYellowBackground
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLightSecondary

@Composable
fun HomeScreen(
    playerName: String,
    onSelectMode: (GameMode) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameYellowBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with App Title, Player Profile Badge & Settings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANDROID EDITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "ENDLESS TTT",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp,
                        color = TextDark
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile Pill (Tappable to change name)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = playerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.08f))
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Center Branding & Main Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Board Showcase Preview Card
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .shadow(14.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(BoardDark)
                        .border(3.dp, Color(0xFF000000), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MiniCell("X", PlayerXBlue)
                            MiniCell("O", PlayerORed)
                            MiniCell("X", PlayerXBlue)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MiniCell("O", PlayerORed)
                            MiniCell("X", PlayerXBlue)
                            MiniCell("O", PlayerORed)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MiniCell("X", PlayerXBlue)
                            MiniCell("O", PlayerORed)
                            MiniCell("X", PlayerXBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "ENDLESS TIC TAC TOE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = TextDark,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "3-PIECE VANISHING STRATEGY • SET CHAMPIONSHIPS",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mode 1: Online Multiplayer (Cross-network)
                ModeButton(
                    title = "ONLINE MULTIPLAYER",
                    subtitle = "2, 3, or 4 Players • Room Codes & Host Acceptance",
                    icon = Icons.Default.Language,
                    accentColor = GameYellowVibrant,
                    testTag = "btn_online_multiplayer",
                    isFeatured = true,
                    onClick = { onSelectMode(GameMode.ONLINE_MULTIPLAYER) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode 2: Single Player (AI)
                ModeButton(
                    title = "SINGLE PLAYER (AI)",
                    subtitle = "2, 3, or 4 Players • Smart AI Opponents",
                    icon = Icons.Default.SmartToy,
                    accentColor = PlayerXBlue,
                    testTag = "btn_single_player",
                    onClick = { onSelectMode(GameMode.SINGLE_PLAYER) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode 3: Play with Friend (Pass & Play)
                ModeButton(
                    title = "PASS & PLAY (LOCAL)",
                    subtitle = "2, 3, or 4 Players on the same screen",
                    icon = Icons.Default.People,
                    accentColor = PlayerORed,
                    testTag = "btn_play_friend",
                    onClick = { onSelectMode(GameMode.FRIEND) }
                )
            }

            // Bottom Brand Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MULTI-DEVICE RELAY • REAL-TIME SYNC",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun MiniCell(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(CellDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    isFeatured: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(BoardDark)
            .border(
                width = if (isFeatured) 2.5.dp else 2.dp,
                color = if (isFeatured) GameYellowVibrant else Color(0xFF000000),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(2.dp, accentColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = TextLightSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GameYellowVibrant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
