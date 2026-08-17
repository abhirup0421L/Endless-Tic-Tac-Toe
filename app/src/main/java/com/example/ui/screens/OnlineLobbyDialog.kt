package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.NetworkConnectionStatus
import com.example.domain.model.PendingJoinUser
import com.example.domain.model.PlayerCount
import com.example.domain.model.TargetSets
import com.example.ui.theme.BoardDark
import com.example.ui.theme.CellDarkBg
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerTickGreen
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLightSecondary

@Composable
fun OnlineLobbyDialog(
    initialRoomCode: String,
    playerName: String,
    isOnlineHost: Boolean = false,
    networkStatus: NetworkConnectionStatus,
    errorMessage: String?,
    roomPlayerCount: PlayerCount = PlayerCount.TWO,
    pendingJoinRequests: List<PendingJoinUser> = emptyList(),
    connectedPlayers: List<String> = emptyList(),
    onAcceptPlayerRequest: (id: String, name: String) -> Unit = { _, _ -> },
    onRejectPlayerRequest: (id: String) -> Unit = {},
    onStartMatchNow: () -> Unit = {},
    onCreateRoom: (roomCode: String, playerCount: PlayerCount, targetSets: TargetSets) -> Unit,
    onJoinRoom: (roomCode: String) -> Unit,
    onCancelConnecting: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (isOnlineHost) 0 else 0) } // 0 = Create Room, 1 = Join Room
    var joinCodeInput by remember { mutableStateOf("") }
    var selectedPlayerCount by remember { mutableStateOf(PlayerCount.TWO) }
    var selectedTargetSets by remember { mutableStateOf(TargetSets.FIVE) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isConnecting = networkStatus == NetworkConnectionStatus.CONNECTING ||
            networkStatus == NetworkConnectionStatus.WAITING_FOR_OPPONENT ||
            networkStatus == NetworkConnectionStatus.CONNECTED

    Dialog(onDismissRequest = {
        if (!isConnecting) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BoardDark,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(2.5.dp, Color(0xFF27272A), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GameYellowVibrant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ONLINE MULTIPLAYER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Playing as $playerName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameYellowVibrant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isConnecting) onCancelConnecting()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If connecting / waiting
                if (isConnecting) {
                    val activeRoomCode = if (isOnlineHost) {
                        initialRoomCode
                    } else {
                        joinCodeInput.uppercase().trim().ifBlank { initialRoomCode }
                    }
                    ConnectingStateView(
                        isHost = isOnlineHost,
                        roomCode = activeRoomCode,
                        status = networkStatus,
                        playerCount = roomPlayerCount,
                        pendingRequests = pendingJoinRequests,
                        connectedPlayers = connectedPlayers,
                        onAcceptPlayer = onAcceptPlayerRequest,
                        onRejectPlayer = onRejectPlayerRequest,
                        onStartMatchNow = onStartMatchNow,
                        onCancel = onCancelConnecting
                    )
                } else {
                    // Mode Tabs: Create Room / Join Room
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Black.copy(alpha = 0.4f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GameYellowVibrant,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "CREATE ROOM",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
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
                                    text = "JOIN ROOM",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (selectedTab == 1) GameYellowVibrant else TextLightSecondary
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // CREATE ROOM TAB
                        CreateRoomTabContent(
                            roomCode = initialRoomCode,
                            selectedPlayerCount = selectedPlayerCount,
                            onPlayerCountChange = { selectedPlayerCount = it },
                            selectedTargetSets = selectedTargetSets,
                            onTargetSetsChange = { selectedTargetSets = it },
                            onCopyCode = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Room Code", initialRoomCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Room code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onCreateClick = {
                                onCreateRoom(initialRoomCode, selectedPlayerCount, selectedTargetSets)
                            }
                        )
                    } else {
                        // JOIN ROOM TAB
                        JoinRoomTabContent(
                            joinCodeInput = joinCodeInput,
                            onJoinCodeChange = { joinCodeInput = it },
                            onJoinClick = {
                                keyboardController?.hide()
                                onJoinRoom(joinCodeInput)
                            }
                        )
                    }

                    if (!errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = PlayerORed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomTabContent(
    roomCode: String,
    selectedPlayerCount: PlayerCount,
    onPlayerCountChange: (PlayerCount) -> Unit,
    selectedTargetSets: TargetSets,
    onTargetSetsChange: (TargetSets) -> Unit,
    onCopyCode: () -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Room Code Display Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CellDarkBg)
                .border(1.5.dp, Color(0xFF3F3F46), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR ROOM CODE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = TextLightSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = roomCode,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = GameYellowVibrant
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onCopyCode,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Share this code with players on any device/network",
                fontSize = 10.sp,
                color = TextLightSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Player Count Selection
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "NUMBER OF PLAYERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = GameYellowVibrant,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerCount.entries.forEach { count ->
                    val isSelected = selectedPlayerCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GameYellowVibrant else CellDarkBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GameYellowVibrant else Color(0xFF3F3F46),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onPlayerCountChange(count) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${count.count} PLAYERS",
                                color = if (isSelected) Color.Black else Color(0xFFD4D4D8),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "(${count.gridSize}×${count.gridSize})",
                                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else TextLightSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            if (selectedPlayerCount.count > 2) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "★ Host can review and accept join requests before match starts",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameYellowVibrant.copy(alpha = 0.85f)
                )
            }
        }

        // Match Sets Selection
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SELECT MATCH SETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = GameYellowVibrant,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetSets.entries.forEach { sets ->
                    val isSelected = selectedTargetSets == sets
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PlayerXBlue else CellDarkBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF3F3F46),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onTargetSetsChange(sets) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sets.label.uppercase(),
                            color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Host Button
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_room_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GameYellowVibrant,
                contentColor = TextDark
            )
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CREATE ROOM & WAIT",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun JoinRoomTabContent(
    joinCodeInput: String,
    onJoinCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ENTER 5-CHARACTER ROOM CODE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = GameYellowVibrant,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        OutlinedTextField(
            value = joinCodeInput,
            onValueChange = { if (it.length <= 8) onJoinCodeChange(it.uppercase().filter { c -> c.isLetterOrDigit() }) },
            placeholder = { Text("e.g. 7K9X2", color = TextLightSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onJoinClick() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CellDarkBg,
                unfocusedContainerColor = CellDarkBg,
                focusedBorderColor = GameYellowVibrant,
                unfocusedBorderColor = Color(0xFF3F3F46),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("room_code_input")
        )

        Button(
            onClick = onJoinClick,
            enabled = joinCodeInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("join_room_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlayerXBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CONNECT & PLAY",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ConnectingStateView(
    isHost: Boolean,
    roomCode: String,
    status: NetworkConnectionStatus,
    playerCount: PlayerCount = PlayerCount.TWO,
    pendingRequests: List<PendingJoinUser> = emptyList(),
    connectedPlayers: List<String> = emptyList(),
    onAcceptPlayer: (id: String, name: String) -> Unit = { _, _ -> },
    onRejectPlayer: (id: String) -> Unit = {},
    onStartMatchNow: () -> Unit = {},
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            color = GameYellowVibrant,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(44.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isHost) "LOBBY: WAITING FOR PLAYERS" else "CONNECTING TO ROOM...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Room Code: $roomCode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = GameYellowVibrant,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isHost)
                    "Mode: ${playerCount.count} Players (${playerCount.gridSize}×${playerCount.gridSize} grid)"
                else
                    "Waiting for host to admit and launch match...",
                fontSize = 11.sp,
                color = TextLightSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Joined Players Section
        if (connectedPlayers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellDarkBg)
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PLAYERS IN ROOM (${connectedPlayers.size}/${playerCount.count})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = GameYellowVibrant,
                    letterSpacing = 1.sp
                )

                connectedPlayers.forEach { pName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF27272A))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GameYellowVibrant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Host Player Join Requests section (for 3 or 4 players mode)
        if (isHost && playerCount.count > 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellDarkBg)
                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "JOINING REQUESTS (${pendingRequests.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = GameYellowVibrant,
                    letterSpacing = 1.sp
                )

                if (pendingRequests.isEmpty()) {
                    Text(
                        text = "Waiting for players to enter room code...",
                        fontSize = 11.sp,
                        color = TextLightSecondary
                    )
                } else {
                    pendingRequests.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF27272A))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onAcceptPlayer(user.id, user.name) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PlayerTickGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Accept",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onRejectPlayer(user.id) },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PlayerORed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Reject",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = PlayerORed
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PlayerORed),
            modifier = Modifier.height(40.dp)
        ) {
            Text("CANCEL", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
        }
    }
}
