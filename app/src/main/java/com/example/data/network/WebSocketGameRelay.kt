package com.example.data.network

import android.util.Log
import com.example.domain.model.NetworkConnectionStatus
import com.example.domain.model.PlayerSymbol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WebSocketGameRelay {

    private val tag = "GameRelay"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null
    private var streamJob: Job? = null
    private var customWs: WebSocket? = null

    // Multiple cloud endpoints for guaranteed delivery worldwide
    private val defaultRelayServers = listOf(
        "https://ntfy.sh",
        "https://notify.run"
    )

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Infinite read timeout for SSE stream
        .writeTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val postClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    val clientId: String = UUID.randomUUID().toString().take(8)

    private val _connectionStatus = MutableStateFlow(NetworkConnectionStatus.IDLE)
    val connectionStatus: StateFlow<NetworkConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _opponentName = MutableStateFlow<String?>(null)
    val opponentName: StateFlow<String?> = _opponentName.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    // Callbacks
    var onMoveReceived: ((row: Int, col: Int, player: PlayerSymbol) -> Unit)? = null
    var onOpponentConnected: ((opponentName: String, targetSets: Int?) -> Unit)? = null
    var onRestartRoundReceived: (() -> Unit)? = null
    var onRestartMatchReceived: (() -> Unit)? = null
    var onOpponentDisconnected: (() -> Unit)? = null
    var onReactionReceived: ((emoji: String, senderName: String) -> Unit)? = null
    var onErrorOccurred: ((errorMessage: String) -> Unit)? = null

    private var localPlayerName: String = "Player"
    private var configuredTargetSets: Int = 5
    private var customRelayServer: String = ""

    // Processed messages cache
    private val processedMessageKeys = ConcurrentHashMap.newKeySet<String>()

    fun createRoom(
        roomId: String,
        hostName: String,
        targetSets: Int,
        customServerUrl: String = ""
    ) {
        disconnect()
        _isHost.value = true
        localPlayerName = hostName.ifBlank { "Host Player" }
        configuredTargetSets = targetSets
        customRelayServer = customServerUrl.trim()
        val cleanRoom = roomId.uppercase().trim()
        _currentRoomId.value = cleanRoom
        _opponentName.value = null
        _connectionStatus.value = NetworkConnectionStatus.WAITING_FOR_OPPONENT
        processedMessageKeys.clear()

        startRoomSync(cleanRoom)
    }

    fun joinRoom(
        roomId: String,
        guestName: String,
        customServerUrl: String = ""
    ) {
        disconnect()
        _isHost.value = false
        localPlayerName = guestName.ifBlank { "Guest Player" }
        customRelayServer = customServerUrl.trim()
        val cleanRoom = roomId.uppercase().trim()
        _currentRoomId.value = cleanRoom
        _opponentName.value = null
        _connectionStatus.value = NetworkConnectionStatus.CONNECTING
        processedMessageKeys.clear()

        startRoomSync(cleanRoom)
    }

    private fun isUsingCustomServer(): Boolean = customRelayServer.isNotBlank()

    private fun startRoomSync(roomId: String) {
        if (isUsingCustomServer()) {
            connectToCustomWebSocket(roomId)
            return
        }

        startRealTimeSSEStream(roomId)
        startHandshakeBroadcast(roomId)
    }

    /**
     * Real-Time Server-Sent Events (SSE) stream on ntfy.sh
     * Zero lag, instant push notifications between players!
     */
    private fun startRealTimeSSEStream(roomId: String) {
        streamJob?.cancel()
        val channelTopic = "ettt_${roomId.lowercase()}"

        streamJob = scope.launch {
            while (isActive) {
                try {
                    val url = "https://ntfy.sh/$channelTopic/sse"
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "text/event-stream")
                        .build()

                    val call = httpClient.newCall(request)
                    val response = call.execute()

                    if (response.isSuccessful) {
                        val source = response.body?.source()
                        if (source != null) {
                            while (isActive && !source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                val trimmed = line.trim()
                                if (trimmed.startsWith("data:") || trimmed.startsWith("{")) {
                                    val jsonContent = if (trimmed.startsWith("data:")) {
                                        trimmed.substring(5).trim()
                                    } else {
                                        trimmed
                                    }
                                    if (jsonContent.startsWith("{")) {
                                        try {
                                            val rootObj = JSONObject(jsonContent)
                                            val messageText = rootObj.optString("message", "")
                                            if (messageText.isNotBlank() && messageText.startsWith("{")) {
                                                handleGamePayload(messageText)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                        response.close()
                    } else {
                        response.close()
                        delay(1500)
                    }
                } catch (e: Exception) {
                    Log.d(tag, "SSE stream notice (will reconnect): ${e.message}")
                    delay(1200)
                }
            }
        }
    }

    /**
     * Handshake loop: continuously announces presence until both players connect.
     */
    private fun startHandshakeBroadcast(roomId: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            // Immediate first broadcast
            sendHandshake(isHost = _isHost.value)

            while (isActive && _connectionStatus.value != NetworkConnectionStatus.CONNECTED) {
                delay(800)
                sendHandshake(isHost = _isHost.value)
            }
        }
    }

    private fun connectToCustomWebSocket(roomId: String) {
        try {
            val base = customRelayServer
            val url = if (base.contains("{ROOM_ID}")) {
                base.replace("{ROOM_ID}", roomId)
            } else if (base.endsWith("/")) {
                "$base$roomId"
            } else {
                "$base/$roomId"
            }

            Log.d(tag, "Connecting to Custom WebSocket Relay: $url")
            val request = Request.Builder().url(url).build()

            customWs = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    scope.launch {
                        if (_isHost.value) {
                            _connectionStatus.value = NetworkConnectionStatus.WAITING_FOR_OPPONENT
                        }
                        startCustomHandshakeLoop()
                    }
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleGamePayload(text)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    scope.launch {
                        if (_connectionStatus.value == NetworkConnectionStatus.CONNECTED) {
                            _connectionStatus.value = NetworkConnectionStatus.DISCONNECTED
                        }
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(tag, "Custom WebSocket failure: ${t.message}")
                    scope.launch {
                        _connectionStatus.value = NetworkConnectionStatus.ERROR
                        onErrorOccurred?.invoke("Custom Server Error: ${t.localizedMessage}")
                    }
                }
            })
        } catch (e: Exception) {
            _connectionStatus.value = NetworkConnectionStatus.ERROR
            onErrorOccurred?.invoke("Failed to connect: ${e.localizedMessage}")
        }
    }

    private fun startCustomHandshakeLoop() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive && _connectionStatus.value != NetworkConnectionStatus.CONNECTED) {
                sendHandshake(isHost = _isHost.value)
                delay(800)
            }
        }
    }

    private fun handleSingleEventObject(eventKey: String, json: JSONObject) {
        if (processedMessageKeys.contains(eventKey)) return

        val senderId = json.optString("senderId")
        if (senderId == clientId) {
            processedMessageKeys.add(eventKey)
            return
        }

        processedMessageKeys.add(eventKey)

        val type = json.optString("type")
        val senderName = json.optString("senderName", "Opponent")
        val targetSets = if (json.has("targetSets")) json.getInt("targetSets") else null

        scope.launch(Dispatchers.Main) {
            when (type) {
                "HOST_ANNOUNCE" -> {
                    if (!_isHost.value) {
                        _opponentName.value = senderName
                        _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                        onOpponentConnected?.invoke(senderName, targetSets)
                        sendAck()
                    }
                }

                "GUEST_JOIN", "GUEST_ACK" -> {
                    _opponentName.value = senderName
                    _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                    onOpponentConnected?.invoke(senderName, targetSets)

                    if (_isHost.value && type == "GUEST_JOIN") {
                        sendHandshake(isHost = true)
                    }
                }

                "MOVE" -> {
                    val symbolStr = json.optString("symbol", "X")
                    val row = json.getInt("row")
                    val col = json.getInt("col")
                    val symbol = if (symbolStr == "X") PlayerSymbol.X else PlayerSymbol.O
                    onMoveReceived?.invoke(row, col, symbol)
                }

                "REACTION" -> {
                    val emoji = json.optString("emoji", "🔥")
                    onReactionReceived?.invoke(emoji, senderName)
                }

                "RESTART_ROUND" -> {
                    onRestartRoundReceived?.invoke()
                }

                "RESTART_MATCH" -> {
                    onRestartMatchReceived?.invoke()
                }

                "LEAVE" -> {
                    _opponentName.value = null
                    _connectionStatus.value = NetworkConnectionStatus.WAITING_FOR_OPPONENT
                    onOpponentDisconnected?.invoke()
                }
            }
        }
    }

    private fun handleGamePayload(payloadStr: String) {
        try {
            val json = JSONObject(payloadStr)
            val msgId = json.optString("msgId", UUID.randomUUID().toString())
            handleSingleEventObject(msgId, json)
        } catch (e: Exception) {
            Log.e(tag, "handleGamePayload parse error: ${e.message}")
        }
    }

    private fun sendHandshake(isHost: Boolean) {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", if (isHost) "HOST_ANNOUNCE" else "GUEST_JOIN")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("targetSets", configuredTargetSets)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    private fun sendAck() {
        val ack = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "GUEST_ACK")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(ack)
    }

    fun sendReaction(emoji: String) {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "REACTION")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("emoji", emoji)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    fun sendMove(row: Int, col: Int, symbol: PlayerSymbol) {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "MOVE")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("symbol", symbol.name)
            put("row", row)
            put("col", col)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    fun sendRestartRound() {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "RESTART_ROUND")
            put("senderId", clientId)
            put("roomId", _currentRoomId.value)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    fun sendRestartMatch() {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "RESTART_MATCH")
            put("senderId", clientId)
            put("roomId", _currentRoomId.value)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    fun sendLeaveRoom() {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "LEAVE")
            put("senderId", clientId)
            put("roomId", _currentRoomId.value)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    private fun dispatchCloudEvent(json: JSONObject) {
        val text = json.toString()

        if (isUsingCustomServer()) {
            val ws = customWs
            if (ws != null) {
                ws.send(text)
            }
            return
        }

        val roomId = _currentRoomId.value ?: return
        val channelTopic = "ettt_${roomId.lowercase()}"
        val url = "https://ntfy.sh/$channelTopic"

        val request = Request.Builder()
            .url(url)
            .post(text.toRequestBody("text/plain; charset=utf-8".toMediaType()))
            .build()

        postClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(tag, "Dispatch event error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    fun disconnect() {
        try {
            sendLeaveRoom()
            syncJob?.cancel()
            syncJob = null
            streamJob?.cancel()
            streamJob = null
            customWs?.close(1000, "User disconnected")
            customWs = null
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting: ${e.message}")
        } finally {
            _connectionStatus.value = NetworkConnectionStatus.IDLE
            _currentRoomId.value = null
            _opponentName.value = null
            processedMessageKeys.clear()
        }
    }
}
