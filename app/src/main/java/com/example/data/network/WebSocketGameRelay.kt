package com.example.data.network

import android.util.Log
import com.example.domain.model.NetworkConnectionStatus
import com.example.domain.model.PlayerCount
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class RoomPlayerInfo(
    val id: String,
    val name: String,
    val symbol: PlayerSymbol
)

data class RoomStatePayload(
    val roomId: String,
    val playerCount: PlayerCount,
    val targetSets: Int,
    val players: List<RoomPlayerInfo>,
    val isGameStarted: Boolean
)

class WebSocketGameRelay {

    private val tag = "GameRelay"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null
    private var streamJob: Job? = null
    private var primaryWs: WebSocket? = null
    private var customWs: WebSocket? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Infinite read timeout for SSE stream / WebSocket
        .writeTimeout(8, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val postClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val clientId: String = UUID.randomUUID().toString().take(8)

    private val _connectionStatus = MutableStateFlow(NetworkConnectionStatus.IDLE)
    val connectionStatus: StateFlow<NetworkConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    // Callbacks
    var onMoveReceived: ((row: Int, col: Int, player: PlayerSymbol) -> Unit)? = null
    var onRoomStateUpdated: ((roomState: RoomStatePayload) -> Unit)? = null
    var onJoinRequestReceived: ((guestId: String, guestName: String) -> Unit)? = null
    var onMatchStarted: ((roomState: RoomStatePayload) -> Unit)? = null
    var onRestartRoundReceived: (() -> Unit)? = null
    var onRestartMatchReceived: (() -> Unit)? = null
    var onPlayerLeft: ((senderName: String) -> Unit)? = null
    var onReactionReceived: ((emoji: String, senderName: String, symbol: PlayerSymbol) -> Unit)? = null
    var onErrorOccurred: ((errorMessage: String) -> Unit)? = null

    private var localPlayerName: String = "Player"
    private var configuredTargetSets: Int = 5
    private var configuredPlayerCount: PlayerCount = PlayerCount.TWO
    private var customRelayServer: String = ""

    // Host tracks the room players: host is index 0 (X)
    private val joinedPlayers = mutableListOf<RoomPlayerInfo>()
    private var isGameLive = false

    // Processed messages cache (avoids duplicates across multi-gateway relays)
    private val processedMessageKeys = ConcurrentHashMap.newKeySet<String>()

    fun createRoom(
        roomId: String,
        hostName: String,
        playerCount: PlayerCount,
        targetSets: Int,
        customServerUrl: String = ""
    ) {
        disconnect()
        _isHost.value = true
        localPlayerName = hostName.ifBlank { "Host" }
        configuredPlayerCount = playerCount
        configuredTargetSets = targetSets
        customRelayServer = customServerUrl.trim()
        val cleanRoom = roomId.uppercase().trim()
        _currentRoomId.value = cleanRoom
        _connectionStatus.value = NetworkConnectionStatus.WAITING_FOR_OPPONENT
        processedMessageKeys.clear()

        joinedPlayers.clear()
        joinedPlayers.add(RoomPlayerInfo(clientId, localPlayerName, PlayerSymbol.X))
        isGameLive = false

        startRoomSync(cleanRoom)
    }

    fun joinRoom(
        roomId: String,
        guestName: String,
        customServerUrl: String = ""
    ) {
        disconnect()
        _isHost.value = false
        localPlayerName = guestName.ifBlank { "Guest" }
        customRelayServer = customServerUrl.trim()
        val cleanRoom = roomId.uppercase().trim()
        _currentRoomId.value = cleanRoom
        _connectionStatus.value = NetworkConnectionStatus.CONNECTING
        processedMessageKeys.clear()
        joinedPlayers.clear()
        isGameLive = false

        startRoomSync(cleanRoom)
    }

    private fun isUsingCustomServer(): Boolean = customRelayServer.isNotBlank()

    private fun startRoomSync(roomId: String) {
        if (isUsingCustomServer()) {
            connectToCustomWebSocket(roomId)
            return
        }

        // Primary: Native WebSocket with recent message replay
        connectToPrimaryWebSocket(roomId)
        // Secondary: Concurrent SSE stream backup
        startSseFallbackStream(roomId)
        // Heartbeat / sync scheduler
        startHeartbeatBroadcast(roomId)
    }

    private fun connectToPrimaryWebSocket(roomId: String) {
        try {
            val topic = "ettt_${roomId.lowercase()}"
            val wsUrl = "wss://ntfy.sh/$topic/ws?since=30s"
            Log.d(tag, "Connecting Primary WebSocket: $wsUrl")

            val request = Request.Builder().url(wsUrl).build()
            primaryWs = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(tag, "Primary WebSocket opened for room $roomId")
                    scope.launch {
                        if (_isHost.value) {
                            broadcastHostRoomState()
                        } else {
                            sendGuestJoinRequest()
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseAndHandleIncomingText(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(tag, "Primary WebSocket closed: $code, reason: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "Primary WebSocket failure: ${t.message}. SSE fallback remains active.")
                }
            })
        } catch (e: Exception) {
            Log.e(tag, "Error initiating WebSocket: ${e.message}")
        }
    }

    private fun startSseFallbackStream(roomId: String) {
        streamJob?.cancel()
        val topic = "ettt_${roomId.lowercase()}"

        streamJob = scope.launch {
            while (isActive) {
                try {
                    val url = "https://ntfy.sh/$topic/json?since=30s"
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "application/x-ndjson, text/event-stream")
                        .build()

                    val call = httpClient.newCall(request)
                    val response = call.execute()

                    if (response.isSuccessful) {
                        val source = response.body?.source()
                        if (source != null) {
                            while (isActive && !source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                parseAndHandleIncomingText(line)
                            }
                        }
                        response.close()
                    } else {
                        response.close()
                        delay(3000)
                    }
                } catch (e: Exception) {
                    Log.d(tag, "SSE stream notice: ${e.message}")
                    delay(2500)
                }
            }
        }
    }

    private fun parseAndHandleIncomingText(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return

        val jsonStr = if (trimmed.startsWith("data:")) {
            trimmed.substring(5).trim()
        } else {
            trimmed
        }

        if (!jsonStr.startsWith("{")) return

        try {
            val root = JSONObject(jsonStr)
            val eventType = root.optString("event", "")

            if (eventType == "keepalive" || eventType == "open") {
                return
            }

            // ntfy wrapper format: { "event": "message", "message": "{...game json...}" }
            if (root.has("message")) {
                val innerMessage = root.optString("message", "")
                if (innerMessage.startsWith("{")) {
                    handleGamePayload(innerMessage)
                    return
                }
            }

            // If payload itself is direct game JSON
            if (root.has("type")) {
                handleSingleEventObject(root.optString("msgId", UUID.randomUUID().toString()), root)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing incoming text: ${e.message}")
        }
    }

    private fun startHeartbeatBroadcast(roomId: String) {
        syncJob?.cancel()
        syncJob = scope.launch {
            // Immediate broadcast
            if (_isHost.value) {
                broadcastHostRoomState()
            } else {
                sendGuestJoinRequest()
            }

            while (isActive) {
                delay(8000)
                if (_isHost.value) {
                    broadcastHostRoomState()
                } else if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) {
                    sendGuestJoinRequest()
                }
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
                            broadcastHostRoomState()
                        } else {
                            sendGuestJoinRequest()
                        }
                        startCustomHeartbeatLoop()
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

    private fun startCustomHeartbeatLoop() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                if (_isHost.value) {
                    broadcastHostRoomState()
                } else if (_connectionStatus.value != NetworkConnectionStatus.CONNECTED) {
                    sendGuestJoinRequest()
                }
                delay(8000)
            }
        }
    }

    private fun handleSingleEventObject(eventKey: String, json: JSONObject) {
        val senderId = json.optString("senderId")
        if (senderId == clientId) {
            processedMessageKeys.add(eventKey)
            return
        }

        if (processedMessageKeys.contains(eventKey)) return
        processedMessageKeys.add(eventKey)

        val type = json.optString("type")
        val senderName = json.optString("senderName", "Player")

        scope.launch(Dispatchers.Main) {
            when (type) {
                "GUEST_JOIN_REQUEST" -> {
                    if (_isHost.value) {
                        handleGuestJoinRequest(senderId, senderName)
                    }
                }

                "ROOM_STATE", "HOST_ANNOUNCE" -> {
                    val pCountInt = json.optInt("playerCount", 2)
                    val pCount = PlayerCount.fromCount(pCountInt)
                    val targetSets = json.optInt("targetSets", 5)
                    val isGameStarted = json.optBoolean("isGameStarted", false)

                    val playersList = mutableListOf<RoomPlayerInfo>()
                    val playersArr = json.optJSONArray("players")
                    if (playersArr != null) {
                        for (i in 0 until playersArr.length()) {
                            val pObj = playersArr.getJSONObject(i)
                            val pId = pObj.optString("id")
                            val pName = pObj.optString("name")
                            val symStr = pObj.optString("symbol", "X")
                            val symbol = parseSymbol(symStr)
                            playersList.add(RoomPlayerInfo(pId, pName, symbol))
                        }
                    }

                    val roomState = RoomStatePayload(
                        roomId = _currentRoomId.value ?: "",
                        playerCount = pCount,
                        targetSets = targetSets,
                        players = playersList,
                        isGameStarted = isGameStarted
                    )

                    // Check if current guest is admitted
                    val isAdmitted = playersList.any { it.id == clientId }
                    if (!_isHost.value && isAdmitted) {
                        _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                    }

                    onRoomStateUpdated?.invoke(roomState)

                    if (isGameStarted && isAdmitted) {
                        onMatchStarted?.invoke(roomState)
                    }
                }

                "START_MATCH" -> {
                    val pCountInt = json.optInt("playerCount", 2)
                    val pCount = PlayerCount.fromCount(pCountInt)
                    val targetSets = json.optInt("targetSets", 5)

                    val playersList = mutableListOf<RoomPlayerInfo>()
                    val playersArr = json.optJSONArray("players")
                    if (playersArr != null) {
                        for (i in 0 until playersArr.length()) {
                            val pObj = playersArr.getJSONObject(i)
                            val pId = pObj.optString("id")
                            val pName = pObj.optString("name")
                            val symStr = pObj.optString("symbol", "X")
                            val symbol = parseSymbol(symStr)
                            playersList.add(RoomPlayerInfo(pId, pName, symbol))
                        }
                    }

                    val roomState = RoomStatePayload(
                        roomId = _currentRoomId.value ?: "",
                        playerCount = pCount,
                        targetSets = targetSets,
                        players = playersList,
                        isGameStarted = true
                    )

                    val isAdmitted = playersList.any { it.id == clientId }
                    if (isAdmitted || _isHost.value) {
                        _connectionStatus.value = NetworkConnectionStatus.CONNECTED
                        onMatchStarted?.invoke(roomState)
                    }
                }

                "MOVE" -> {
                    val symbolStr = json.optString("symbol", "X")
                    val row = json.getInt("row")
                    val col = json.getInt("col")
                    val symbol = parseSymbol(symbolStr)
                    onMoveReceived?.invoke(row, col, symbol)
                }

                "REACTION" -> {
                    val emoji = json.optString("emoji", "🔥")
                    val symStr = json.optString("symbol", "X")
                    val symbol = parseSymbol(symStr)
                    onReactionReceived?.invoke(emoji, senderName, symbol)
                }

                "RESTART_ROUND" -> {
                    onRestartRoundReceived?.invoke()
                }

                "RESTART_MATCH" -> {
                    onRestartMatchReceived?.invoke()
                }

                "LEAVE" -> {
                    if (_isHost.value) {
                        val removed = joinedPlayers.removeIf { it.id == senderId }
                        if (removed) {
                            broadcastHostRoomState()
                        }
                    }
                    onPlayerLeft?.invoke(senderName)
                }
            }
        }
    }

    private fun parseSymbol(symbolStr: String): PlayerSymbol {
        return when (symbolStr.trim().uppercase()) {
            "X" -> PlayerSymbol.X
            "O" -> PlayerSymbol.O
            "TICK", "✓" -> PlayerSymbol.TICK
            "TRIANGLE", "▲" -> PlayerSymbol.TRIANGLE
            else -> {
                try {
                    PlayerSymbol.valueOf(symbolStr.trim().uppercase())
                } catch (_: Exception) {
                    PlayerSymbol.X
                }
            }
        }
    }

    private fun handleGuestJoinRequest(guestId: String, guestName: String) {
        if (!_isHost.value) return

        // If guest is ALREADY joined, immediately re-send current state and start if live
        if (joinedPlayers.any { it.id == guestId }) {
            broadcastHostRoomState()
            if (isGameLive) {
                broadcastStartMatch()
            }
            return
        }

        val maxPlayers = configuredPlayerCount.count
        if (joinedPlayers.size >= maxPlayers) {
            return
        }

        // In 2-player mode, immediately admit and start match
        if (maxPlayers == 2) {
            val assignedSymbol = PlayerSymbol.O
            joinedPlayers.add(RoomPlayerInfo(guestId, guestName, assignedSymbol))
            isGameLive = true
            broadcastHostRoomState()
            broadcastStartMatch()
        } else {
            // In 3 or 4 player mode: notify host of join request
            onJoinRequestReceived?.invoke(guestId, guestName)
        }
    }

    fun hostAcceptPlayer(guestId: String, guestName: String) {
        if (!_isHost.value) return
        if (joinedPlayers.any { it.id == guestId }) {
            broadcastHostRoomState()
            return
        }

        val maxPlayers = configuredPlayerCount.count
        if (joinedPlayers.size >= maxPlayers) return

        val symbols = configuredPlayerCount.symbols
        val usedSymbols = joinedPlayers.map { it.symbol }
        val assignedSymbol = symbols.firstOrNull { it !in usedSymbols } ?: PlayerSymbol.O

        joinedPlayers.add(RoomPlayerInfo(guestId, guestName, assignedSymbol))

        broadcastHostRoomState()

        // Auto start if room reached player capacity
        if (joinedPlayers.size >= maxPlayers) {
            isGameLive = true
            broadcastStartMatch()
        }
    }

    fun hostStartMatchNow() {
        if (!_isHost.value) return
        if (joinedPlayers.size >= 2) {
            isGameLive = true
            broadcastStartMatch()
        }
    }

    fun broadcastStartMatch() {
        val playersArray = JSONArray()
        joinedPlayers.forEach { player ->
            playersArray.put(JSONObject().apply {
                put("id", player.id)
                put("name", player.name)
                put("symbol", player.symbol.name)
            })
        }

        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "START_MATCH")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("playerCount", configuredPlayerCount.count)
            put("targetSets", configuredTargetSets)
            put("players", playersArray)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)

        // Resend after short delay to guarantee delivery across network edges
        scope.launch {
            delay(350)
            dispatchCloudEvent(payload)
        }

        // Trigger locally on host
        val roomState = RoomStatePayload(
            roomId = _currentRoomId.value ?: "",
            playerCount = configuredPlayerCount,
            targetSets = configuredTargetSets,
            players = joinedPlayers.toList(),
            isGameStarted = true
        )
        _connectionStatus.value = NetworkConnectionStatus.CONNECTED
        onMatchStarted?.invoke(roomState)
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

    private fun broadcastHostRoomState() {
        val playersArray = JSONArray()
        joinedPlayers.forEach { player ->
            playersArray.put(JSONObject().apply {
                put("id", player.id)
                put("name", player.name)
                put("symbol", player.symbol.name)
            })
        }

        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "ROOM_STATE")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("playerCount", configuredPlayerCount.count)
            put("targetSets", configuredTargetSets)
            put("players", playersArray)
            put("isGameStarted", isGameLive)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)

        // Trigger locally so host UI updates instantly
        val roomState = RoomStatePayload(
            roomId = _currentRoomId.value ?: "",
            playerCount = configuredPlayerCount,
            targetSets = configuredTargetSets,
            players = joinedPlayers.toList(),
            isGameStarted = isGameLive
        )
        onRoomStateUpdated?.invoke(roomState)
    }

    private fun sendGuestJoinRequest() {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "GUEST_JOIN_REQUEST")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("timestamp", System.currentTimeMillis())
        }
        dispatchCloudEvent(payload)
    }

    fun sendReaction(emoji: String, symbol: PlayerSymbol) {
        val payload = JSONObject().apply {
            put("msgId", UUID.randomUUID().toString())
            put("type", "REACTION")
            put("senderId", clientId)
            put("senderName", localPlayerName)
            put("roomId", _currentRoomId.value)
            put("emoji", emoji)
            put("symbol", symbol.name)
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

        // Resend move with slight delay to guarantee delivery (deduplicated by msgId)
        scope.launch {
            delay(300)
            dispatchCloudEvent(payload)
            delay(400)
            dispatchCloudEvent(payload)
        }
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

        scope.launch {
            delay(300)
            dispatchCloudEvent(payload)
        }
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
            put("senderName", localPlayerName)
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
        val topic = "ettt_${roomId.lowercase()}"

        val url = "https://ntfy.sh/$topic"
        val request = Request.Builder()
            .url(url)
            .post(text.toRequestBody("text/plain; charset=utf-8".toMediaType()))
            .build()

        postClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(tag, "Dispatch event network error: ${e.message}")
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
            primaryWs?.close(1000, "User disconnected")
            primaryWs = null
            customWs?.close(1000, "User disconnected")
            customWs = null
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting: ${e.message}")
        } finally {
            _connectionStatus.value = NetworkConnectionStatus.IDLE
            _currentRoomId.value = null
            joinedPlayers.clear()
            isGameLive = false
            processedMessageKeys.clear()
        }
    }
}
