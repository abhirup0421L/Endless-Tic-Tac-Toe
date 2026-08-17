package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.GameDao
import com.example.data.db.GameRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(
    private val gameDao: GameDao,
    context: Context? = null
) {
    private val prefs: SharedPreferences? = context?.getSharedPreferences("endless_ttt_prefs", Context.MODE_PRIVATE)

    private val _playerName = MutableStateFlow(prefs?.getString("player_name", "Player 1") ?: "Player 1")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _customRelayUrl = MutableStateFlow(prefs?.getString("custom_relay_url", "") ?: "")
    val customRelayUrl: StateFlow<String> = _customRelayUrl.asStateFlow()

    val singlePlayerRecords: Flow<List<GameRecord>> = gameDao.getRecordsByMode("SINGLE_PLAYER")
    val friendRecords: Flow<List<GameRecord>> = gameDao.getRecordsByMode("FRIEND")
    val onlineRecords: Flow<List<GameRecord>> = gameDao.getRecordsByMode("ONLINE_MULTIPLAYER")
    val allRecords: Flow<List<GameRecord>> = gameDao.getAllRecords()

    fun setPlayerName(name: String) {
        val trimmed = name.trim().ifEmpty { "Player 1" }
        _playerName.value = trimmed
        prefs?.edit()?.putString("player_name", trimmed)?.apply()
    }

    fun setCustomRelayUrl(url: String) {
        val trimmed = url.trim()
        _customRelayUrl.value = trimmed
        prefs?.edit()?.putString("custom_relay_url", trimmed)?.apply()
    }

    suspend fun saveGameResult(record: GameRecord): Long {
        return gameDao.insertRecord(record)
    }

    suspend fun resetSinglePlayerScores() {
        gameDao.clearSinglePlayerScores()
    }

    suspend fun resetFriendScores() {
        gameDao.clearFriendScores()
    }

    suspend fun resetOnlineScores() {
        gameDao.clearOnlineScores()
    }

    suspend fun resetAllScores() {
        gameDao.clearAllScores()
    }
}
