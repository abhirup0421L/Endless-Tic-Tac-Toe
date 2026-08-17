package com.example.domain.model

enum class PlayerSymbol(val displayName: String) {
    X("X"),
    O("O"),
    TICK("✓"),
    TRIANGLE("▲")
}

enum class PlayerCount(
    val count: Int,
    val gridSize: Int,
    val label: String,
    val symbols: List<PlayerSymbol>
) {
    TWO(2, 3, "2 Players (3×3 Grid)", listOf(PlayerSymbol.X, PlayerSymbol.O)),
    THREE(3, 4, "3 Players (4×4 Grid)", listOf(PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.TICK)),
    FOUR(4, 5, "4 Players (5×5 Grid)", listOf(PlayerSymbol.X, PlayerSymbol.O, PlayerSymbol.TICK, PlayerSymbol.TRIANGLE));

    companion object {
        fun fromCount(count: Int): PlayerCount {
            return entries.find { it.count == count } ?: TWO
        }
    }
}

enum class GameMode {
    SINGLE_PLAYER,
    FRIEND,
    ONLINE_MULTIPLAYER
}

enum class NetworkConnectionStatus {
    IDLE,
    CONNECTING,
    WAITING_FOR_OPPONENT,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

enum class LocalPlayerRole {
    HOST_X,          // Host plays as X (Blue)
    GUEST_O,         // Guest 1 plays as O (Red)
    GUEST_TICK,      // Guest 2 plays as Tick (Green)
    GUEST_TRIANGLE,  // Guest 3 plays as Triangle (Purple)
    LOCAL_ALL        // Local pass and play for all players
}

enum class AiDifficulty(val title: String, val description: String) {
    EASY("Easy", "Casual & relaxed AI"),
    MEDIUM("Medium", "Tactical & watchful AI"),
    HARD("Hard", "Master strategist AI")
}

enum class TargetSets(val count: Int, val label: String) {
    FIVE(5, "5 Sets"),
    TEN(10, "10 Sets"),
    FIFTEEN(15, "15 Sets")
}

data class BoardPosition(val row: Int, val col: Int) {
    fun index(gridSize: Int = 3): Int = row * gridSize + col

    companion object {
        fun fromIndex(index: Int, gridSize: Int = 3): BoardPosition {
            return BoardPosition(index / gridSize, index % gridSize)
        }
    }
}

data class PlacedPiece(
    val player: PlayerSymbol,
    val position: BoardPosition,
    val order: Int // 1 (oldest), 2, 3 (newest)
)

data class WinningLine(
    val positions: List<BoardPosition>,
    val winner: PlayerSymbol
)

data class MatchStats(
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val player3Wins: Int = 0,
    val player4Wins: Int = 0,
    val currentRound: Int = 1,
    val targetSets: Int = 5
) {
    fun getWins(symbol: PlayerSymbol): Int = when (symbol) {
        PlayerSymbol.X -> player1Wins
        PlayerSymbol.O -> player2Wins
        PlayerSymbol.TICK -> player3Wins
        PlayerSymbol.TRIANGLE -> player4Wins
    }

    fun incrementWin(symbol: PlayerSymbol): MatchStats = when (symbol) {
        PlayerSymbol.X -> copy(player1Wins = player1Wins + 1, currentRound = currentRound + 1)
        PlayerSymbol.O -> copy(player2Wins = player2Wins + 1, currentRound = currentRound + 1)
        PlayerSymbol.TICK -> copy(player3Wins = player3Wins + 1, currentRound = currentRound + 1)
        PlayerSymbol.TRIANGLE -> copy(player4Wins = player4Wins + 1, currentRound = currentRound + 1)
    }
}

data class ActiveReaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emoji: String,
    val senderName: String = "",
    val isLocal: Boolean,
    val playerSymbol: PlayerSymbol = PlayerSymbol.X,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingJoinUser(
    val id: String,
    val name: String
)
