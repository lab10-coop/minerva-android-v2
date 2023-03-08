package minerva.android.walletmanager.repository.walletconnect

import android.app.Application
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
import minerva.android.walletmanager.BuildConfig
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.getFormattedMessage
import minerva.android.kotlinUtils.crypto.hexToUtf8
import minerva.android.kotlinUtils.list.mergeWithoutDuplicates
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.MESSAGE
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.TYPED_MESSAGE
import minerva.android.walletConnect.model.ethereum.WCEthereumTransaction
import minerva.android.walletConnect.model.exceptions.InvalidJsonRpcParamsException
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.manager.networks.NetworkManager.getNetworkNameOrNull
import minerva.android.walletmanager.model.mappers.DappSessionToEntityMapper
import minerva.android.walletmanager.model.mappers.EntitiesToDappSessionsMapper
import minerva.android.walletmanager.model.mappers.SessionEntityToDappSessionMapper
import minerva.android.walletmanager.model.mappers.WCEthTransactionToWalletConnectTransactionMapper
import minerva.android.walletmanager.model.mappers.WCPeerToWalletConnectPeerMetaMapper
import minerva.android.walletmanager.model.mappers.WCSessionToWalletConnectSessionMapper
import minerva.android.walletmanager.model.mappers.WalletConnectSessionMapper
import minerva.android.walletmanager.model.minervaprimitives.MinervaPrimitive
import minerva.android.walletmanager.model.walletconnect.*
import minerva.android.walletmanager.utils.logger.Logger
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WalletConnectRepositoryImpl(
    private val signatureRepository: SignatureRepository,
    minervaDatabase: MinervaDatabase,
    private val logger: Logger,
    private var wcClient: WCClient = WCClient(),
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap(),
    builder: GsonBuilder = GsonBuilder()
) : WalletConnectRepository {
    private var status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override var connectionStatusFlowable: Flowable<WalletConnectStatus> =
        status.toFlowable(BackpressureStrategy.DROP)
    private var currentRequestId: Long = Long.InvalidValue
    internal lateinit var currentEthMessage: WCEthereumSignMessage
    private var disposable: CompositeDisposable = CompositeDisposable()
    private var pingDisposable: Disposable? = null
    private val dappDao = minervaDatabase.dappSessionDao()
    private var reconnectionAttempts: MutableMap<String, Int> = mutableMapOf()
    private var isInitialized = initialize()
    private val gson = builder
        .serializeNulls()
        .create()

    private fun sessionProposalToWalletConnectProposalNamespace(sessionProposal: Sign.Model.SessionProposal): WalletConnectProposalNamespace {
        val eip155TempNamespace = sessionProposal.requiredNamespaces[EIP155]!!
        return WalletConnectProposalNamespace(
            chains = eip155TempNamespace.chains ?: emptyList(),
            methods = eip155TempNamespace.methods,
            events = eip155TempNamespace.events
        )
    }

    private fun initialize(): Boolean {
        if (isInitialized) {
            return true
        }

        val walletDelegate = object : SignClient.WalletDelegate {
            override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
                // Triggered when wallet receives the session proposal sent by a Dapp
                Timber.i("onSessionProposal: $sessionProposal")

                val numberOfNonEip155Chains = namespacesCountNonEip155Chains(sessionProposal.requiredNamespaces)
                val eip155ProposalNamespace = sessionProposalToWalletConnectProposalNamespace(sessionProposal)

                // show popup here, only then proceed
                status.onNext(OnSessionRequestV2(
                    WalletConnectPeerMeta(
                        name = sessionProposal.name,
                        url = sessionProposal.url,
                        description = sessionProposal.description,
                        icons = sessionProposal.icons.map { it.toString() },
                        proposerPublicKey = sessionProposal.proposerPublicKey
                    ),
                    numberOfNonEip155Chains,
                    eip155ProposalNamespace
                ))
            }

            override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
                // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
                Timber.i("onSessionRequest: $sessionRequest")

                // todo: test this
                // todo: move somewhere more sensible
                fun caipChainIdToInt(chainId: String?): Int {
                    return chainId?.split(":")?.get(1)?.toInt() ?: Int.InvalidValue
                }
                val chainId = caipChainIdToInt(sessionRequest.chainId)

                // todo: use some constants here
                // todo: maybe all sign messages could be combined in one case?
                when (sessionRequest.request.method) {
                    "personal_sign" -> {
                        // this seems to be something metamask specific
                        // todo: throw InvalidJsonRpcParamsException?
                        // https://docs.walletconnect.com/2.0/advanced/rpc-reference/ethereum-rpc#personal_sign
                        val params: List<String> = gson.fromJson(sessionRequest.request.params, Array<String>::class.java).toList()
                        currentRequestId = sessionRequest.request.id // todo: or should we pass it along?
                        currentEthMessage = WCEthereumSignMessage(type = PERSONAL_MESSAGE, raw = params)
                        // todo: isMobileWalletConnect?
                        // todo: accountName needs to come from somewhere else..
                        val session = getDappSessionByTopic(sessionRequest.topic, currentEthMessage.address, chainId)
                        session?.let {
                            status.onNext(OnEthSignV2(currentEthMessage.data.hexToUtf8.getFormattedMessage, it))
                        }
                    }
                    "eth_sign" -> {
                        // https://docs.walletconnect.com/2.0/advanced/rpc-reference/ethereum-rpc#eth_sign
                        // todo: throw InvalidJsonRpcParamsException?
                        val params: List<String> = gson.fromJson(sessionRequest.request.params, Array<String>::class.java).toList()
                        currentRequestId = sessionRequest.request.id // todo: or should we pass it along?
                        currentEthMessage = WCEthereumSignMessage(type = MESSAGE, raw = params)
                        // todo: isMobileWalletConnect?
                        // todo: accountName needs to come from somewhere else..
                        val session = getDappSessionByTopic(sessionRequest.topic, currentEthMessage.address, chainId)
                        session?.let {
                            status.onNext(OnEthSignV2(currentEthMessage.data.hexToUtf8.getFormattedMessage, it))
                        }
                    }
                    "eth_signTypedData" -> {
                        // https://docs.walletconnect.com/2.0/advanced/rpc-reference/ethereum-rpc#eth_signtypeddata
                        // todo: refactor? this section is weird because it tries to use WC 1.0 structures
                        // todo: throw InvalidJsonRpcParamsException?
                        val params: List<String> = JsonParser.parseString(sessionRequest.request.params).asJsonArray
                            .map { value ->
                                if (value.toString().startsWith("\"0x")) {
                                    return@map value.asString
                                }
                                val stringBuilder = StringBuilder()
                                // todo: with or without 0x?
                                stringBuilder.append("0x")
                                for (char in value.toString()) {
                                    val hexChar = Integer.toHexString(char.code)
                                    stringBuilder.append(hexChar)
                                }
                                stringBuilder.toString().hexToUtf8
                            }
                        currentRequestId = sessionRequest.request.id // todo: or should we pass it along?
                        currentEthMessage = WCEthereumSignMessage(type = TYPED_MESSAGE, raw = params)
                        // todo: isMobileWalletConnect?
                        // todo: accountName needs to come from somewhere else..
                        val session = getDappSessionByTopic(sessionRequest.topic, currentEthMessage.address, chainId)
                        session?.let {
                            status.onNext(OnEthSignV2(currentEthMessage.data.getFormattedMessage, it))
                        }
                    }
                    "eth_sendTransaction" -> {
                        // It seems like eth_sendTransaction allows for multiple Transactions
                        val listType = object : TypeToken<List<WCEthereumTransaction>>() {}.type
                        val transaction =
                            gson.fromJson<List<WCEthereumTransaction>>(sessionRequest.request.params, listType)
                                .firstOrNull() ?: throw InvalidJsonRpcParamsException(sessionRequest.request.id)
                        currentRequestId = sessionRequest.request.id // todo: or should we pass it along?
                        Timber.i("${LoggerMessages.ON_ETH_SEND_TX} topic: ${sessionRequest.topic}; transaction: $transaction")
                        // todo: isMobileWalletConnect?
                        // todo: accountName needs to come from somewhere else..
                        val session = getDappSessionByTopic(sessionRequest.topic, transaction.from, chainId)
                        session?.let {
                            status.onNext(
                                OnEthSendTransactionV2(
                                    WCEthTransactionToWalletConnectTransactionMapper.map(transaction),
                                    session
                                )
                            )
                        }
                    }
                    "eth_signTransaction" -> {
                        // todo: but not that common or important. wasn't implemented for wc 1.0
                    }
                    "eth_sendRawTransaction" -> {
                        // todo: but not that common or important. wasn't implemented for wc 1.0
                    }
                }
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                // Triggered when the session is deleted by the peer
                Timber.i("onSessionDelete")
                when (deletedSession) {
                    is Sign.Model.DeletedSession.Success -> {
                        Timber.i("onSessionDelete Success")
                        val session = SignClient.getActiveSessionByTopic(deletedSession.topic)
                        val name = session?.metaData?.name
                        // todo: fetching the name of the session doesn't seem to work.
                        // todo: also we could give a reason for the session end here.
                        status.onNext(OnDisconnect(name ?: String.Empty))
                    }
                    is Sign.Model.DeletedSession.Error -> {
                        Timber.i("onSessionDelete Error")
                        status.onNext(OnDisconnect(String.Empty))
                    }
                }
            }

            override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
                // Triggered when wallet receives the session settlement response from Dapp
                Timber.i("onSessionSettleResponse")
                // todo
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
                // Triggered when wallet receives the session update response from Dapp
                Timber.i("onSessionUpdateResponse")
                // todo
            }

            override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
                //Triggered whenever the connection state is changed
                Timber.i("onConnectionStateChange")
                // todo
            }

            override fun onError(error: Sign.Model.Error) {
                // Triggered whenever there is an issue inside the SDK
                Timber.e("onError: $error")
                // todo
            }
        }
        try {
            SignClient.setWalletDelegate(walletDelegate)
        } catch (e: IllegalStateException) {
            Timber.e(e.toString())
        }

        return true
    }

    override fun connect(
        session: WalletConnectSession,
        peerId: String,
        remotePeerId: String?,
        dapps: List<DappSessionV1>
    ) {
        if (session.version == "2") {
            Timber.i("Connect WalletConnect 2.0 pairing: ${session.toUri()}")
            val pairingParams = Core.Params.Pair(session.toUri())
            CoreClient.Pairing.pair(pairingParams) { error ->
                Timber.e(error.toString())
            }
            return
        }

        wcClient = WCClient()
        with(wcClient) {

            onWCOpen = { peerId ->
                logger.logToFirebase("${LoggerMessages.ON_CONNECTION_OPEN}$peerId")
                if (reconnectionAttempts.isNotEmpty()) {
                    reconnectionAttempts.clear()
                }
                clientMap[peerId] = this
                if (pingDisposable == null) {
                    startPing(dapps)
                }
            }

            onSessionRequest = { remotePeerId, meta, chainId, peerId, handshakeId, type ->
                logger.logToFirebase("${LoggerMessages.ON_SESSION_REQUEST}$peerId")
                status.onNext(
                    OnSessionRequest(
                        WCPeerToWalletConnectPeerMetaMapper.map(meta),
                        chainId,
                        Topic(peerId, remotePeerId),
                        handshakeId,
                        type = type
                    )
                )
            }
            onFailure = { error, peerId, isForceTermination ->
                if (isForceTermination) {
                    logger.logToFirebase("${LoggerMessages.CONNECTION_TERMINATION} $error, peerId: $peerId")
                    terminateConnection(peerId, error)
                } else {
                    logger.logToFirebase("${LoggerMessages.RECONNECTING_CONNECTION} $error, peerId: $peerId")
                    reconnect(peerId, error, remotePeerId)
                }
            }
            onDisconnect = { _, peerId, isExternal ->
                peerId?.let {
                    logger.logToFirebase("${LoggerMessages.ON_DISCONNECTING} $peerId")
                    if (reconnectionAttempts.containsKey(peerId)) {
                        reconnectionAttempts.remove(peerId)
                    }
                    if (isExternal) {
                        removeDappSession(it, onSuccess = { session ->
                            removeWcClient(session.peerId)
                            status.onNext(OnDisconnect(session.name))
                        }, onError = {
                            if (clientMap.containsKey(peerId)) {
                                clientMap.remove(peerId)
                                status.onNext(OnDisconnect())
                            }
                        })
                    }
                }
            }

            onEthSign = { id, message, peerId ->
                logger.logToFirebase("${LoggerMessages.ON_ETH_SIGN} $peerId")
                currentRequestId = id
                currentEthMessage = message
                status.onNext(OnEthSign(getUserReadableData(message), peerId))
            }

            onEthSendTransaction = { id, transaction, peerId ->
                logger.logToFirebase("${LoggerMessages.ON_ETH_SEND_TX} peerId: $peerId; transaction: $transaction")
                currentRequestId = id
                status.onNext(
                    OnEthSendTransactionV1(
                        WCEthTransactionToWalletConnectTransactionMapper.map(transaction),
                        peerId
                    )
                )
            }
            connect(
                WalletConnectSessionMapper.map(session),
                peerMeta = WCPeerMeta(),
                peerId = peerId,
                remotePeerId = remotePeerId
            )
        }
    }

    private fun reconnect(peerId: String, error: Throwable, remotePeerId: String?) {
        if (reconnectionAttempts[peerId] == MAX_RECONNECTION_ATTEMPTS) {
            terminateConnection(peerId, error)
        } else {
            retryConnection(peerId, remotePeerId, error)
        }
    }

    private fun terminateConnection(peerId: String, error: Throwable) {
        if (reconnectionAttempts.containsKey(peerId)) {
            reconnectionAttempts.remove(peerId)
        }
        removeDappSession(peerId, onSuccess = { session ->
            removeWcClient(session.peerId)
            status.onNext(OnFailure(error, session.name))
        }, onError = {
            removeWcClient(peerId)
            status.onNext(OnFailure(error, String.Empty))
        })
    }

    private fun retryConnection(peerId: String, remotePeerId: String?, error: Throwable) {
        checkDisposable()
        Observable.timer(RETRY_DELAY, TimeUnit.SECONDS)
            .flatMapSingle { getDappSessionById(peerId) }
            .map { session ->
                var attempt: Int = reconnectionAttempts[peerId] ?: INIT_ATTEMPT
                attempt += ONE_ATTEMPT
                reconnectionAttempts[peerId] = attempt
                with(session) {
                    clientMap[peerId]!!.connect(
                        WalletConnectSessionMapper.map(
                            WalletConnectSession(
                                topic,
                                version,
                                key,
                                bridge
                            )
                        ),
                        peerMeta = WCPeerMeta(),
                        peerId = peerId,
                        remotePeerId = remotePeerId
                    )
                }
            }
            .doOnError { terminateConnection(peerId, error)  }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposable)
    }

    private fun removeDappSession(
        peerId: String,
        onSuccess: (session: DappSessionV1) -> Unit,
        onError: () -> Unit
    ) {
        checkDisposable()
        getDappSessionById(peerId)
            .flatMap { session -> deleteDappSession(session.peerId).toSingleDefault(session) }
            .map { session -> onSuccess(session) }
            .doOnError { onError() }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposable)
    }

    private fun checkDisposable() {
        if (disposable.isDisposed) {
            disposable = CompositeDisposable()
        }
    }

    override fun rejectSession(peerId: String) {
        logger.logToFirebase("${LoggerMessages.REJECT_SESSION} $peerId")
        with(clientMap) {
            this[peerId]?.rejectSession()
            this[peerId]?.disconnect()
            remove(peerId)
        }
    }

    override fun removeDeadSessions() = with(clientMap) {
        forEach {
            if (it.value.handshakeId == Long.InvalidValue) {
                it.value.disconnect()
                remove(it.key)
            }
        }
    }

    private fun removeWcClient(peerId: String) {
        with(clientMap) {
            if (containsKey(peerId)) {
                if (this[peerId]?.session != null) {
                    this[peerId]?.killSession()
                }
                remove(peerId)
            }
        }
    }

    override fun saveDappSession(dappSession: DappSessionV1): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun updateDappSession(peerId: String, address: String, chainId: Int, accountName: String, networkName: String): Completable =
        dappDao.update(peerId, address, chainId, accountName, networkName)

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getV2Pairings(): List<Pairing> =
        CoreClient.Pairing.getPairings()
            // todo: create some mapper instead
            .map { pairing ->
                Pairing(
                    String.Empty,
                    pairing.topic,
                    pairing.peerAppMetaData?.name ?: String.Empty,
                    // todo: check which icon is good
                    pairing.peerAppMetaData?.icons?.getOrNull(0) ?: String.Empty,
                    String.Empty,
                    String.Empty,
                    Int.InvalidValue,
                    pairing.peerAppMetaData
                )
            }

    override fun getV2PairingsWithoutActiveSession(): List<Pairing> {
        try {
            val sessions = SignClient.getListOfActiveSessions()
            return CoreClient.Pairing.getPairings()
                .filter { pairing -> !sessions.any { session -> session.pairingTopic == pairing.topic} }
                // todo: create some mapper instead
                .map { pairing ->
                    Pairing(
                        String.Empty,
                        pairing.topic,
                        pairing.peerAppMetaData?.name ?: String.Empty,
                        // todo: check which icon is good
                        pairing.peerAppMetaData?.icons?.getOrNull(0) ?: String.Empty,
                        String.Empty,
                        String.Empty,
                        Int.InvalidValue,
                        pairing.peerAppMetaData
                    )
                }
        } catch (e: IllegalStateException) {
            Timber.e(e.toString())
            return emptyList<Pairing>()
        }
    }

    override fun getV2Sessions(): List<DappSessionV2> =
        try {
            SignClient.getListOfActiveSessions()
                // todo: create some mapper instead
                .map { session -> DappSessionV2(
                    String.Empty,
                    session.topic,
                    "2",
                    session.metaData?.name ?: String.Empty,
                    // todo: check which icon is good
                    session.metaData?.icons?.getOrNull(0) ?: String.Empty,
                    String.Empty,
                    String.Empty,
                    String.Empty,
                    Int.InvalidValue,
                    false, // todo: how do we know this here.
                    session.metaData,
                    session.namespaces
                ) }
        } catch (e: IllegalStateException) {
            Timber.e(e.toString())
            emptyList<DappSessionV2>()
        }

    override fun getV1Sessions(): Single<List<DappSessionV1>> =
        dappDao.getAll().firstOrError()
            .map { EntitiesToDappSessionsMapper.map(it) }

    override fun getV1SessionsFlowable(): Flowable<List<DappSessionV1>> =
        dappDao.getAll()
            .map { EntitiesToDappSessionsMapper.map(it) }

    // only returns pairings without active sessions
    override fun getSessionsAndPairings(): Single<List<MinervaPrimitive>> =
        dappDao.getAll().firstOrError()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
                    .mergeWithoutDuplicates(getV2PairingsWithoutActiveSession())
            }

    // only returns pairings without active sessions
    override fun getSessionsAndPairingsFlowable(): Flowable<List<MinervaPrimitive>> =
        dappDao.getAll()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
                    .mergeWithoutDuplicates(getV2PairingsWithoutActiveSession())
            }

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
            }

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
            }

    override fun getDappSessionById(peerId: String): Single<DappSessionV1> =
        dappDao.getDappSessionById(peerId).map { SessionEntityToDappSessionMapper.map(it) }

    fun getDappSessionByTopic(topic: String, address: String = String.Empty, chainId: Int = Int.InvalidValue): DappSessionV2? {
        val session = SignClient.getActiveSessionByTopic(topic) ?: return null
        val networkName = getNetworkNameOrNull(chainId) ?: String.Empty
        // todo: create some mapper instead
        return DappSessionV2(
            address,
            session.topic,
            "2",
            session.metaData?.name ?: String.Empty,
            // todo: check which icon is good
            session.metaData?.icons?.getOrNull(0) ?: String.Empty,
            String.Empty,
            networkName,
            String.Empty,
            chainId,
            false, // todo: how do we know this here.
            session.metaData,
            session.namespaces
        )
    }

    private fun getUserReadableData(message: WCEthereumSignMessage) =
        when (message.type) {
            PERSONAL_MESSAGE -> message.data.hexToUtf8
            MESSAGE, TYPED_MESSAGE -> message.data.getFormattedMessage
        }

    override fun getWCSessionFromQr(qrCode: String): WalletConnectSession =
        WCSessionToWalletConnectSessionMapper.map(WCSession.from(qrCode))

    override fun approveSession(
        addresses: List<String>,
        chainId: Int,
        peerId: String,
        dapp: DappSessionV1
    ): Completable =
        if (clientMap[peerId]?.approveSession(addresses, chainId, peerId) == true) {
            logger.logToFirebase("${LoggerMessages.APPROVE_SESSION} $peerId")
            saveDappSession(dapp)
        } else {
            Completable.error(Throwable("Session not approved"))
        }

    override fun approveSessionV2(
        proposerPublicKey: String,
        namespace: WalletConnectSessionNamespace
        // todo: pass relayProtocol here?
    ): Completable {
        val namespaces: Map<String, Sign.Model.Namespace.Session> =
            mapOf(
                Pair(
                    EIP155,
                    Sign.Model.Namespace.Session(
                        chains = namespace.chains,
                        accounts = namespace.accounts,
                        methods = namespace.methods,
                        events = namespace.events)
                )
            )
        val relayProtocol = null
        val approveParams: Sign.Params.Approve = Sign.Params.Approve(proposerPublicKey, namespaces, relayProtocol)

        return Completable.create { emitter ->
            SignClient.approveSession(approveParams) { error ->
                Timber.e(error.toString())
                emitter.onError(Throwable(error.toString()))
            }
            emitter.onComplete()
        }
    }

    override fun rejectSessionV2(proposerPublicKey: String, reason: String) {
        SignClient.rejectSession(Sign.Params.Reject(proposerPublicKey, reason)) {
            error ->
            Timber.e(error.toString())
        }
    }

    override fun updateSession(
        connectionPeerId: String,
        accountAddress: String,
        accountChainId: Int,
        accountName: String,
        networkName: String
    ): Completable =
        if (clientMap[connectionPeerId]?.approveSession(listOf(accountAddress), accountChainId, connectionPeerId) == true) {
            logger.logToFirebase("${LoggerMessages.APPROVE_SESSION} ${connectionPeerId}")
            //update specified dapp session db record by specified parameters
            updateDappSession(connectionPeerId, accountAddress, accountChainId, accountName, networkName)
        } else {
            Completable.error(Throwable("Update of Session not approved"))
        }

    private fun startPing(dapps: List<DappSessionV1>) {
        pingDisposable = Observable.interval(0, PING_TIMEOUT, TimeUnit.SECONDS)
            .doOnNext { ping(dapps) }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e("Error while ping: $it") })
    }

    private fun ping(dapps: List<DappSessionV1>) {
        if (clientMap.isNotEmpty()) {
            clientMap.forEach { entry ->
                val currentDapp = dapps.find { dapp -> dapp.peerId == entry.key }
                if (shouldPing(entry)) {
                    entry.value.approveSession(
                        entry.value.accounts!!,
                        entry.value.chainId!!,
                        entry.key
                    )
                } else if (dapps.isNotEmpty() && currentDapp != null && entry.value.session != null) {
                    entry.value.approveSession(
                        listOf(currentDapp.address),
                        currentDapp.chainId,
                        currentDapp.peerId,
                        currentDapp.handshakeId
                    )
                }
            }
        } else {
            pingDisposable?.dispose()
            pingDisposable = null
        }
    }

    private fun shouldPing(it: Map.Entry<String, WCClient>) =
        it.value.isConnected && it.value.accounts != null && it.value.chainId != null

    override fun approveRequest(peerId: String, privateKey: String) {
        logger.logToFirebase("${LoggerMessages.APPROVE_REQUEST} $peerId")
        clientMap[peerId]?.approveRequest(currentRequestId, signData(privateKey))
    }

    override fun approveRequestV2(topic: String, privateKey: String) {
        logger.logToFirebase("${LoggerMessages.APPROVE_REQUEST} topic: $topic")
        Timber.i("${LoggerMessages.APPROVE_REQUEST} topic: $topic")
        val jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
            id = currentRequestId,
            result = signData(privateKey)
        )
        Timber.i("${LoggerMessages.APPROVE_REQUEST} $topic $jsonRpcResponse")
        val result = Sign.Params.Response(sessionTopic = topic, jsonRpcResponse = jsonRpcResponse)
        SignClient.respond(result) { error ->
            Timber.e(error.toString())
        }
    }

    override fun approveTransactionRequest(peerId: String, message: String) {
        logger.logToFirebase("${LoggerMessages.APPROVE_TX_REQUEST} peerId: $peerId")
        clientMap[peerId]?.approveRequest(currentRequestId, message)
    }

    override fun approveTransactionRequestV2(topic: String, message: String) {
        logger.logToFirebase("${LoggerMessages.APPROVE_TX_REQUEST} topic: $topic")
        val jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
            id = currentRequestId,
            result = message
        )
        Timber.i("${LoggerMessages.APPROVE_REQUEST} $topic $jsonRpcResponse")
        val result = Sign.Params.Response(sessionTopic = topic, jsonRpcResponse = jsonRpcResponse)
        SignClient.respond(result) {error ->
            Timber.e(error.toString())
        }
    }

    private fun signData(privateKey: String) = if (currentEthMessage.type == TYPED_MESSAGE) {
        signatureRepository.signTypedData(currentEthMessage.data, privateKey)
    } else {
        signatureRepository.signData(currentEthMessage.data, privateKey)
    }

    override fun rejectRequest(peerId: String) {
        logger.logToFirebase("${LoggerMessages.REJECT_REQUEST} $peerId")
        clientMap[peerId]?.rejectRequest(currentRequestId)
    }

    override fun rejectRequestV2(topic: String) {
        logger.logToFirebase("${LoggerMessages.REJECT_REQUEST} $topic")
        val jsonRpcResponseError = Sign.Model.JsonRpcResponse.JsonRpcError(
            id = currentRequestId,
            code = -32000,
            message = "Rejected by the user"
        )
        val result = Sign.Params.Response(sessionTopic = topic, jsonRpcResponse = jsonRpcResponseError)
        SignClient.respond(result) { error ->
            Timber.e(error.toString())
        }
    }

    override fun killAllAccountSessions(address: String, chainId: Int): Completable =
        getSessions()
            .map { sessions ->
                sessions.filter { session ->
                    session.chainId == chainId && session.address.equals(
                        address,
                        true
                    )
                }
                    .forEach { session ->
                        with(clientMap) {
                            this[session.peerId]?.killSession()
                            remove(session.peerId)
                        }
                    }
            }.flatMapCompletable {
                dappDao.deleteAllDappsForAccount(address)
            }

    override fun killSessionByPeerId(peerId: String): Completable {
        logger.logToFirebase("${LoggerMessages.KILL_SESSION} $peerId")
        return deleteDappSession(peerId)
            .andThen {
                with(clientMap) {
                    if (this[peerId]?.session != null) {
                        this[peerId]?.killSession()
                    }
                    remove(peerId)
                }
            }
    }

    override fun killSessionByTopic(topic: String) {
        val disconnectParams = Sign.Params.Disconnect(topic)

        // todo: return this error to be subscribed to, like in killSessionByPeerId(peerId)
        SignClient.disconnect(disconnectParams) { error ->
            Timber.e(error.toString())
        }
    }

    // only the pairing is killed and not the session
    override fun killPairingByTopic(topic: String) {
        val disconnectParams = Core.Params.Disconnect(topic)

        // todo: return this error to be subscribed to, like in killSessionByPeerId(peerId)
        CoreClient.Pairing.disconnect(disconnectParams) { error ->
            Timber.e(error.toString())
        }
    }

    // only the pairing is killed and not the session
    override fun killPairingBySessionTopic(sessionTopic: String) {
        val session = SignClient.getActiveSessionByTopic(sessionTopic) ?: return
        val disconnectParams = Core.Params.Disconnect(session.pairingTopic)

        // todo: return this error to be subscribed to, like in killSessionByPeerId(peerId)
        CoreClient.Pairing.disconnect(disconnectParams) { error ->
            Timber.e(error.toString())
        }
    }

    override fun dispose() {
        disposable.dispose()
        pingDisposable?.dispose()
        pingDisposable = null
        clientMap.forEach { (_: String, client: WCClient) -> client.disconnect() }
        clientMap.clear()
        reconnectionAttempts.clear()
    }

    companion object {
        fun namespacesCountNonEip155Chains(namespaces: Map<String, Sign.Model.Namespace.Proposal>): Int {
            return namespaces.entries
                .filter { entry -> entry.key != EIP155 }
                .flatMap { entry -> entry.value.chains ?: emptyList() }
                .size
        }

        // todo: move somewhere else?
        // only works for eip155 namespace
        fun namespacesToAddresses(namespaces: Map<String, Sign.Model.Namespace.Session>): List<String> {
            val accounts = namespaces[EIP155]?.accounts ?: emptyList()
            return accounts
                .mapNotNull { account -> account.split(":").getOrNull(2) }
                // todo: check for valid address?
                .distinct()
        }

        // todo: move somewhere else?
        // only works for eip155 namespace and known chains
        fun sessionNamespacesToChainNames(namespaces: Map<String, Sign.Model.Namespace.Session>): List<String> {
            val accounts = namespaces[EIP155]?.accounts ?: emptyList()
            return accounts
                .mapNotNull { account -> account.split(":").getOrNull(1)?.toIntOrNull() }
                .distinct()
                .mapNotNull { chainId -> getNetworkNameOrNull(chainId) }
        }

        // todo: move somewhere else?
        // only works for eip155 namespace
        // todo: show names of non supported evm namespaces, see: fetchUnsupportedNetworkName
        fun proposalNamespacesToChainNames(namespace: WalletConnectProposalNamespace): List<String> {
            return namespace.chains
                .mapNotNull { chain -> chain.split(":").getOrNull(1)?.toIntOrNull() }
                .distinct()
                .map { chainId -> getNetworkNameOrNull(chainId) ?: "unknown evm chain" }
        }

        fun initializeWalletConnect2(application: Application) {
            val projectId = BuildConfig.WALLETCONNECT_PROJECT_ID
            val relayUrl = BuildConfig.WALLETCONNECT_RELAY_URL

            val serverUrl = "wss://$relayUrl?projectId=${projectId}"
            val connectionType = ConnectionType.AUTOMATIC

            // todo: move to localization
            val appMetaData = Core.Model.AppMetaData(
                name = "Minerva Wallet",
                description = "Minerva is like your physical wallet and it simplifies everything around your identities and moneys, while you always stay in control over your assets.",
                url = "https://minerva.digital/",
                icons = listOf("https://minerva.digital/i/minerva-owl.svg"),
                redirect = "kotlin-wallet-wc:/request" // todo:Custom Redirect URI
            )

            CoreClient.initialize(
                metaData = appMetaData,
                relayServerUrl = serverUrl,
                connectionType = connectionType,
                application = application,
                // no relay
                onError = { error ->
                    Timber.e(error.toString())
                }
            )

            val init = Sign.Params.Init(core = CoreClient)
            SignClient.initialize(init) { error ->
                Timber.e(error.toString())
            }
        }

        const val PING_TIMEOUT: Long = 60
        const val RETRY_DELAY: Long = 5
        const val MAX_RECONNECTION_ATTEMPTS: Int = 3
        const val INIT_ATTEMPT: Int = 0
        const val ONE_ATTEMPT: Int = 1
        const val EIP155: String = "eip155"
    }
}