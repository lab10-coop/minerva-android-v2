package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import minerva.android.blockchainprovider.repository.signature.SignatureRepository
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
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class WalletConnectRepositoryImpl(
    private val signatureRepository: SignatureRepository,
    minervaDatabase: MinervaDatabase,
    private var wcClient: WCClient = WCClient(),
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()
) : WalletConnectRepository {
    private var currentRequestId: Long = Long.InvalidValue
    internal lateinit var currentEthMessage: WCEthereumSignMessage
    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus> get() = status.toFlowable(BackpressureStrategy.BUFFER)
    override val isClientMapEmpty: Boolean get() = clientMap.isEmpty()
    override val walletConnectClients: ConcurrentHashMap<String, WCClient> get() = clientMap
    private var disposable: Disposable? = null
    private val dappDao = minervaDatabase.dappDao()

    override fun connect(session: WalletConnectSession, peerId: String, remotePeerId: String?) {
        wcClient = WCClient()
        with(wcClient) {
            onWCOpen = { peerId -> clientMap[peerId] = this }
            onSessionRequest = { remotePeerId, meta, chainId, peerId ->
                status.onNext(
                    OnSessionRequest(WCPeerToWalletConnectPeerMetaMapper.map(meta), chainId, Topic(peerId, remotePeerId))
                )
            }
            onFailure = { error, _ ->
                Timber.e(error)
                status.onError(error)
            }
            onDisconnect = { _, peerId ->
                peerId?.let {
                    if (walletConnectClients.containsKey(peerId)) {
                        deleteSession(peerId)
                    }
                }
                status.onNext(OnDisconnect)
            }

            onEthSign = { id, message, peerId ->
                currentRequestId = id
                currentEthMessage = message
                status.onNext(OnEthSign(getUserReadableData(message), peerId))
            }

            onEthSendTransaction = { id, transaction, peerId ->
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

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntitiesToDappSessionsMapper.map(it) }

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntitiesToDappSessionsMapper.map(it) }

    override fun getDappSessionById(peerId: String): Single<DappSession> =
        dappDao.getDapSessionById(peerId).map { SessionEntityToDappSessionMapper.map(it) }

    private fun getUserReadableData(message: WCEthereumSignMessage) =
        when (message.type) {
            PERSONAL_MESSAGE -> message.data.hexToUtf8
            MESSAGE, TYPED_MESSAGE -> message.data.getFormattedMessage
        }

    private fun deleteSession(peerId: String) {
        disposable = deleteDappSession(peerId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { Timber.e(it) })
    }

    override fun getWCSessionFromQr(qrCode: String): WalletConnectSession =
        WCSessionToWalletConnectSessionMapper.map(WCSession.from(qrCode))

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String, dapp: DappSession): Completable =
        if (clientMap[peerId]?.approveSession(addresses, chainId, peerId) == true) {
            saveDappSession(dapp)
        } else {
            Completable.error(Throwable("Session not approved"))
        }

    override fun rejectSession(peerId: String) {
        with(clientMap) {
            this[peerId]?.rejectSession()
            remove(peerId)
        }
    }

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
                sessions.filter { it.address == address }.forEach { session ->
                    with(clientMap) {
                        this[session.peerId]?.killSession()
                        remove(session.peerId)
                    }
                }
            }.flatMapCompletable {
                dappDao.deleteAllDappsForAccount(address)
            }

    override fun dispose() {
        disposable?.dispose()
    }

    override fun killSession(peerId: String): Completable =
        deleteDappSession(peerId)
            .andThen {
                with(clientMap) {
                    this[peerId]?.killSession()
                    remove(peerId)
                }
            }
}