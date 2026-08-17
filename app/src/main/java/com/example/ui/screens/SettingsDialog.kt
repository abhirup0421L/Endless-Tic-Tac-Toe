package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.GameRecord
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.CellDarkBorder
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLightSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    playerName: String,
    onSavePlayerName: (String) -> Unit,
    customRelayUrl: String,
    onSaveCustomRelayUrl: (String) -> Unit,
    singlePlayerRecords: List<GameRecord>,
    friendRecords: List<GameRecord>,
    onlineRecords: List<GameRecord>,
    onResetSinglePlayerScores: () -> Unit,
    onResetFriendScores: () -> Unit,
    onResetOnlineScores: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Profile & Settings, 1: Scores, 2: How to Play, 3: About

    var showConfirmResetSinglePlayer by remember { mutableStateOf(false) }
    var showConfirmResetFriend by remember { mutableStateOf(false) }
    var showConfirmResetOnline by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BoardDark,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .border(2.dp, Color(0xFF27272A), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS & STATS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextLightSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CellDarkBg,
                    contentColor = GameYellowVibrant,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GameYellowVibrant,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "PROFILE",
                                fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (selectedTab == 0) GameYellowVibrant else TextLightSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "SCORES",
                                fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (selectedTab == 1) GameYellowVibrant else TextLightSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "RULES",
                                fontWeight = if (selectedTab == 2) FontWeight.Black else FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (selectedTab == 2) GameYellowVibrant else TextLightSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text(
                                "ABOUT",
                                fontWeight = if (selectedTab == 3) FontWeight.Black else FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (selectedTab == 3) GameYellowVibrant else TextLightSecondary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ProfileSettingsSection(
                            currentName = playerName,
                            onSaveName = onSavePlayerName,
                            currentRelayUrl = customRelayUrl,
                            onSaveRelayUrl = onSaveCustomRelayUrl
                        )
                        1 -> ScoresSection(
                            singlePlayerRecords = singlePlayerRecords,
                            friendRecords = friendRecords,
                            onlineRecords = onlineRecords,
                            onRefreshSinglePlayer = { showConfirmResetSinglePlayer = true },
                            onRefreshFriend = { showConfirmResetFriend = true },
                            onRefreshOnline = { showConfirmResetOnline = true }
                        )
                        2 -> HowToPlaySection()
                        3 -> AboutSection(
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Play Endless Tic Tac Toe!"
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out Endless Tic Tac Toe for Android! A fast-paced endless 3-piece vanishing strategy game with Online Multiplayer and Smart AI. Created by Abhirup Das."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Endless TTT"))
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for Single Player reset
    if (showConfirmResetSinglePlayer) {
        AlertDialog(
            onDismissRequest = { showConfirmResetSinglePlayer = false },
            title = { Text("Reset Single Player Scores?") },
            text = { Text("This will permanently clear all saved matches against AI.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSinglePlayerScores()
                        showConfirmResetSinglePlayer = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerORed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetSinglePlayer = false }) {
                    Text("Cancel", color = TextLightSecondary)
                }
            },
            containerColor = BoardDark,
            titleContentColor = Color.White,
            textContentColor = TextLightSecondary
        )
    }

    // Confirmation dialog for Friend reset
    if (showConfirmResetFriend) {
        AlertDialog(
            onDismissRequest = { showConfirmResetFriend = false },
            title = { Text("Reset Play with Friend Scores?") },
            text = { Text("This will permanently clear all saved 2-player match history.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetFriendScores()
                        showConfirmResetFriend = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerORed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetFriend = false }) {
                    Text("Cancel", color = TextLightSecondary)
                }
            },
            containerColor = BoardDark,
            titleContentColor = Color.White,
            textContentColor = TextLightSecondary
        )
    }

    // Confirmation dialog for Online reset
    if (showConfirmResetOnline) {
        AlertDialog(
            onDismissRequest = { showConfirmResetOnline = false },
            title = { Text("Reset Online Match Scores?") },
            text = { Text("This will permanently clear all saved online multiplayer match records.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetOnlineScores()
                        showConfirmResetOnline = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerORed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetOnline = false }) {
                    Text("Cancel", color = TextLightSecondary)
                }
            },
            containerColor = BoardDark,
            titleContentColor = Color.White,
            textContentColor = TextLightSecondary
        )
    }
}

@Composable
private fun ProfileSettingsSection(
    currentName: String,
    onSaveName: (String) -> Unit,
    currentRelayUrl: String,
    onSaveRelayUrl: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var relayUrlInput by remember { mutableStateOf(currentRelayUrl) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Player Profile Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CellDarkBg)
                    .border(1.5.dp, Color(0xFF3F3F46), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GameYellowVibrant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PLAYER NAME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Displayed across lobbies, matches & leaderboards",
                            fontSize = 10.sp,
                            color = TextLightSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 20) nameInput = it },
                    singleLine = true,
                    placeholder = { Text("Enter your player name", color = TextLightSecondary) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (nameInput.isNotBlank()) {
                                onSaveName(nameInput.trim())
                                Toast.makeText(context, "Player name updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedBorderColor = GameYellowVibrant,
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_input")
                )

                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (nameInput.isNotBlank()) {
                            onSaveName(nameInput.trim())
                            Toast.makeText(context, "Player name saved as '${nameInput.trim()}'", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_player_name_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameYellowVibrant,
                        contentColor = TextDark
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE PLAYER NAME", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                }
            }
        }

        // Multiplayer Network Relay Setting
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CellDarkBg)
                    .border(1.5.dp, Color(0xFF3F3F46), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PlayerXBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NETWORK RELAY SERVER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Default: Cloud WebSocket Relay (Pre-configured)",
                            fontSize = 10.sp,
                            color = TextLightSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = relayUrlInput,
                    onValueChange = { relayUrlInput = it },
                    singleLine = true,
                    placeholder = { Text("Optional: wss://custom-server.com/room/{ROOM_ID}", color = TextLightSecondary, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onSaveRelayUrl(relayUrlInput.trim())
                            Toast.makeText(context, "Relay settings saved!", Toast.LENGTH_SHORT).show()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF18181B),
                        unfocusedContainerColor = Color(0xFF18181B),
                        focusedBorderColor = PlayerXBlue,
                        unfocusedBorderColor = Color(0xFF3F3F46),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (relayUrlInput.isNotBlank()) {
                        Button(
                            onClick = {
                                relayUrlInput = ""
                                onSaveRelayUrl("")
                                Toast.makeText(context, "Reset to default cloud relay", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextLightSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46))
                        ) {
                            Text("USE DEFAULT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onSaveRelayUrl(relayUrlInput.trim())
                            Toast.makeText(context, "Relay settings saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PlayerXBlue, contentColor = Color.White)
                    ) {
                        Text("SAVE RELAY", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoresSection(
    singlePlayerRecords: List<GameRecord>,
    friendRecords: List<GameRecord>,
    onlineRecords: List<GameRecord>,
    onRefreshSinglePlayer: () -> Unit,
    onRefreshFriend: () -> Unit,
    onRefreshOnline: () -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: AI, 1: Friend, 2: Online

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 0) PlayerXBlue else CellDarkBg)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI (${singlePlayerRecords.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeSubTab == 0) Color.White else TextLightSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 1) PlayerORed else CellDarkBg)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Friend (${friendRecords.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeSubTab == 1) Color.White else TextLightSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeSubTab == 2) GameYellowVibrant else CellDarkBg)
                    .clickable { activeSubTab = 2 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Online (${onlineRecords.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeSubTab == 2) TextDark else TextLightSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == 0) {
            // Single Player Scores
            val totalGames = singlePlayerRecords.size
            val wins = singlePlayerRecords.count { it.winner != "AI" && !it.winner.contains("AI") }
            val losses = totalGames - wins
            val winRate = if (totalGames > 0) (wins * 100 / totalGames) else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI MATCH HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameYellowVibrant
                )

                OutlinedButton(
                    onClick = onRefreshSinglePlayer,
                    modifier = Modifier.height(30.dp).testTag("refresh_single_player_scores"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Single Player",
                        modifier = Modifier.size(12.dp),
                        tint = GameYellowVibrant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear AI", fontSize = 10.sp, color = GameYellowVibrant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellDarkBg)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ScoreStatItem("Matches", "$totalGames")
                ScoreStatItem("Wins", "$wins", PlayerXBlue)
                ScoreStatItem("Losses", "$losses", PlayerORed)
                ScoreStatItem("Win Rate", "$winRate%")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (singlePlayerRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Single Player matches recorded yet.\nPlay a match vs AI to track scores!",
                        color = TextLightSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(singlePlayerRecords) { record ->
                        ScoreRecordCard(record = record, modeTitle = "vs AI (${record.difficulty ?: "Normal"})")
                    }
                }
            }
        } else if (activeSubTab == 1) {
            // Play with Friend Scores
            val totalGames = friendRecords.size
            val p1Wins = friendRecords.count { it.winner.contains("1") || it.winner == "Player 1" }
            val p2Wins = totalGames - p1Wins

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PASS & PLAY HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameYellowVibrant
                )

                OutlinedButton(
                    onClick = onRefreshFriend,
                    modifier = Modifier.height(30.dp).testTag("refresh_friend_scores"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Friend Scores",
                        modifier = Modifier.size(12.dp),
                        tint = GameYellowVibrant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Friend", fontSize = 10.sp, color = GameYellowVibrant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellDarkBg)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ScoreStatItem("Matches", "$totalGames")
                ScoreStatItem("P1 Wins", "$p1Wins", PlayerXBlue)
                ScoreStatItem("P2 Wins", "$p2Wins", PlayerORed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (friendRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No 2-Player matches recorded yet.\nPlay with a friend to record your rivalry!",
                        color = TextLightSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(friendRecords) { record ->
                        ScoreRecordCard(record = record, modeTitle = "Local 2-Player")
                    }
                }
            }
        } else {
            // Online Multiplayer Scores
            val totalGames = onlineRecords.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONLINE MATCH HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameYellowVibrant
                )

                OutlinedButton(
                    onClick = onRefreshOnline,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Online",
                        modifier = Modifier.size(12.dp),
                        tint = GameYellowVibrant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Online", fontSize = 10.sp, color = GameYellowVibrant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (onlineRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No online multiplayer matches completed yet.\nHost or join an online room to play!",
                        color = TextLightSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(onlineRecords) { record ->
                        ScoreRecordCard(record = record, modeTitle = "Online Match")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreStatItem(label: String, value: String, valueColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(text = label, fontSize = 9.sp, color = TextLightSecondary)
    }
}

@Composable
private fun ScoreRecordCard(record: GameRecord, modeTitle: String) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(record.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CellDarkBg)
            .border(width = 1.dp, color = CellDarkBorder, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${record.winner} Won!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameYellowVibrant
                )
                Text(
                    text = "$modeTitle • ${record.targetSets} Sets • $dateStr",
                    fontSize = 10.sp,
                    color = TextLightSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF18181B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${record.player1Score} - ${record.player2Score}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HowToPlaySection() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            RuleCard(
                step = "1",
                title = "Endless 3-Piece Rule",
                description = "Each player can only have a maximum of 3 symbols on the board at a time. When you place your 4th move, your oldest (1st) piece instantly disappears!"
            )
        }
        item {
            RuleCard(
                step = "2",
                title = "Watch the Fading Piece",
                description = "When you have 3 pieces placed, your oldest piece will pulse with a 'NEXT OUT' tag, helping you plan your next strategic move."
            )
        }
        item {
            RuleCard(
                step = "3",
                title = "Scoring Points",
                description = "Form 3 symbols in a row (horizontal, vertical, or diagonal) to win 1 Point! After a point is scored, the board resets for the next round."
            )
        }
        item {
            RuleCard(
                step = "4",
                title = "Multiplayer Across Networks",
                description = "Host an Online Room, share your 5-letter Room Code, and play in real time with a friend on any network or Wi-Fi!"
            )
        }
    }
}

@Composable
private fun RuleCard(step: String, title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CellDarkBg)
            .border(1.dp, CellDarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(GameYellowVibrant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = TextDark
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextLightSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AboutSection(onShareClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CellDarkBg)
                    .border(1.dp, CellDarkBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GameYellowVibrant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Developer",
                                fontSize = 11.sp,
                                color = TextLightSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Abhirup Das",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Designed and developed Endless Tic Tac Toe with 3-piece vanishing strategy, Smart AI, local multiplayer, and Cross-Network Online Multiplayer.",
                        fontSize = 11.sp,
                        color = TextLightSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // App Version Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellDarkBg)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GameYellowVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Version",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "v1.2.0 (Live Reactions & Failover)",
                        fontSize = 11.sp,
                        color = GameYellowVibrant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Share Button at bottom
        Button(
            onClick = onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("share_game_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GameYellowVibrant,
                contentColor = TextDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Game",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SHARE THIS GAME",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
