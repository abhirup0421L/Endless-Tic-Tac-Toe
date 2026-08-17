package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.GameRecord
import com.example.data.network.RoomStatePayload
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
import com.example.domain.model.PendingJoinUser
import com.example.domain.model.PlayerCount
import com.example.domain.model.PlayerSymbol
import com.example.domain.model.TargetSets
import com.example.domain.model.WinningLine
import com.example.ui.sound.SoundEffect
import com.example.ui.sound.SoundManager
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
    ONBOARDING,
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
    val isSoundEnabled: Boolean = true,

    // Match Config
    val gameMode: GameMode = GameMode.SINGLE_PLAYER,
    val playerCount: PlayerCount = PlayerCount.TWO,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val targetSets: TargetSets = TargetSets.FIVE,
    val player1Name: String = "Player 1",
    val player2Name: String = "AI Opponent",
    val player3Name: String = "Player 3",
    val player4Name: String = "Player 4",

    // Online Multiplayer State
    val localPlayerRole: LocalPlayerRole = LocalPlayerRole.LOCAL_ALL,
    val networkStatus: NetworkConnectionStatus = NetworkConnectionStatus.IDLE,
    val currentRoomCode: String = "",
    val isOnlineHost: Boolean = false,
    val onlineOpponentName: String = "",
    val connectedPlayers: List<String> = emptyList(), // Names of all players currently in room
    val pendingJoinRequests: List<PendingJoinUser> = emptyList(), // Players requesting to join host room (3/4P)
    val onlineErrorMessage: String? = null,
    val showOnlineLobbyDialog: Boolean = false,
    val showOpponentLeftDialog: Boolean = false,
    val activeReactions: List<ActiveReaction> = emptyList(),

    // Current Game State
    val currentTurn: PlayerSymbol = PlayerSymbol.X,
    val player1Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
    val player2Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
    val player3Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
    val player4Pieces: List<BoardPosition> = emptyList(), // Size <= 3, FIFO
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
    private val soundManager: SoundManager = SoundManager.getInstance(application)

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
            player1Name = savedName,
            isSoundEnabled = soundManager.isSoundEnabled.value
        )

        // Observe Sound setting changes
        viewModelScope.launch {
            soundManager.isSoundEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isSoundEnabled = enabled)
            }
        }

        setupRelayListeners()
        startSplashLoading()
    }

    private fun setupRelayListeners() {
        relay.onRoomStateUpdated = { roomState ->
            viewModelScope.launch {
                handleRoomStateUpdate(roomState)
            }
        }

        relay.onJoinRequestReceived = { guestId, guestName ->
            viewModelScope.launch {
                val current = _uiState.value.pendingJoinRequests.toMutableList()
                if (current.none { it.id == guestId }) {
                    current.add(PendingJoinUser(guestId, guestName))
                    _uiState.value = _uiState.value.copy(pendingJoinRequests = current)
                    soundManager.playSound(SoundEffect.BUTTON_CLICK)
                }
            }
        }

        relay.onMatchStarted = { roomState ->
            viewModelScope.launch {
                val state = _uiState.value
                if (state.currentScreen == AppScreen.GAME && state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
                    return@launch
                }
                handleMatchStarted(roomState)
            }
        }

        relay.onMoveReceived = { row, col, symbol ->
            viewModelScope.launch {
                val state = _uiState.value
                if (state.gameMode != GameMode.ONLINE_MULTIPLAYER) return@launch
                if (state.roundStatus != RoundStatus.PLAYING) return@launch

                val clickedPos = BoardPosition(row, col)
                val currentPieces = state.getPiecesForSymbol(symbol)
                val willVanish = currentPieces.size == 3
                val updatedPieces = EndlessAiEngine.simulateMove(currentPieces, clickedPos)
                playPieceSound(symbol, willVanish)
                processMoveResult(symbol, updatedPieces, isNetworkMove = true)
            }
        }

        relay.onReactionReceived = { emoji, senderName, symbol ->
            viewModelScope.launch {
                displayReaction(emoji = emoji, senderName = senderName, isLocal = false, symbol = symbol)
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
                    player3Pieces = emptyList(),
                    player4Pieces = emptyList(),
                    currentTurn = PlayerSymbol.X,
                    matchStats = MatchStats(
                        player1Wins = 0,
                        player2Wins = 0,
                        player3Wins = 0,
                        player4Wins = 0,
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

        relay.onPlayerLeft = { _ ->
            viewModelScope.launch {
                if (_uiState.value.gameMode == GameMode.ONLINE_MULTIPLAYER && _uiState.value.currentScreen == AppScreen.GAME) {
                    _uiState.value = _uiState.value.copy(
                        networkStatus = NetworkConnectionStatus.DISCONNECTED,
                        showOpponentLeftDialog = true
                    )
                }
            }
        }

        relay.onErrorOccurred = { errorMsg ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    onlineErrorMessage = errorMsg,
                    networkStatus = NetworkConnectionStatus.ERROR
                )
            }
        }
    }

    private fun handleRoomStateUpdate(roomState: RoomStatePayload) {
        val isHost = relay.isHost.value
        val setsEnum = TargetSets.entries.find { it.count == roomState.targetSets } ?: TargetSets.FIVE

        val myPlayer = roomState.players.find { it.id == relay.clientId }
        val myRole = when (myPlayer?.symbol) {
            PlayerSymbol.X -> LocalPlayerRole.HOST_X
            PlayerSymbol.O -> LocalPlayerRole.GUEST_O
            PlayerSymbol.TICK -> LocalPlayerRole.GUEST_TICK
            PlayerSymbol.TRIANGLE -> LocalPlayerRole.GUEST_TRIANGLE
            null -> if (isHost) LocalPlayerRole.HOST_X else LocalPlayerRole.GUEST_O
        }

        val p1 = roomState.players.getOrNull(0)?.name ?: "Player 1"
        val p2 = roomState.players.getOrNull(1)?.name ?: "Player 2"
        val p3 = roomState.players.getOrNull(2)?.name ?: "Player 3"
        val p4 = roomState.players.getOrNull(3)?.name ?: "Player 4"

        val connectedNames = roomState.players.map { "${it.name} (${it.symbol.displayName})" }

        // Remove any pending join requests for players that are now admitted
        val remainingPending = _uiState.value.pendingJoinRequests.filterNot { req ->
            roomState.players.any { it.id == req.id }
        }

        _uiState.value = _uiState.value.copy(
            playerCount = roomState.playerCount,
            targetSets = setsEnum,
            localPlayerRole = myRole,
            connectedPlayers = connectedNames,
            pendingJoinRequests = remainingPending,
            player1Name = p1,
            player2Name = p2,
            player3Name = p3,
            player4Name = p4,
            networkStatus = if (myPlayer != null || isHost) NetworkConnectionStatus.CONNECTED else NetworkConnectionStatus.CONNECTING
        )
    }

    private fun handleMatchStarted(roomState: RoomStatePayload) {
        val isHost = relay.isHost.value
        val setsEnum = TargetSets.entries.find { it.count == roomState.targetSets } ?: TargetSets.FIVE

        val myPlayer = roomState.players.find { it.id == relay.clientId }
        val myRole = when (myPlayer?.symbol) {
            PlayerSymbol.X -> LocalPlayerRole.HOST_X
            PlayerSymbol.O -> LocalPlayerRole.GUEST_O
            PlayerSymbol.TICK -> LocalPlayerRole.GUEST_TICK
            PlayerSymbol.TRIANGLE -> LocalPlayerRole.GUEST_TRIANGLE
            null -> if (isHost) LocalPlayerRole.HOST_X else LocalPlayerRole.GUEST_O
        }

        val p1 = roomState.players.getOrNull(0)?.name ?: "Player 1"
        val p2 = roomState.players.getOrNull(1)?.name ?: "Player 2"
        val p3 = roomState.players.getOrNull(2)?.name ?: "Player 3"
        val p4 = roomState.players.getOrNull(3)?.name ?: "Player 4"

        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            gameMode = GameMode.ONLINE_MULTIPLAYER,
            playerCount = roomState.playerCount,
            targetSets = setsEnum,
            localPlayerRole = myRole,
            networkStatus = NetworkConnectionStatus.CONNECTED,
            player1Name = p1,
            player2Name = p2,
            player3Name = p3,
            player4Name = p4,
            currentTurn = PlayerSymbol.X,
            player1Pieces = emptyList(),
            player2Pieces = emptyList(),
            player3Pieces = emptyList(),
            player4Pieces = emptyList(),
            activeReactions = emptyList(),
            matchStats = MatchStats(
                player1Wins = 0,
                player2Wins = 0,
                player3Wins = 0,
                player4Wins = 0,
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

    private fun startSplashLoading() {
        viewModelScope.launch {
            val totalSteps = 25
            for (i in 1..totalSteps) {
                delay(30)
                _uiState.value = _uiState.value.copy(
                    splashProgress = i / totalSteps.toFloat()
                )
            }
            delay(150)
            _uiState.value = _uiState.value.copy(
                isSplashLoading = false,
                currentScreen = AppScreen.HOME
            )
        }
    }

    fun setPlayerName(name: String) {
        val trimmed = name.trim().take(16)
        if (trimmed.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                customPlayerName = trimmed,
                player1Name = trimmed
            )
            viewModelScope.launch {
                repository.setPlayerName(trimmed)
            }
        }
    }

    fun setCustomRelayUrl(url: String) {
        val trimmed = url.trim()
        _uiState.value = _uiState.value.copy(customRelayUrl = trimmed)
        viewModelScope.launch {
            repository.setCustomRelayUrl(trimmed)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundManager.setSoundEnabled(enabled)
    }

    fun openModeSelect(mode: GameMode) {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        if (mode == GameMode.ONLINE_MULTIPLAYER) {
            val code = generateRoomCode()
            _uiState.value = _uiState.value.copy(
                selectedModeForDialog = mode,
                currentRoomCode = code,
                showOnlineLobbyDialog = true,
                onlineErrorMessage = null,
                connectedPlayers = emptyList(),
                pendingJoinRequests = emptyList(),
                networkStatus = NetworkConnectionStatus.IDLE
            )
        } else {
            _uiState.value = _uiState.value.copy(
                selectedModeForDialog = mode,
                showModeSelectDialog = true
            )
        }
    }

    fun dismissModeSelect() {
        _uiState.value = _uiState.value.copy(showModeSelectDialog = false)
    }

    fun openOnlineLobby() {
        val code = generateRoomCode()
        _uiState.value = _uiState.value.copy(
            showOnlineLobbyDialog = true,
            currentRoomCode = code,
            onlineErrorMessage = null,
            connectedPlayers = emptyList(),
            pendingJoinRequests = emptyList(),
            networkStatus = NetworkConnectionStatus.IDLE
        )
    }

    fun dismissOnlineLobby() {
        if (_uiState.value.networkStatus != NetworkConnectionStatus.CONNECTING &&
            _uiState.value.networkStatus != NetworkConnectionStatus.WAITING_FOR_OPPONENT) {
            _uiState.value = _uiState.value.copy(showOnlineLobbyDialog = false)
        }
    }

    fun cancelConnectingOnline() {
        relay.disconnect()
        _uiState.value = _uiState.value.copy(
            networkStatus = NetworkConnectionStatus.IDLE,
            showOnlineLobbyDialog = false,
            pendingJoinRequests = emptyList(),
            connectedPlayers = emptyList()
        )
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun createOnlineRoom(roomCode: String, playerCount: PlayerCount = PlayerCount.TWO, targetSets: TargetSets = TargetSets.FIVE) {
        val hostName = _uiState.value.customPlayerName.ifBlank { "Host" }
        _uiState.value = _uiState.value.copy(
            currentRoomCode = roomCode,
            isOnlineHost = true,
            playerCount = playerCount,
            targetSets = targetSets,
            networkStatus = NetworkConnectionStatus.WAITING_FOR_OPPONENT,
            onlineErrorMessage = null,
            connectedPlayers = listOf("$hostName (X)"),
            pendingJoinRequests = emptyList()
        )
        relay.createRoom(
            roomId = roomCode,
            hostName = hostName,
            playerCount = playerCount,
            targetSets = targetSets.count,
            customServerUrl = _uiState.value.customRelayUrl
        )
    }

    fun acceptPlayerRequest(guestId: String, guestName: String) {
        val currentPending = _uiState.value.pendingJoinRequests.filterNot { it.id == guestId }
        _uiState.value = _uiState.value.copy(pendingJoinRequests = currentPending)
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        relay.hostAcceptPlayer(guestId, guestName)
    }

    fun rejectPlayerRequest(guestId: String) {
        val currentPending = _uiState.value.pendingJoinRequests.filterNot { it.id == guestId }
        _uiState.value = _uiState.value.copy(pendingJoinRequests = currentPending)
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
    }

    fun hostStartMatchNow() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        relay.hostStartMatchNow()
    }

    fun joinOnlineRoom(roomCode: String) {
        val guestName = _uiState.value.customPlayerName.ifBlank { "Guest" }
        _uiState.value = _uiState.value.copy(
            currentRoomCode = roomCode,
            isOnlineHost = false,
            networkStatus = NetworkConnectionStatus.CONNECTING,
            onlineErrorMessage = null,
            connectedPlayers = emptyList(),
            pendingJoinRequests = emptyList()
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
        if (now - lastReactionSentTimestamp < 2500L) {
            // Cooldown active
            return
        }
        lastReactionSentTimestamp = now
        val mySymbol = when (_uiState.value.localPlayerRole) {
            LocalPlayerRole.HOST_X -> PlayerSymbol.X
            LocalPlayerRole.GUEST_O -> PlayerSymbol.O
            LocalPlayerRole.GUEST_TICK -> PlayerSymbol.TICK
            LocalPlayerRole.GUEST_TRIANGLE -> PlayerSymbol.TRIANGLE
            LocalPlayerRole.LOCAL_ALL -> _uiState.value.currentTurn
        }
        relay.sendReaction(emoji, mySymbol)
        displayReaction(emoji = emoji, senderName = _uiState.value.customPlayerName, isLocal = true, symbol = mySymbol)
    }

    private fun displayReaction(emoji: String, senderName: String, isLocal: Boolean, symbol: PlayerSymbol = PlayerSymbol.X) {
        soundManager.playSound(SoundEffect.REACTION_POP)
        val reaction = ActiveReaction(
            emoji = emoji,
            senderName = senderName,
            isLocal = isLocal,
            playerSymbol = symbol
        )
        val filtered = _uiState.value.activeReactions.filterNot { it.playerSymbol == symbol }
        _uiState.value = _uiState.value.copy(
            activeReactions = filtered + reaction
        )

        viewModelScope.launch {
            delay(4000)
            _uiState.value = _uiState.value.copy(
                activeReactions = _uiState.value.activeReactions.filterNot { it.id == reaction.id }
            )
        }
    }

    fun startMatch(mode: GameMode, playerCount: PlayerCount, difficulty: AiDifficulty, targetSets: TargetSets) {
        val p1Name = _uiState.value.customPlayerName
        val p2Name = when (mode) {
            GameMode.SINGLE_PLAYER -> "AI 1 (${difficulty.title})"
            GameMode.FRIEND -> "Player O"
            GameMode.ONLINE_MULTIPLAYER -> "Online Player O"
        }
        val p3Name = when (mode) {
            GameMode.SINGLE_PLAYER -> "AI 2 (${difficulty.title})"
            GameMode.FRIEND -> "Player ✓"
            GameMode.ONLINE_MULTIPLAYER -> "Online Player ✓"
        }
        val p4Name = when (mode) {
            GameMode.SINGLE_PLAYER -> "AI 3 (${difficulty.title})"
            GameMode.FRIEND -> "Player ▲"
            GameMode.ONLINE_MULTIPLAYER -> "Online Player ▲"
        }

        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            gameMode = mode,
            playerCount = playerCount,
            aiDifficulty = difficulty,
            targetSets = targetSets,
            player1Name = p1Name,
            player2Name = p2Name,
            player3Name = p3Name,
            player4Name = p4Name,
            localPlayerRole = LocalPlayerRole.LOCAL_ALL,
            currentTurn = PlayerSymbol.X,
            player1Pieces = emptyList(),
            player2Pieces = emptyList(),
            player3Pieces = emptyList(),
            player4Pieces = emptyList(),
            activeReactions = emptyList(),
            matchStats = MatchStats(
                player1Wins = 0,
                player2Wins = 0,
                player3Wins = 0,
                player4Wins = 0,
                currentRound = 1,
                targetSets = targetSets.count
            ),
            roundStatus = RoundStatus.PLAYING,
            winningLine = null,
            matchWinner = null,
            isAiThinking = false,
            showModeSelectDialog = false,
            showOnlineLobbyDialog = false,
            showPauseDialog = false,
            showVictoryDialog = false,
            showOpponentLeftDialog = false
        )
    }

    fun openSettings() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun dismissSettings() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun openPauseDialog() {
        soundManager.playSound(SoundEffect.BUTTON_CLICK)
        _uiState.value = _uiState.value.copy(showPauseDialog = true)
    }

    fun dismissPauseDialog() {
        _uiState.value = _uiState.value.copy(showPauseDialog = false)
    }

    fun dismissVictoryDialog() {
        _uiState.value = _uiState.value.copy(showVictoryDialog = false)
    }

    fun dismissOpponentLeftDialog() {
        _uiState.value = _uiState.value.copy(showOpponentLeftDialog = false)
        exitToHome()
    }

    fun GameUiState.getPiecesForSymbol(symbol: PlayerSymbol): List<BoardPosition> {
        return when (symbol) {
            PlayerSymbol.X -> player1Pieces
            PlayerSymbol.O -> player2Pieces
            PlayerSymbol.TICK -> player3Pieces
            PlayerSymbol.TRIANGLE -> player4Pieces
        }
    }

    private fun GameUiState.withUpdatedPieces(symbol: PlayerSymbol, pieces: List<BoardPosition>): GameUiState {
        return when (symbol) {
            PlayerSymbol.X -> copy(player1Pieces = pieces)
            PlayerSymbol.O -> copy(player2Pieces = pieces)
            PlayerSymbol.TICK -> copy(player3Pieces = pieces)
            PlayerSymbol.TRIANGLE -> copy(player4Pieces = pieces)
        }
    }

    fun onCellClicked(row: Int, col: Int) {
        val state = _uiState.value
        if (state.roundStatus != RoundStatus.PLAYING) return
        if (state.isAiThinking) return

        val clickedPos = BoardPosition(row, col)

        val allOccupied = state.player1Pieces + state.player2Pieces + state.player3Pieces + state.player4Pieces
        if (allOccupied.contains(clickedPos)) {
            return
        }

        // Check if user has right to move in Online mode
        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            val isMyTurn = when (state.localPlayerRole) {
                LocalPlayerRole.HOST_X -> state.currentTurn == PlayerSymbol.X
                LocalPlayerRole.GUEST_O -> state.currentTurn == PlayerSymbol.O
                LocalPlayerRole.GUEST_TICK -> state.currentTurn == PlayerSymbol.TICK
                LocalPlayerRole.GUEST_TRIANGLE -> state.currentTurn == PlayerSymbol.TRIANGLE
                LocalPlayerRole.LOCAL_ALL -> true
            }
            if (!isMyTurn) return
        }

        val turn = state.currentTurn
        val currentPieces = state.getPiecesForSymbol(turn)
        val willVanish = currentPieces.size == 3
        val updatedPieces = EndlessAiEngine.simulateMove(currentPieces, clickedPos)
        playPieceSound(turn, willVanish)

        processMoveResult(turn, updatedPieces, isNetworkMove = false)

        if (state.gameMode == GameMode.ONLINE_MULTIPLAYER) {
            relay.sendMove(row, col, turn)
        }
    }

    private fun playPieceSound(symbol: PlayerSymbol, willVanish: Boolean) {
        if (willVanish) {
            soundManager.playSound(SoundEffect.PIECE_VANISH)
        } else {
            when (symbol) {
                PlayerSymbol.X -> soundManager.playSound(SoundEffect.PIECE_PLACE_X)
                PlayerSymbol.O -> soundManager.playSound(SoundEffect.PIECE_PLACE_O)
                PlayerSymbol.TICK -> soundManager.playSound(SoundEffect.PIECE_PLACE_X)
                PlayerSymbol.TRIANGLE -> soundManager.playSound(SoundEffect.PIECE_PLACE_O)
            }
        }
    }

    private fun getNextTurnSymbol(current: PlayerSymbol, count: PlayerCount): PlayerSymbol {
        val symbols = count.symbols
        val idx = symbols.indexOf(current)
        return if (idx >= 0 && idx < symbols.size - 1) symbols[idx + 1] else symbols[0]
    }

    private fun processMoveResult(
        movedPlayer: PlayerSymbol,
        newPieces: List<BoardPosition>,
        isNetworkMove: Boolean = false
    ) {
        val state = _uiState.value
        val updatedState = state.withUpdatedPieces(movedPlayer, newPieces)
        val winningPos = EndlessAiEngine.checkWin(newPieces, state.playerCount.gridSize)

        if (winningPos != null) {
            _uiState.value = updatedState
            handlePointScored(movedPlayer, winningPos)
        } else {
            val nextPlayer = getNextTurnSymbol(movedPlayer, state.playerCount)
            val isAiNext = (state.gameMode == GameMode.SINGLE_PLAYER && nextPlayer != PlayerSymbol.X)

            _uiState.value = updatedState.copy(
                currentTurn = nextPlayer,
                isAiThinking = isAiNext
            )

            if (isAiNext) {
                triggerAiMove(nextPlayer)
            }
        }
    }

    private fun triggerAiMove(aiSymbol: PlayerSymbol) {
        viewModelScope.launch {
            delay(500)
            val state = _uiState.value
            if (state.roundStatus != RoundStatus.PLAYING) return@launch

            val aiPieces = state.getPiecesForSymbol(aiSymbol)
            val otherPieces = state.playerCount.symbols
                .filter { it != aiSymbol }
                .map { state.getPiecesForSymbol(it) }

            val bestMove = EndlessAiEngine.getBestMove(
                difficulty = state.aiDifficulty,
                aiPieces = aiPieces,
                otherPlayersPieces = otherPieces,
                gridSize = state.playerCount.gridSize
            )

            val willVanish = aiPieces.size == 3
            val updatedAiPieces = EndlessAiEngine.simulateMove(aiPieces, bestMove)
            playPieceSound(aiSymbol, willVanish)
            processMoveResult(aiSymbol, updatedAiPieces)
        }
    }

    private fun handlePointScored(
        winner: PlayerSymbol,
        winningPositions: List<BoardPosition>
    ) {
        val state = _uiState.value
        val currentStats = state.matchStats
        val updatedStats = when (winner) {
            PlayerSymbol.X -> currentStats.copy(player1Wins = currentStats.player1Wins + 1)
            PlayerSymbol.O -> currentStats.copy(player2Wins = currentStats.player2Wins + 1)
            PlayerSymbol.TICK -> currentStats.copy(player3Wins = currentStats.player3Wins + 1)
            PlayerSymbol.TRIANGLE -> currentStats.copy(player4Wins = currentStats.player4Wins + 1)
        }

        val targetSets = state.targetSets.count
        val winnerScore = updatedStats.getWins(winner)
        val isMatchWon = winnerScore >= targetSets
        val winningLine = WinningLine(winningPositions, winner)

        if (isMatchWon) {
            soundManager.playSound(SoundEffect.VICTORY_FANFARE)
            _uiState.value = state.copy(
                matchStats = updatedStats,
                roundStatus = RoundStatus.MATCH_OVER,
                winningLine = winningLine,
                matchWinner = winner,
                showVictoryDialog = true,
                isAiThinking = false
            )

            saveGameRecord(updatedStats, winner)
        } else {
            soundManager.playSound(SoundEffect.POINT_SCORED)
            _uiState.value = state.copy(
                matchStats = updatedStats,
                roundStatus = RoundStatus.POINT_SCORED,
                winningLine = winningLine,
                isAiThinking = false
            )

            viewModelScope.launch {
                delay(1600)
                val curState = _uiState.value
                if (curState.roundStatus == RoundStatus.POINT_SCORED) {
                    _uiState.value = curState.copy(
                        player1Pieces = emptyList(),
                        player2Pieces = emptyList(),
                        player3Pieces = emptyList(),
                        player4Pieces = emptyList(),
                        currentTurn = PlayerSymbol.X,
                        roundStatus = RoundStatus.PLAYING,
                        winningLine = null
                    )
                }
            }
        }
    }

    private fun saveGameRecord(stats: MatchStats, winner: PlayerSymbol) {
        viewModelScope.launch {
            val state = _uiState.value
            val winnerName = when (winner) {
                PlayerSymbol.X -> state.player1Name
                PlayerSymbol.O -> state.player2Name
                PlayerSymbol.TICK -> state.player3Name
                PlayerSymbol.TRIANGLE -> state.player4Name
            }
            val record = GameRecord(
                gameMode = state.gameMode.name,
                difficulty = if (state.gameMode == GameMode.SINGLE_PLAYER) state.aiDifficulty.name else null,
                player1Score = stats.player1Wins,
                player2Score = stats.player2Wins,
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
            player3Pieces = emptyList(),
            player4Pieces = emptyList(),
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
                player3Pieces = emptyList(),
                player4Pieces = emptyList(),
                currentTurn = PlayerSymbol.X,
                activeReactions = emptyList(),
                matchStats = MatchStats(
                    player1Wins = 0,
                    player2Wins = 0,
                    player3Wins = 0,
                    player4Wins = 0,
                    currentRound = 1,
                    targetSets = state.targetSets.count
                ),
                roundStatus = RoundStatus.PLAYING,
                winningLine = null,
                matchWinner = null,
                showVictoryDialog = false
            )
        } else {
            startMatch(state.gameMode, state.playerCount, state.aiDifficulty, state.targetSets)
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
