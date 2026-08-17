package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.GameRecord
import com.example.data.network.WebSocketGameRelay
import com.example.data.repository.GameRepository
import com.example.domain.ai.EndlessAiEngine
import com.example.domain.model.ActiveReaction
import com.example.domain.model.AiDifficulty
import com.example.domain.model.BoardPosition
import com.example.domain.model.GameMode
import com.example.domain.model.LocalPlayerRole
import com.example.domain.model.MatchStats
import com.example.domain.model.NetworkConnectionStatus
import com.example.domain.model.PlayerSymbol
import com.example.domain.model.TargetSets
import com.example.domain.model.WinningLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppScreen {
    SPLASH,
    HOME,
    GAME
}

enum class RoundStatus {
    PLAYING,
    POINT_SCORED, // 1 point scored, showing winning line animation
    MATCH_OVER    // Someone reached target sets (5, 10, 15 wins)
}

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.SPLASH,
    val splashProgress: Float = 0f,
    val isSplashLoading: Boolean = true,

    // Profile & Settings
    val customPlayerName: String = "Player 1",
    val customRelayUrl: String = "",

    // Match Config
    val gameMode: GameMode = GameMode.SINGLE_PLAYER,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val targetSets: TargetSets = TargetSets.FIVE,
    val player1Name: String = "Player 1",
    val player2Name: String = "AI Opponent",

    // Online Multiplayer State
    val localPlayerRole: LocalPlayerRole = LocalPlayerRole.LOCAL_BOTH,
    val networkStatus: NetworkConnectionStatus = NetworkConnectionStatus.IDLE,
    val currentRoomCode: String = "",
    val isOnlineHost: Boolean = false,
    val onlineOpponentName: String = "",
    val onlineErrorMessage: String? = null,
    val showOnlineLobbyDialog: Boolean = false,
    val showOpponentLeftDialog: Boolean = false,
    val activeReactions: List<ActiveReaction> = emptyList(),

    // Current Game State
    val currentTurn: PlayerSymbol = PlayerSymbol.X,
    val player1Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
    val player2Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
    val matchStats: MatchStats = MatchStats(),
    val roundStatus: RoundStatus = RoundStatus.PLAYING,
    val winningLine: WinningLine? = null,
    val matchWinner: PlayerSymbol? = null,
    val isAiThinking: Boolean = false,

    // Dialogs & Sheets
    val showModeSelectDialog: Boolean = false,
    val selectedModeForDialog: GameMode = GameMode.SINGLE_PLAYER,
    val showSettingsDialog: Boolean = false,
    val showPauseDialog: Boolean = false,
    val showVictoryDialog: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val relay: WebSocketGameRelay = WebSocketGameRelay()

    val singlePlayerRecords: StateFlow<List<GameRecord>>
    val friendRecords: StateFlow<List<GameRecord>>
    val onlineRecords: StateFlow<List<GameRecord>>

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = GameRepository(database.gameDao(), application)

        singlePlayerRecords = repository.singlePlayerRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        friendRecords = repository.friendRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        onlineRecords = repository.onlineRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val savedName = repository.playerName.value
        val savedRelay = repository.customRelayUrl.value
        _uiState.value = _uiState.value.copy(
            customPlayerName = savedName,
            customRelayUrl = savedRelay,
            player1Name = savedName
        )

        setupRelayListeners()
        startSplashLoading()
    }

    private fun setupRelayListeners() {
        relay.onOpponentConnected = { oppName, oppTargetSets ->
            viewModelScope.launch {
                val isHost = relay.isHost.value
                val setsEnum = when (oppTargetSets) {
                    10 -> TargetSets.TEN
                    15 -> TargetSets.FIFTEEN
                    else -> _uiState.value.targetSets
                }

                val p1 = if (isHost) _uiState.value.customPlayerName else oppName
                val p2 = if (isHost) oppName else _uiState.value.customPlayerName

                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.GAME,
                    gameMode = GameMode.ONLINE_MULTIPLAYER,
                    localPlayerRole = if (isHost) LocalPlayerRole.HOST_X else LocalPlayerRole.GUEST_O,
                    networkStatus = NetworkConnectionStatus.CONNECTED,
                    onlineOpponentName = oppName,
                    targetSets = setsEnum,
                    player1Name = p1,
                    player2Name = p2,
                    currentTurn = PlayerSymbol.X,
                    player1Pieces = emptyList(),
                    player2Pieces = emptyList(),
                    activeReactions = emptyList(),
                    matchStats = MatchStats(
                        player1Wins = 0,
                        player2Wins = 0,
                        currentRound = 1,
                        targetSets = setsEnum.count
                    ),
                    roundStatus = RoundStatus.PLAYING,
                    winningLine = null,
                    matchWinner = null,
                    isAiThinking = false,
                    showOnlineLobbyDialog = false,
                    showModeSelectDialog = false,
                    showPauseDialog = false,
                    showVictoryDialog = false,
                    showOpponentLeftDialog = false
                )
            }
        }

        relay.onMoveReceived = { row, col, symbol ->
            viewModelScope.launch {
                val state = _uiState.value
                if (state.gameMode != GameMode.ONLINE_MULTIPLAYER) return@launch
                if (state.roundStatus != RoundStatus.PLAYING) return@launch

                val clickedPos = BoardPosition(row, col)
                if (symbol == PlayerSymbol.X) {
                    val updatedP1Pieces = EndlessAiEngine.simulateMove(state.player1Pieces, clickedPos)
                    processMoveResult(PlayerSymbol.X, updatedP1Pieces, state.player2Pieces, isNetworkMove = true)
                } else {
                    val updatedP2Pieces = EndlessAiEngine.simulateMove(state.player2Pieces, clickedPos)
                    processMoveResult(PlayerSymbol.O, state.player1Pieces, updatedP2Pieces, isNetworkMove = true)
                }
            }
        }

        relay.onReactionReceived = { emoji, senderName ->
            viewModelScope.launch {
                displayReaction(emoji = emoji, senderName = senderName, isLocal = false)
            }
        }

        relay.onRestartRoundReceived = {
            viewModelScope.launch {
                resetRoundLocally()
            }
        }

        relay.onRestartMatchReceived = {
            viewModelScope.launch {
                val state = _uiState.value
                _uiState.value = state.copy(
                    player1Pieces = emptyList(),
                    player2Pieces = emptyList(),
                    currentTurn = PlayerSymbol.X,
                    matchStats = MatchStats(
                        player1Wins = 0,
                        player2Wins = 0,
                        currentRound = 1,
                        targetSets = state.targetSets.count
                    ),
                    roundStatus = RoundStatus.PLAYING,
                    winningLine = null,
                    matchWinner = null,
                    showVictoryDialog = false
                )
            }
        }

        relay.onOpponentDisconnected = {
            viewModelScope.launch {
                if (_uiState.value.gameMode == GameMode.ONLINE_MULTIPLAYER && _uiState.value.currentScreen == AppScreen.GAME) {
                    _uiState.value = _uiState.value.copy(
                        networkStatus = NetworkConnectionStatus.DISCONNECTED,
                        showOpponentLeftDialog = true
                    )
                }
            }
        }

        relay.onErrorOccurred = { error ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    networkStatus = NetworkConnectionStatus.ERROR,
                    onlineErrorMessage = error
                )
            }
        }
    }

    private fun startSplashLoading() {
        viewModelScope.launch {
            for (step in 1..100) {
                delay(18)
                _uiState.value = _uiState.value.copy(splashProgress = step / 100f)
            }
            delay(200)
            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.HOME,
                isSplashLoading = false
            )
        }
    }

    fun setPlayerName(name: String) {
        val trimmed = name.trim().ifEmpty { "Player 1" }
        viewModelScope.launch {
            repository.setPlayerName(trimmed)
            _uiState.value = _uiState.value.copy(
                customPlayerName = trimmed,
                player1Name = trimmed
            )
        }
    }

    fun setCustomRelayUrl(url: String) {
        val trimmed = url.trim()
        viewModelScope.launch {
            repository.setCustomRelayUrl(trimmed)
            _uiState.value = _uiState.value.copy(customRelayUrl = trimmed)
        }
    }

    fun openModeSelect(mode: GameMode) {
        if (mode == GameMode.ONLINE_MULTIPLAYER) {
            val randomCode = generateRoomCode()
            _uiState.value = _uiState.value.copy(
                currentRoomCode = randomCode,
                showOnlineLobbyDialog = true,
                onlineErrorMessage = null,
                networkStatus = NetworkConnectionStatus.IDLE
            )
        } else {
            _uiState.value = _uiState.value.copy(
                selectedModeForDialog = mode,
                showModeSelectDialog = true
            )
        }
    }

    fun closeModeSelect() {
        _uiState.value = _uiState.value.copy(showModeSelectDialog = false)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun openPauseDialog() {
        _uiState.value = _uiState.value.copy(showPauseDialog = true)
    }

    fun closePauseDialog() {
        _uiState.value = _uiState.value.copy(showPauseDialog = false)
    }

    fun closeOpponentLeftDialog() {
        _uiState.value = _uiState.value.copy(
            showOpponentLeftDialog = false,
            currentScreen = AppScreen.HOME
        )
    }

    fun closeOnlineLobby() {
        relay.disconnect()
        _uiState.value = _uiState.value.copy(
            showOnlineLobbyDialog = false,
            networkStatus = NetworkConnectionStatus.IDLE,
            onlineErrorMessage = null
        )
    }

    fun cancelOnlineConnecting() {
        relay.disconnect()
        _uiState.value = _uiState.value.copy(
            networkStatus = NetworkConnectionStatus.IDLE,
            onlineErrorMessage = null
        )
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun createOnlineRoom(roomCode: String, targetSets: TargetSets) {
        val hostName = _uiState.value.customPlayerName.ifBlank { "Host" }
        _uiState.value = _uiState.value.copy(
            currentRoomCode = roomCode,
            isOnlineHost = true,
            targetSets = targetSets,
            networkStatus = NetworkConnectionStatus.CONNECTING,
            onlineErrorMessage = null
        )
        relay.createRoom(
            roomId = roomCode,
            hostName = hostName,
            targetSets = targetSets.count,
            customServerUrl = _uiState.value.customRelayUrl
        )
    }

    fun joinOnlineRoom(roomCode: String) {
        val guestName = _uiState.value.customPlayerName.ifBlank { "Guest" }
        _uiState.value = _uiState.value.copy(
            currentRoomCode = roomCode,
            isOnlineHost = false,
            networkStatus = NetworkConnectionStatus.CONNECTING,
            onlineErrorMessage = null
        )
        relay.joinRoom(
            roomId = roomCode,
            guestName = guestName,
            customServerUrl = _uiState.value.customRelayUrl
        )
    }

    private var lastReactionSentTimestamp: Long = 0L

    fun sendReaction(emoji: String) {
        if (_uiState.value.gameMode != GameMode.ONLINE_MULTIPLAYER) return
        val now = System.currentTimeMillis()
        if (now - lastReactionSentTimestamp < 3000L) {
            // Anti-spam cooldown active (3 seconds)
            return
        }
        lastReactionSentTimestamp = now
        relay.sendReaction(emoji)
        displayReaction(emoji = emoji, senderName = _uiState.value.customPlayerName, isLocal = true)
    }

    private fun displayReaction(emoji: String, senderName: String, isLocal: Boolean) {
        val reaction = ActiveReaction(
            emoji = emoji,
            senderName = senderName,
            isLocal = isLocal
        )
        // Keep active list clean by keeping only latest reaction per sender side
        val filtered = _uiState.value.activeReactions.filterNot { it.isLocal == isLocal }
        _uiState.value = _uiState.value.copy(
            activeReactions = filtered + reaction
        )

        // Auto remove reaction after 5 seconds
        viewModelScope.launch {
            delay(5000)
            _uiState.value = _uiState.value.copy(
                activeReactions = _uiState.value.activeReactions.filterNot { it.id == reaction.id }
            )
        }
    }

    fun startMatch(mode: GameMode, difficulty: AiDifficulty, targetSets: TargetSets) {
        val p1Name = _uiState.value.customPlayerName
        val p2Name = when (mode) {
            GameMode.SINGLE_PLAYER -> "AI (${difficulty.title})"
            GameMode.FRIEND -> "Friend"
            GameMode.ONLINE_MULTIPLAYER -> "Online Player"
        }

        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            gameMode = mode,
            aiDifficulty = difficulty,
            targetSets = targetSets,
            player1Name = p1Name,
            player2Name = p2Name,
            localPlayerRole = LocalPlayerRole.LOCAL_BOTH,
            currentTurn = PlayerSymbol.X,
            player1Pieces = emptyList(),
            player2Pieces = emptyList(),
            activeReactions = emptyList(),
            matchStats = MatchStats(
                player1Wins = 0,
                player2Wins = 0,
                currentRound = 1,
                targetSets = targetSets.count
            ),
            roundStatus = RoundStatus.PLAYING,
            winningLine = null,
            matchWinner = null,
            isAiThinking = false,
            showModeSelectDialog = false,
            showPauseDialog = false,
            showVictoryDialog = false,
            showOpponentLeftDialog = false
        )
    }

    fun onCellClicked(row: Int, col: Int) {
        val state = _uiState.value
        if (state.roundStatus != RoundStatus.PLAYING) return
        if (state.isAiThinking) return

        val clickedPos = BoardPosition(row, col)

        if (state.player1Pieces.contains(clickedPos) || state.player2Pieces.contains(clickedPos)) {
            return
        }

        // Check if user has right to move in Online mode
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            val isMyTurn = (state.localPlayerRole == LocalPlayerRole.HOST_X && state.currentTurn == PlayerSymbol.X) ||
                    (state.localPlayerRole == LocalPlayerRole.GUEST_O && state.currentTurn == PlayerSymbol.O)
            if (!isMyTurn) return
        }

        if (state.currentTurn == PlayerSymbol.X) {
            val updatedP1Pieces = EndlessAiEngine.simulateMove(state.player1Pieces, clickedPos)
            processMoveResult(PlayerSymbol.X, updatedP1Pieces, state.player2Pieces, isNetworkMove = false)

            if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                relay.sendMove(row, col, PlayerSymbol.X)
            }
        } else {
            val updatedP2Pieces = EndlessAiEngine.simulateMove(state.player2Pieces, clickedPos)
            processMoveResult(PlayerSymbol.O, state.player1Pieces, updatedP2Pieces, isNetworkMove = false)

            if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                relay.sendMove(row, col, PlayerSymbol.O)
            }
        }
    }

    private fun processMoveResult(
        movedPlayer: PlayerSymbol,
        newP1Pieces: List<BoardPosition>,
        newP2Pieces: List<BoardPosition>,
        isNetworkMove: Boolean = false
    ) {
        val winningPos = EndlessAiEngine.checkWin(if (movedPlayer == PlayerSymbol.X) newP1Pieces else newP2Pieces)

        if (winningPos != null) {
            handlePointScored(movedPlayer, winningPos, newP1Pieces, newP2Pieces)
        } else {
            val nextPlayer = if (movedPlayer == PlayerSymbol.X) PlayerSymbol.O else PlayerSymbol.X
            val isAiNext = (_uiState.value.gameMode == GameMode.SINGLE_PLAYER && nextPlayer == PlayerSymbol.O)

            _uiState.value = _uiState.value.copy(
                player1Pieces = newP1Pieces,
                player2Pieces = newP2Pieces,
                currentTurn = nextPlayer,
                isAiThinking = isAiNext
            )

            if (isAiNext) {
                triggerAiMove(newP1Pieces, newP2Pieces)
            }
        }
    }

    private fun triggerAiMove(humanPieces: List<BoardPosition>, aiPieces: List<BoardPosition>) {
        viewModelScope.launch {
            delay(500)
            val state = _uiState.value
            if (state.roundStatus != RoundStatus.PLAYING) return@launch

            val bestMove = EndlessAiEngine.getBestMove(
                difficulty = state.aiDifficulty,
                aiPieces = aiPieces,
                humanPieces = humanPieces
            )

            val updatedAiPieces = EndlessAiEngine.simulateMove(aiPieces, bestMove)
            processMoveResult(PlayerSymbol.O, humanPieces, updatedAiPieces)
        }
    }

    private fun handlePointScored(
        winner: PlayerSymbol,
        winningPositions: List<BoardPosition>,
        finalP1Pieces: List<BoardPosition>,
        finalP2Pieces: List<BoardPosition>
    ) {
        val currentStats = _uiState.value.matchStats
        val newP1Wins = if (winner == PlayerSymbol.X) currentStats.player1Wins + 1 else currentStats.player1Wins
        val newP2Wins = if (winner == PlayerSymbol.O) currentStats.player2Wins + 1 else currentStats.player2Wins

        val targetSets = _uiState.value.targetSets.count
        val isMatchWon = newP1Wins >= targetSets || newP2Wins >= targetSets

        val winningLine = WinningLine(winningPositions, winner)

        if (isMatchWon) {
            val matchWinner = if (newP1Wins >= targetSets) PlayerSymbol.X else PlayerSymbol.O
            _uiState.value = _uiState.value.copy(
                player1Pieces = finalP1Pieces,
                player2Pieces = finalP2Pieces,
                matchStats = currentStats.copy(player1Wins = newP1Wins, player2Wins = newP2Wins),
                roundStatus = RoundStatus.MATCH_OVER,
                winningLine = winningLine,
                matchWinner = matchWinner,
                showVictoryDialog = true,
                isAiThinking = false
            )

            saveGameRecord(newP1Wins, newP2Wins, matchWinner)
        } else {
            _uiState.value = _uiState.value.copy(
                player1Pieces = finalP1Pieces,
                player2Pieces = finalP2Pieces,
                matchStats = currentStats.copy(
                    player1Wins = newP1Wins,
                    player2Wins = newP2Wins
                ),
                roundStatus = RoundStatus.POINT_SCORED,
                winningLine = winningLine,
                isAiThinking = false
            )

            viewModelScope.launch {
                delay(1600)
                val state = _uiState.value
                if (state.roundStatus == RoundStatus.POINT_SCORED) {
                    val nextRoundNumber = state.matchStats.currentRound + 1
                    val nextStartingPlayer = if (nextRoundNumber % 2 == 1) PlayerSymbol.X else PlayerSymbol.O

                    _uiState.value = state.copy(
                        player1Pieces = emptyList(),
                        player2Pieces = emptyList(),
                        currentTurn = nextStartingPlayer,
                        roundStatus = RoundStatus.PLAYING,
                        winningLine = null,
                        matchStats = state.matchStats.copy(currentRound = nextRoundNumber),
                        isAiThinking = (nextStartingPlayer == PlayerSymbol.O && state.gameMode == GameMode.SINGLE_PLAYER)
                    )

                    if (nextStartingPlayer == PlayerSymbol.O && state.gameMode == GameMode.SINGLE_PLAYER) {
                        triggerAiMove(emptyList(), emptyList())
                    }
                }
            }
        }
    }

    private fun saveGameRecord(p1Score: Int, p2Score: Int, winner: PlayerSymbol) {
        viewModelScope.launch {
            val state = _uiState.value
            val winnerName = if (winner == PlayerSymbol.X) {
                state.player1Name
            } else {
                state.player2Name
            }

            val record = GameRecord(
                gameMode = state.gameMode.name,
                difficulty = if (state.gameMode == GameMode.SINGLE_PLAYER) state.aiDifficulty.name else null,
                player1Score = p1Score,
                player2Score = p2Score,
                targetSets = state.targetSets.count,
                winner = winnerName
            )
            repository.saveGameResult(record)
        }
    }

    fun restartCurrentRound() {
        val state = _uiState.value
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            relay.sendRestartRound()
        }
        resetRoundLocally()
    }

    private fun resetRoundLocally() {
        val state = _uiState.value
        _uiState.value = state.copy(
            player1Pieces = emptyList(),
            player2Pieces = emptyList(),
            currentTurn = PlayerSymbol.X,
            roundStatus = RoundStatus.PLAYING,
            winningLine = null,
            showPauseDialog = false
        )
    }

    fun restartCurrentMatch() {
        val state = _uiState.value
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            relay.sendRestartMatch()
            _uiState.value = state.copy(
                player1Pieces = emptyList(),
                player2Pieces = emptyList(),
                currentTurn = PlayerSymbol.X,
                activeReactions = emptyList(),
                matchStats = MatchStats(
                    player1Wins = 0,
                    player2Wins = 0,
                    currentRound = 1,
                    targetSets = state.targetSets.count
                ),
                roundStatus = RoundStatus.PLAYING,
                winningLine = null,
                matchWinner = null,
                showVictoryDialog = false
            )
        } else {
            startMatch(state.gameMode, state.aiDifficulty, state.targetSets)
        }
    }

    fun exitToHome() {
        if (_uiState.value.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            relay.disconnect()
        }
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.HOME,
            showPauseDialog = false,
            showVictoryDialog = false,
            showModeSelectDialog = false,
            showOnlineLobbyDialog = false,
            showOpponentLeftDialog = false,
            activeReactions = emptyList()
        )
    }

    fun resetSinglePlayerScores() {
        viewModelScope.launch {
            repository.resetSinglePlayerScores()
        }
    }

    fun resetFriendScores() {
        viewModelScope.launch {
            repository.resetFriendScores()
        }
    }

    fun resetOnlineScores() {
        viewModelScope.launch {
            repository.resetOnlineScores()
        }
    }

    override fun onCleared() {
        super.onCleared()
        relay.disconnect()
    }
}
