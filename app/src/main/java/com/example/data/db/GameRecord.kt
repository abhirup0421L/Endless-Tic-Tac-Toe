package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameMode: String, // "SINGLE_PLAYER" or "FRIEND"
    val difficulty: String? = null, // "EASY", "MEDIUM", "HARD"
    val player1Score: Int,
    val player2Score: Int,
    val targetSets: Int,
    val winner: String, // "PLAYER_1", "PLAYER_2" (or "AI")
    val timestamp: Long = System.currentTimeMillis()
)
