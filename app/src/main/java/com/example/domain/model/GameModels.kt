package com.example.domain.model

enum class PlayerSymbol(val displayName: String) {
    X("X"),
    O("O")
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
    HOST_X,   // Host plays as X (Blue)
    GUEST_O,  // Guest plays as O (Red)
    LOCAL_BOTH // Local pass and play
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
    val index: Int get() = row * 3 + col

    companion object {
        fun fromIndex(index: Int): BoardPosition {
            return BoardPosition(index / 3, index % 3)
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
    val currentRound: Int = 1,
    val targetSets: Int = 5
)
