package minerva.android.walletmanager.repository.walletconnect

import android.app.Application
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
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()
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

    private fun initialize(): Boolean {
        if (isInitialized) {
            return true
        }

        // do this only once as to not duplicate event watching..
        // todo: handle events etc.
        val walletDelegate = object : SignClient.WalletDelegate {
            override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal) {
                // Triggered when wallet receives the session proposal sent by a Dapp
                Timber.i("onSessionProposal: $sessionProposal")

                // check if the wallet is able to do what is proposed
                val requiredNamespacesEip155 = sessionProposal.requiredNamespaces["eip155"]
                if (requiredNamespacesEip155 == null) {
                    Timber.e("Not requesting eip155 namespace.")
                    // todo: show info to the user then reject proposal
                    return
                }
                // check if non eip155 is requested and then reject
                if (sessionProposal.requiredNamespaces.keys.size > 1) {
                    Timber.e("Contains namespaces that are not eip155.")
                    // todo: show info to the user then reject proposal
                    return
                }
                // todo: check if methods are required that we don't support
                // todo: check if events are required that we don't support
                // todo: check which chains are requested and check if we have active accounts or show the current UI


                // todo: show popup here, only then proceed
                status.onNext(OnSessionRequestV2(sessionProposal))

                /*
                // Namespace identifier, see for reference: https://github.com/ChainAgnostic/CAIPs/blob/master/CAIPs/caip-2.md#syntax
                val namespace = "eip155"
                // List of accounts on chains
                val accounts: List<String> = listOf<String>(
                    "eip155:100:0xc269D9794473Fee9912B13be65764826341bFd3e"
                ) // todo: get from accounts
                // List of methods that wallet approves
                val methods: List<String> = requiredNamespacesEip155.methods // todo: add addition capabilities?
                // List of events that wallet approves
                val events: List<String> = requiredNamespacesEip155.events // todo: add addition capabilities?
                val namespaces: Map<String, Sign.Model.Namespace.Session> =
                    mapOf(Pair(namespace, Sign.Model.Namespace.Session(accounts, methods, events, extensions = null)))

                val approveParams: Sign.Params.Approve = Sign.Params.Approve(sessionProposal.proposerPublicKey, namespaces)
                SignClient.approveSession(approveParams) { error ->
                    Timber.e(error.toString())
                }
                */
            }

            override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
                // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
                Timber.i("onSessionRequest")

                // todo: use some constants here
                when (sessionRequest.request.method) {
                    "personal_sign" -> {
                        // todo: this seems to be something metamask specific
                    }
                    "eth_sign" -> {
                        // todo
                        //currentRequestId = id
                        //currentEthMessage = message
                        //status.onNext(OnEthSign(getUserReadableData(message), peerId))
                    }
                    "eth_signTypedData" -> {
                        // todo: this would be awesome to support.
                    }
                    "eth_sendTransaction" -> {
                        // todo
                        //currentRequestId = id
                        //status.onNext(
                        //    OnEthSendTransaction(
                        //        WCEthTransactionToWalletConnectTransactionMapper.map(transaction),
                        //        peerId
                        //    )
                        //)
                    }
                    "eth_signTransaction" -> {
                        // todo: but not that common or important.
                    }
                    "eth_sendRawTransaction" -> {
                        // todo: but not that common or important.
                    }
                }

                // todo: response
                //val sessionTopic: String = /*Topic of Session*/
                //val jsonRpcResponse: Sign.Model.JsonRpcResponse.JsonRpcResult = /*Settled Session Request ID along with request data*/
                //val result = Sign.Params.Response(sessionTopic = sessionTopic, jsonRpcResponse = jsonRpcResponse)

                //SignClient.respond(result) { error -> /*callback for error while responding session request*/ }
            }

            override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
                // Triggered when the session is deleted by the peer
                Timber.i("onSessionDelete")
                when (deletedSession) {
                    is Sign.Model.DeletedSession.Success -> {
                        Timber.i("onSessionDelete Success")
                        val deletedSessionSuccess = deletedSession as Sign.Model.DeletedSession.Success
                        val session = SignClient.getSettledSessionByTopic(deletedSessionSuccess.topic)
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
        SignClient.setWalletDelegate(walletDelegate)

        return true
    }

    override fun connect(
        session: WalletConnectSession,
        peerId: String,
        remotePeerId: String?,
        dapps: List<DappSessionV1>
    ) {
        if (session.version == "2") {
            Timber.i("connect to ${session.toUri()}")
            // todo: the bottom sheet should come up before pairing.

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

            onSessionRequest = { remotePeerId, meta, chainId, peerId, handshakeId ->
                logger.logToFirebase("${LoggerMessages.ON_SESSION_REQUEST}$peerId")
                status.onNext(
                    OnSessionRequest(
                        WCPeerToWalletConnectPeerMetaMapper.map(meta),
                        chainId,
                        Topic(peerId, remotePeerId),
                        handshakeId
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
                    OnEthSendTransaction(
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

    override fun getV2Sessions(): List<DappSessionV2> =
        SignClient.getListOfSettledSessions()
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
                Int.InvalidValue,
                false, // todo: how do we know this here.
                session.metaData,
                session.namespaces
            ) }

    override fun getV1Sessions(): Single<List<DappSessionV1>> =
        dappDao.getAll().firstOrError()
            .map { EntitiesToDappSessionsMapper.map(it) }

    override fun getV1SessionsFlowable(): Flowable<List<DappSessionV1>> =
        dappDao.getAll()
            .map { EntitiesToDappSessionsMapper.map(it) }

    override fun getSessionsAndPairings(): Single<List<MinervaPrimitive>> =
        dappDao.getAll().firstOrError()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
                    .mergeWithoutDuplicates(getV2Pairings()) // todo: only add pairings when there is no session already
            }

    override fun getSessionsAndPairingsFlowable(): Flowable<List<MinervaPrimitive>> =
        dappDao.getAll()
            .map {
                EntitiesToDappSessionsMapper
                    .map(it)
                    .mergeWithoutDuplicates(getV2Sessions())
                    .mergeWithoutDuplicates(getV2Pairings()) // todo: only add pairings when there is no session already
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

    // todo: add WC 2.0 sessions here with ByTopic instead?
    override fun getDappSessionById(peerId: String): Single<DappSessionV1> =
        dappDao.getDappSessionById(peerId).map { SessionEntityToDappSessionMapper.map(it) }

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

    override fun approveTransactionRequest(peerId: String, message: String) {
        logger.logToFirebase("${LoggerMessages.APPROVE_TX_REQUEST} $peerId")
        clientMap[peerId]?.approveRequest(currentRequestId, message)
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

    // todo: check if the pairing is killed if also the session is killed.
    override fun killPairingByTopic(topic: String) {
        val disconnectParams = Core.Params.Disconnect(topic)

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
        // todo: move somewhere else?
        // only works for eip155 namespace
        fun namespacesToAddresses(namespaces: Map<String, Sign.Model.Namespace.Session>): List<String> {
            val accounts = namespaces["eip155"]?.accounts ?: emptyList()
            return accounts
                .mapNotNull { account -> account.split(":").getOrNull(2) }
                // todo: check for valid address?
                .distinct()
        }

        // todo: move somewhere else?
        // only works for eip155 namespace
        fun sessionNamespacesToChainNames(namespaces: Map<String, Sign.Model.Namespace.Session>): List<String> {
            val accounts = namespaces["eip155"]?.accounts ?: emptyList()
            return accounts
                .mapNotNull { account -> account.split(":").getOrNull(1)?.toIntOrNull() }
                .distinct()
                .mapNotNull { chainId -> getNetworkNameOrNull(chainId) }
        }

        // todo: move somewhere else?
        // only works for eip155 namespace
        // todo: support non eip155 namespaces (just say non evm chain(s))
        // todo: show names of non supported evm namepsaces
        fun proposalNamespacesToChainNames(namespaces: Map<String, Sign.Model.Namespace.Proposal>): List<String> {
            val chains = namespaces["eip155"]?.chains ?: emptyList()
            return chains
                .mapNotNull { chain -> chain.split(":").getOrNull(1)?.toIntOrNull() }
                .distinct()
                .mapNotNull { chainId -> getNetworkNameOrNull(chainId) }
        }

        fun initializeWalletConnect2(application: Application) {
            val projectId = BuildConfig.WALLETCONNET_PROJECT_ID
            val relayUrl = BuildConfig.WALLETCONNET_RELAY_URL

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
                relayServerUrl = serverUrl,
                connectionType = connectionType,
                application = application,
                metaData = appMetaData
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
    }
}