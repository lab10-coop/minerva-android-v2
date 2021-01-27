package minerva.android.walletConnect.client

import android.util.Log
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import minerva.android.kotlinUtils.Empty
import minerva.android.walletConnect.model.enums.MessageType
import minerva.android.walletConnect.model.enums.WCMethod
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage
import minerva.android.walletConnect.model.ethereum.WCEthereumTransaction
import minerva.android.walletConnect.model.exceptions.InvalidJsonRpcParamsException
import minerva.android.walletConnect.model.jsonRpc.JsonRpcError
import minerva.android.walletConnect.model.jsonRpc.JsonRpcErrorResponse
import minerva.android.walletConnect.model.jsonRpc.JsonRpcRequest
import minerva.android.walletConnect.model.jsonRpc.JsonRpcResponse
import minerva.android.walletConnect.model.session.*
import minerva.android.walletConnect.utils.WCCipher
import minerva.android.walletConnect.utils.toByteArray
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import java.util.*

const val JSONRPC_VERSION = "2.0"

open class WCClient(
    builder: GsonBuilder = GsonBuilder(),
    private val httpClient: OkHttpClient = OkHttpClient()
) : WebSocketListener() {

    private val TAG = WCClient::class.java.simpleName

    private val gson = builder
        .serializeNulls()
        .create()

    private var socket: WebSocket? = null

    private val listeners: MutableSet<WebSocketListener> = mutableSetOf()

    var session: WCSession? = null
        private set

    private var peerMeta: WCPeerMeta? = null

    var peerId: String = String.Empty
        private set

    private var remotePeerId: String? = null

    private var isConnected: Boolean = false

    fun sessionId(): String? =
        if (session != null) session!!.topic;
        else null;

    private var handshakeId: Long = -1

    var accounts: List<String>? = null
        private set
    private var chainId: Int? = null
        private set

    var onFailure: (error: Throwable, peerId: String) -> Unit = { _, _ -> }
    var onDisconnect: (code: Int, peerId: String?) -> Unit = { _, _ -> }
    var onSessionRequest: (remotePeerId: String?, peer: WCPeerMeta, chainId: Int?, peerId: String) -> Unit =
        { _, _, _, _ -> }
    var onEthSign: (id: Long, message: WCEthereumSignMessage) -> Unit = { _, _ -> }
    var onEthSignTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> }
    var onEthSendTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> }
    var onCustomRequest: (id: Long, payload: String) -> Unit = { _, _ -> }
    var onGetAccounts: (id: Long) -> Unit = { _ -> }
    var onWCOpen: (peerId: String) -> Unit = { _ -> }
    var onPong: (peerId: String) -> Unit = { _ -> }
    var onSignTransaction: (id: Long, transaction: WCSignTransaction) -> Unit = { _, _ -> }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isConnected = true

        listeners.forEach { it.onOpen(webSocket, response) }

        val session =
            this.session ?: throw IllegalStateException("session can't be null on connection open")
        val peerId =
            this.peerId ?: throw IllegalStateException("peerId can't be null on connection open")
        // The Session.topic channel is used to listen session request messages only.
        subscribe(session.topic)
        // The peerId channel is used to listen to all messages sent to this httpClient.
        subscribe(peerId)

        onWCOpen(peerId);
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        var decrypted: String? = null
        try {
            if (text.equals("ping")) {
                onPong(text);
                return;
            }

            val message = gson.fromJson<WCSocketMessage>(text)
            decrypted = decryptMessage(message)
            handleMessage(decrypted)
        } catch (e: Exception) {
            onFailure(e, peerId)
        } finally {
            listeners.forEach { it.onMessage(webSocket, decrypted ?: text) }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        resetState()
        onFailure(t, peerId)

        listeners.forEach { it.onFailure(webSocket, t, response) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        listeners.forEach { it.onClosed(webSocket, code, reason) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        listeners.forEach { it.onMessage(webSocket, bytes) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onDisconnect(code, peerId)
        resetState()
        listeners.forEach { it.onClosing(webSocket, code, reason) }
    }

    fun connect(
        session: WCSession,
        peerMeta: WCPeerMeta,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    ) {
        if (this.session != null && this.session?.topic != session.topic) {
            killSession()
        }

        this.session = session
        this.peerMeta = peerMeta
        this.peerId = peerId
        this.remotePeerId = remotePeerId

        val request = Request.Builder()
            .url(session.bridge)
            .build()

        socket = httpClient.newWebSocket(request, this)
    }

    fun approveSession(accounts: List<String>, chainId: Int, peerId: String): Boolean {
        check(handshakeId > 0) { "handshakeId must be greater than 0 on session approve" }

        this.accounts = accounts;
        this.chainId = chainId;

        val result = WCApproveSessionResponse(
            chainId = chainId,
            accounts = accounts,
            peerId = peerId,
            peerMeta = peerMeta
        )
        val response = JsonRpcResponse(
            id = handshakeId,
            result = result
        )

        return encryptAndSend(gson.toJson(response))
    }

    fun sendPing(): Boolean {
        return socket?.send("ping") ?: false
    }

    fun updateSession(
        accounts: List<String>? = null,
        chainId: Int? = null,
        approved: Boolean = true
    ): Boolean {
        val request = JsonRpcRequest(
            id = Date().time,
            method = WCMethod.SESSION_UPDATE,
            params = listOf(
                WCSessionUpdate(
                    approved = approved,
                    chainId = chainId,
                    accounts = accounts
                )
            )
        )
        return encryptAndSend(gson.toJson(request))
    }

    fun rejectSession(message: String = "Session rejected"): Boolean {
        check(handshakeId > 0) { "handshakeId must be greater than 0" }
        val response = JsonRpcErrorResponse(
            id = handshakeId,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun killSession(): Boolean {
        updateSession(approved = false)
        return disconnect()
    }

    fun <T> approveRequest(id: Long, result: T): Boolean {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun rejectRequest(id: Long, message: String = "Rejected by the user"): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun decryptMessage(message: WCSocketMessage): String {
        val encrypted = gson.fromJson<WCEncryptionPayload>(message.payload)
        val session = this.session
            ?: throw IllegalStateException("Session is null")
        return String(WCCipher.decrypt(encrypted, session.key.toByteArray()), Charsets.UTF_8)
    }

    private fun invalidParams(id: Long): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.invalidParams(
                message = "Invalid parameters"
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun handleMessage(payload: String) {
        try {
            val request = gson.fromJson<JsonRpcRequest<JsonArray>>(
                payload,
                typeToken<JsonRpcRequest<JsonArray>>()
            )
            val method = request.method
            if (method != null) {
                handleRequest(request)
            } else {
                onCustomRequest(request.id, payload)
            }
        } catch (e: InvalidJsonRpcParamsException) {
            invalidParams(e.requestId)
        }
    }

    private fun handleRequest(request: JsonRpcRequest<JsonArray>) {
        when (request.method) {
            WCMethod.SESSION_REQUEST -> {
                val param = gson.fromJson<List<WCSessionRequest>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                handshakeId = request.id
                remotePeerId = param.peerId
                onSessionRequest(remotePeerId, param.peerMeta, param.chainId?.toInt(), peerId)
            }
            WCMethod.SESSION_UPDATE -> {
                val param = gson.fromJson<List<WCSessionUpdate>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                if (!param.approved) {
                    killSession()
                }
            }
            WCMethod.ETH_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.MESSAGE)
                )
            }
            WCMethod.ETH_PERSONAL_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE)
                )
            }
            WCMethod.ETH_SIGN_TYPE_DATA -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.TYPED_MESSAGE)
                )
            }
            WCMethod.ETH_SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSignTransaction(request.id, param)
            }
            WCMethod.ETH_SEND_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSendTransaction(request.id, param)
            }
            WCMethod.GET_ACCOUNTS -> {
                onGetAccounts(request.id)
            }
            WCMethod.SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCSignTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onSignTransaction(request.id, param)
            }
        }
    }

    private fun subscribe(topic: String): Boolean {
        val message = WCSocketMessage(
            topic = topic,
            type = MessageType.SUB,
            payload = ""
        )
        val json = gson.toJson(message)
        Log.d(TAG, "==> Subscribe: $json")

        return socket?.send(gson.toJson(message)) ?: false
    }

    private fun encryptAndSend(result: String): Boolean {
        Log.d(TAG, "==> message $result")
        val session = this.session
            ?: throw IllegalStateException("Session is null")
        val payload = gson.toJson(
            WCCipher.encrypt(
                result.toByteArray(Charsets.UTF_8),
                session.key.toByteArray()
            )
        )
        val message = WCSocketMessage(
            topic = remotePeerId ?: session.topic,
            type = MessageType.PUB,
            payload = payload
        )

        val rpId = remotePeerId ?: session.topic

        val json = gson.toJson(message)
        return socket?.send(json) ?: false
    }

    fun disconnect(): Boolean {
        return socket?.close(1000, null) ?: false
    }

    fun addSocketListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeSocketListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    private fun resetState() {
        handshakeId = -1
        isConnected = false
        session = null
        peerId = String.Empty
        remotePeerId = null
        peerMeta = null
    }
}
