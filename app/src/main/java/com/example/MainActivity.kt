package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.GameMode
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModeSelectDialog
import com.example.ui.screens.OnlineLobbyDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.EndlessTTTTheme
import com.example.ui.theme.GameYellowBackground
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EndlessTTTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GameYellowBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        EndlessTTTApp()
                    }
                }
            }
        }
    }
}

@Composable
fun EndlessTTTApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val singlePlayerRecords by viewModel.singlePlayerRecords.collectAsStateWithLifecycle()
    val friendRecords by viewModel.friendRecords.collectAsStateWithLifecycle()
    val onlineRecords by viewModel.onlineRecords.collectAsStateWithLifecycle()

    when (uiState.currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(progress = uiState.splashProgress)
        }

        AppScreen.HOME -> {
            HomeScreen(
                playerName = uiState.customPlayerName,
                onSelectMode = { mode ->
                    viewModel.openModeSelect(mode)
                },
                onOpenSettings = {
                    viewModel.openSettings()
                }
            )
        }

        AppScreen.GAME -> {
            GameScreen(
                player1Name = uiState.player1Name,
                player2Name = uiState.player2Name,
                player1Pieces = uiState.player1Pieces,
                player2Pieces = uiState.player2Pieces,
                currentTurn = uiState.currentTurn,
                matchStats = uiState.matchStats,
                roundStatus = uiState.roundStatus,
                winningLine = uiState.winningLine,
                matchWinner = uiState.matchWinner,
                gameMode = uiState.gameMode,
                localPlayerRole = uiState.localPlayerRole,
                networkStatus = uiState.networkStatus,
                activeReactions = uiState.activeReactions,
                isAiThinking = uiState.isAiThinking,
                showPauseDialog = uiState.showPauseDialog,
                showVictoryDialog = uiState.showVictoryDialog,
                showOpponentLeftDialog = uiState.showOpponentLeftDialog,
                onCellClick = { row, col ->
                    viewModel.onCellClicked(row, col)
                },
                onSendReaction = { emoji ->
                    viewModel.sendReaction(emoji)
                },
                onPauseClick = {
                    viewModel.openPauseDialog()
                },
                onResumeClick = {
                    viewModel.closePauseDialog()
                },
                onRestartRoundClick = {
                    viewModel.restartCurrentRound()
                },
                onRestartMatchClick = {
                    viewModel.restartCurrentMatch()
                },
                onExitToHomeClick = {
                    viewModel.exitToHome()
                },
                onOpenSettings = {
                    viewModel.openSettings()
                },
                onCloseOpponentLeftDialog = {
                    viewModel.closeOpponentLeftDialog()
                }
            )
        }
    }

    // Mode Selection Dialog (for AI & Local Friend)
    if (uiState.showModeSelectDialog) {
        ModeSelectDialog(
            gameMode = uiState.selectedModeForDialog,
            onDismiss = { viewModel.closeModeSelect() },
            onStartGame = { difficulty, targetSets ->
                viewModel.startMatch(
                    mode = uiState.selectedModeForDialog,
                    difficulty = difficulty,
                    targetSets = targetSets
                )
            }
        )
    }

    // Online Multiplayer Lobby Dialog (for Host & Join across separate devices)
    if (uiState.showOnlineLobbyDialog) {
        OnlineLobbyDialog(
            initialRoomCode = uiState.currentRoomCode,
            playerName = uiState.customPlayerName,
            networkStatus = uiState.networkStatus,
            errorMessage = uiState.onlineErrorMessage,
            onCreateRoom = { code, targetSets ->
                viewModel.createOnlineRoom(code, targetSets)
            },
            onJoinRoom = { roomCode ->
                viewModel.joinOnlineRoom(roomCode)
            },
            onCancelConnecting = {
                viewModel.cancelOnlineConnecting()
            },
            onDismiss = {
                viewModel.closeOnlineLobby()
            },
            onOpenSettings = {
                viewModel.openSettings()
            }
        )
    }

    // Settings & Scores Dialog (Includes Player Name edit & network relay settings)
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            playerName = uiState.customPlayerName,
            onSavePlayerName = { newName ->
                viewModel.setPlayerName(newName)
            },
            customRelayUrl = uiState.customRelayUrl,
            onSaveCustomRelayUrl = { newUrl ->
                viewModel.setCustomRelayUrl(newUrl)
            },
            singlePlayerRecords = singlePlayerRecords,
            friendRecords = friendRecords,
            onlineRecords = onlineRecords,
            onResetSinglePlayerScores = {
                viewModel.resetSinglePlayerScores()
            },
            onResetFriendScores = {
                viewModel.resetFriendScores()
            },
            onResetOnlineScores = {
                viewModel.resetOnlineScores()
            },
            onDismiss = {
                viewModel.closeSettings()
            }
        )
    }
}
