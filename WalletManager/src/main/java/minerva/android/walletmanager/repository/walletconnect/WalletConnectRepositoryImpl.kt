package minerva.android.walletmanager.repository.walletconnect

import android.annotation.SuppressLint
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.ONE
import minerva.android.kotlinUtils.crypto.getFormattedMessage
import minerva.android.kotlinUtils.crypto.hexToUtf8
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.MESSAGE
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.TYPED_MESSAGE
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.mappers.DappSessionToEntityMapper
import minerva.android.walletmanager.model.mappers.EntitiesToDappSessionsMapper
import minerva.android.walletmanager.model.mappers.SessionEntityToDappSessionMapper
import minerva.android.walletmanager.model.mappers.WCEthTransactionToWalletConnectTransactionMapper
import minerva.android.walletmanager.model.mappers.WCPeerToWalletConnectPeerMetaMapper
import minerva.android.walletmanager.model.mappers.WCSessionToWalletConnectSessionMapper
import minerva.android.walletmanager.model.mappers.WalletConnectSessionMapper
import minerva.android.walletmanager.model.walletconnect.DappSession
import minerva.android.walletmanager.model.walletconnect.Topic
import minerva.android.walletmanager.model.walletconnect.WalletConnectSession
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

    @SuppressLint("CheckResult")
    override fun connect(
        session: WalletConnectSession,
        peerId: String,
        remotePeerId: String?,
        dapps: List<DappSession>
    ) {
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
                if (WCClient.CHANGE_TYPE == type) {//"CHANGE_TYPE" case - get data from db for setting details into popap dialog
                    getDappSessionById(peerId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onSuccess = { dappSession ->
                                status.onNext(
                                    OnSessionRequest(
                                        meta = WCPeerToWalletConnectPeerMetaMapper.map(
                                            if (dappSession.iconUrl.isNotEmpty()) {//add icon of api to popap dialog
                                                meta.copy(icons = listOf(dappSession.iconUrl))
                                            } else {
                                                meta
                                            }
                                        ),
                                        chainId,
                                        Topic(peerId, remotePeerId),
                                        handshakeId,
                                        type = type
                                    )
                                )
                            }
                        )
                } else {
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
                                bridge,
                                key
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
        onSuccess: (session: DappSession) -> Unit,
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

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun updateDappSession(peerId: String, address: String, chainId: Int, accountName: String, networkName: String): Completable =
        dappDao.update(peerId, address, chainId, accountName, networkName)

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntitiesToDappSessionsMapper.map(it) }

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll()
            .map { sessionEntities -> EntitiesToDappSessionsMapper.map(sessionEntities) }

    override fun getDappSessionById(peerId: String): Single<DappSession> =
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
        dapp: DappSession
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
        networkName: String,
        handshakeId: Long
    ): Completable =
        if (clientMap[connectionPeerId]?.approveSession(listOf(accountAddress), accountChainId, connectionPeerId, handshakeId) == true) {
            logger.logToFirebase("${LoggerMessages.APPROVE_SESSION} ${connectionPeerId}")
            //update specified dapp session db record by specified parameters
            updateDappSession(connectionPeerId, accountAddress, accountChainId, accountName, networkName)
        } else {
            Completable.error(Throwable("Update of Session not approved"))
        }

    private fun startPing(dapps: List<DappSession>) {
        pingDisposable = Observable.interval(0, PING_TIMEOUT, TimeUnit.SECONDS)
            .doOnNext { ping(dapps) }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e("Error while ping: $it") })
    }

    private fun ping(dapps: List<DappSession>) {
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

    override fun killSession(peerId: String): Completable {
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

    override fun dispose() {
        disposable.dispose()
        pingDisposable?.dispose()
        pingDisposable = null
        clientMap.forEach { (_: String, client: WCClient) -> client.disconnect() }
        clientMap.clear()
        reconnectionAttempts.clear()
    }

    companion object {
        const val PING_TIMEOUT: Long = 60
        const val RETRY_DELAY: Long = 5
        const val MAX_RECONNECTION_ATTEMPTS: Int = 3
        const val INIT_ATTEMPT: Int = 0
        const val ONE_ATTEMPT: Int = 1
    }
}