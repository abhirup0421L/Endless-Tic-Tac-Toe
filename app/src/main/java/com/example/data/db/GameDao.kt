package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecord): Long

    @Query("SELECT * FROM game_records WHERE gameMode = :gameMode ORDER BY timestamp DESC")
    fun getRecordsByMode(gameMode: String): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC LIMIT 50")
    fun getAllRecords(): Flow<List<GameRecord>>

    @Query("DELETE FROM game_records WHERE gameMode = 'SINGLE_PLAYER'")
    suspend fun clearSinglePlayerScores()

    @Query("DELETE FROM game_records WHERE gameMode = 'FRIEND'")
    suspend fun clearFriendScores()

    @Query("DELETE FROM game_records WHERE gameMode = 'ONLINE_MULTIPLAYER'")
    suspend fun clearOnlineScores()

    @Query("DELETE FROM game_records")
    suspend fun clearAllScores()
}
