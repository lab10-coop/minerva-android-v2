package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
import minerva.android.kotlinUtils.Empty
import minerva.android.kotlinUtils.InvalidValue
import minerva.android.kotlinUtils.crypto.getFormattedMessage
import minerva.android.kotlinUtils.crypto.hexToUtf8
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage
import minerva.android.walletConnect.model.ethereum.WCEthereumSignMessage.WCSignType.*
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.mappers.*
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
    private var currentRequestId: Long = Long.InvalidValue
    internal lateinit var currentEthMessage: WCEthereumSignMessage
    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus> get() = status.toFlowable(BackpressureStrategy.DROP)
    private var disposable: CompositeDisposable = CompositeDisposable()
    private var pingDisposable: Disposable? = null
    private val dappDao = minervaDatabase.dappDao()

    override fun connect(session: WalletConnectSession, peerId: String, remotePeerId: String?, dapps: List<DappSession>) {
        wcClient = WCClient()
        with(wcClient) {
            onWCOpen = { peerId ->
                clientMap[peerId] = this
                if (pingDisposable == null) {
                    startPing(dapps)
                }
            }

            onSessionRequest = { remotePeerId, meta, chainId, peerId, handshakeId ->
                status.onNext(
                    OnSessionRequest(
                        WCPeerToWalletConnectPeerMetaMapper.map(meta),
                        chainId,
                        Topic(peerId, remotePeerId),
                        handshakeId
                    )
                )
            }

            onFailure = { error, peerId ->
                Timber.e("WalletConnect onFailure: $error, peerId: $peerId")
                removeDappSession(peerId, onSuccess = { session ->
                    removeWcClient(session.peerId)
                    status.onNext(OnFailure(error, session.name))
                }, onError = {
                    removeWcClient(peerId)
                    status.onNext(OnFailure(error, String.Empty))
                })
            }
            onDisconnect = { _, peerId ->
                peerId?.let {
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

            onEthSign = { id, message, peerId ->
                currentRequestId = id
                currentEthMessage = message
                status.onNext(OnEthSign(getUserReadableData(message), peerId))
            }

            onEthSendTransaction = { id, transaction, peerId ->
                logger.logToFirebase("WalletConnect transaction: peerId: $peerId; transaction: $transaction")
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

    private fun removeDappSession(peerId: String, onSuccess: (session: DappSession) -> Unit, onError: () -> Unit) {
        getDappSessionById(peerId)
            .flatMap { session ->
                deleteDappSession(session.peerId)
                    .toSingleDefault(session)
            }
            .map { session -> onSuccess(session) }
            .doOnError { onError() }
            .subscribeOn(Schedulers.io())
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposable)
    }

    override fun rejectSession(peerId: String) {
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

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntitiesToDappSessionsMapper.map(it) }

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll().map { sessionEntities -> EntitiesToDappSessionsMapper.map(sessionEntities) }

    override fun getDappSessionById(peerId: String): Single<DappSession> =
        dappDao.getDapSessionById(peerId).map { SessionEntityToDappSessionMapper.map(it) }

    private fun getUserReadableData(message: WCEthereumSignMessage) =
        when (message.type) {
            PERSONAL_MESSAGE -> message.data.hexToUtf8
            MESSAGE, TYPED_MESSAGE -> message.data.getFormattedMessage
        }

    override fun getWCSessionFromQr(qrCode: String): WalletConnectSession =
        WCSessionToWalletConnectSessionMapper.map(WCSession.from(qrCode))

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String, dapp: DappSession): Completable =
        if (clientMap[peerId]?.approveSession(addresses, chainId, peerId) == true) {
            saveDappSession(dapp)
        } else {
            Completable.error(Throwable("Session not approved"))
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
                    entry.value.approveSession(entry.value.accounts!!, entry.value.chainId!!, entry.key)
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
        clientMap[peerId]?.approveRequest(currentRequestId, signData(privateKey))
    }

    override fun approveTransactionRequest(peerId: String, message: String) {
        clientMap[peerId]?.approveRequest(currentRequestId, message)
    }

    private fun signData(privateKey: String) = if (currentEthMessage.type == TYPED_MESSAGE) {
        signatureRepository.signTypedData(currentEthMessage.data, privateKey)
    } else {
        signatureRepository.signData(currentEthMessage.data, privateKey)
    }

    override fun rejectRequest(peerId: String) {
        clientMap[peerId]?.rejectRequest(currentRequestId)
    }

    override fun killAllAccountSessions(address: String): Completable =
        getSessions()
            .map { sessions ->
                sessions.filter { it.address.equals(address, true) }.forEach { session ->
                    with(clientMap) {
                        this[session.peerId]?.killSession()
                        remove(session.peerId)
                    }
                }
            }.flatMapCompletable {
                dappDao.deleteAllDappsForAccount(address)
            }

    override fun dispose() {
        disposable.dispose()
        pingDisposable?.dispose()
        pingDisposable = null
    }

    override fun killSession(peerId: String): Completable =
        deleteDappSession(peerId)
            .andThen {
                with(clientMap) {
                    if (this[peerId]?.session != null) {
                        this[peerId]?.killSession()
                    }
                    remove(peerId)
                }
            }

    companion object {
        const val PING_TIMEOUT: Long = 60
    }
}