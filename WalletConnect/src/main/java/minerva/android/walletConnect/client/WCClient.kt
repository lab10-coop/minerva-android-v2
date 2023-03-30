package minerva.android.walletConnect.client

import android.util.Log
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidIndex
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.toByteArray
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
import minerva.android.walletConnect.providers.OkHttpProvider
import minerva.android.walletConnect.utils.WCCipher
import okhttp3.*
import okio.ByteString
import java.util.*

const val JSONRPC_VERSION = "2.0"

class WCClient(
    private val httpClient: OkHttpClient = OkHttpProvider.okHttpClient,
    builder: GsonBuilder = GsonBuilder()
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

    var isMobileWalletConnect: Boolean = false
        private set

    var peerId: String = String.Empty
        private set

    private var remotePeerId: String? = null

    var isConnected: Boolean = false

    var handshakeId: Long = Long.InvalidValue

    var accounts: List<String>? = null
        private set
    var chainId: Int? = null

    var onFailure: (error: Throwable, peerId: String, isForceTermination: Boolean) -> Unit = { _, _, _ -> }
    var onDisconnect: (code: Int, peerId: String?, isExternal: Boolean) -> Unit = { _, _, _ -> }
    var onSessionRequest: (remotePeerId: String?, peer: WCPeerMeta, chainId: Int?, peerId: String, handshakeId: Long, type: Int?) -> Unit =
        { _, _, _, _, _, _ -> }
    var onEthSign: (id: Long, message: WCEthereumSignMessage, peerId: String) -> Unit = { _, _, _ -> }
    var onEthSignTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> }
    var onEthSendTransaction: (id: Long, transaction: WCEthereumTransaction, peerId: String) -> Unit = { _, _, _ -> }
    var onCustomRequest: (id: Long, payload: String) -> Unit = { _, _ -> }
    var onGetAccounts: (id: Long) -> Unit = { _ -> }
    var onWCOpen: (peerId: String) -> Unit = { _ -> }
    var onSignTransaction: (id: Long, transaction: WCSignTransaction) -> Unit = { _, _ -> }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isConnected = true

        listeners.forEach { it.onOpen(webSocket, response) }

        val session =
            this.session ?: throw IllegalStateException("session can't be null on connection open")
        val peerId =
            this.peerId
        // The Session.topic channel is used to listen session request messages only.
        subscribe(session.topic)
        // The peerId channel is used to listen to all messages sent to this httpClient.
        subscribe(peerId)

        onWCOpen(peerId)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        var decrypted: String? = null
        try {
            val message = gson.fromJson<WCSocketMessage>(text)
            decrypted = decryptMessage(message)
            handleMessage(decrypted)
        } catch (error: Exception) {
            onFailure(error, peerId, error.isForceTerminationError())
        } finally {
            listeners.forEach { it.onMessage(webSocket, decrypted ?: text) }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        var isForceTermination = false
        if (t.isForceTerminationError() || response.isForceTerminationError()) {
            resetState()
            isForceTermination = true
        }
        onFailure(t, peerId, isForceTermination)

        if (t.isForceTerminationError() || response.isForceTerminationError()) {
            peerId = String.Empty
        }

        listeners.forEach { it.onFailure(webSocket, t, response) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        listeners.forEach { it.onClosed(webSocket, code, reason) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        listeners.forEach { it.onMessage(webSocket, bytes) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onDisconnect(code, peerId, reason == EXTERNAL_DISCONNECT)
        resetState()
        peerId = String.Empty
        listeners.forEach { it.onClosing(webSocket, code, reason) }
    }

    fun connect(
        session: WCSession,
        peerMeta: WCPeerMeta,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    ) {
        if (session.version != WCSession.VERSION_1 || session.bridge == null) {
            throw Throwable(ONLY_WC_1_ERROR)
        }

        if (this.session != null && this.session?.topic != session.topic) {
            killSession()
        }

        this.session = session
        this.peerMeta = peerMeta
        this.peerId = peerId
        this.remotePeerId = remotePeerId
        this.isMobileWalletConnect = session.isMobileWalletConnect

        val request = Request.Builder()
            .url(session.bridge)
            .build()

        socket = httpClient.newWebSocket(request, this)
    }

    fun approveSession(
        accounts: List<String>,
        chainId: Int,
        peerId: String,
        handshake: Long = Long.InvalidValue
    ): Boolean {
        if (handshake != Long.InvalidValue) {
            handshakeId = handshake
        }

        check(handshakeId > 0) { "handshakeId must be greater than 0 on session approve" }

        this.accounts = accounts
        this.chainId = chainId

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

    private fun updateSession(
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

    fun killSession(reason: String? = null): Boolean {
        updateSession(approved = false)
        return disconnect(reason)
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
                onSessionRequest(remotePeerId, param.peerMeta, param.chainId?.toInt(), peerId, handshakeId, null)
            }
            WCMethod.SESSION_UPDATE -> {
                val param = gson.fromJson<List<WCSessionUpdate>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                if (!param.approved) {
                    killSession(EXTERNAL_DISCONNECT)
                }
            }
            WCMethod.SWITCH_ETHEREUM_CHAIN -> {
                val param = gson.fromJson<List<WCSwitchEthereumChain>>(request.params)
                val chainId: Int? = Integer.decode(param.firstOrNull()?.chainId)//decode from hex string("0xXXX") to Int
                this.peerMeta?.let { peerMeta ->
                    this.chainId?.let { currentChainId ->//chain id of already connected network (which would be changed)
                        if (Int.InvalidIndex != currentChainId) {
                            //set current chain id to data for get it while popap displaying; peerId (db record ~id~) for update db
                            val meta: WCPeerMeta = peerMeta.copy(chainId = currentChainId, peerId = this.peerId, isMobileWalletConnect = this.isMobileWalletConnect)
                            onSessionRequest(this.remotePeerId, meta, chainId, this.peerId, this.handshakeId, CHANGE_TYPE)
                        }
                    }
                }
            }
            WCMethod.ETH_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)

                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.MESSAGE),
                    peerId
                )
            }
            WCMethod.ETH_PERSONAL_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)

                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE),
                    peerId
                )
            }
            WCMethod.ETH_SIGN_TYPE_DATA -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.TYPED_MESSAGE),
                    peerId
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
                onEthSendTransaction(request.id, param, peerId)
            }
            WCMethod.GET_ACCOUNTS -> {
                onGetAccounts(request.id)
            }
            WCMethod.SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCSignTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onSignTransaction(request.id, param)
            }
            else -> {
                // do nothing.
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

        val json = gson.toJson(message)
        return socket?.send(json) ?: false
    }

    fun disconnect(reason: String? = null): Boolean {
        return socket?.close(1000, reason) ?: false
    }

    private fun resetState() {
        handshakeId = -1
        isConnected = false
        session = null
        remotePeerId = null
        peerMeta = null
    }

    companion object {
        private const val EXTERNAL_DISCONNECT = "external_disconnect"
        const val CHANGE_TYPE = 1
        private const val ONLY_WC_1_ERROR = "Only WalletConnect Version 1 supported"
    }
}
