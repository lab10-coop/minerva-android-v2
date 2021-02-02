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
import minerva.android.kotlinUtils.event.Event
import minerva.android.kotlinUtils.function.orElse
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.Topic
import minerva.android.walletmanager.model.WalletConnectSession
import minerva.android.walletmanager.model.mappers.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class WalletConnectRepositoryImpl(
    minervaDatabase: MinervaDatabase,
    private var wcClient: WCClient = WCClient(),
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()
) : WalletConnectRepository {

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    override val isClientMapEmpty: Boolean
        get() = clientMap.isEmpty()

    override val walletConnectClients: ConcurrentHashMap<String, WCClient>
        get() = clientMap

    private var disposable: Disposable? = null

    private val dappDao = minervaDatabase.dappDao()

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntityToDappSessionMapper.map(it) }

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntityToDappSessionMapper.map(it) }

    override fun connect(session: WalletConnectSession, peerId: String, remotePeerId: String?) {
        wcClient = WCClient()
        with(wcClient) {
            onWCOpen = { peerId -> clientMap[peerId] = this }

            onSessionRequest = { remotePeerId, meta, chainId, peerId ->
                status.onNext(
                    OnSessionRequest(
                        WCPeerToWalletConnectPeerMetaMapper.map(meta),
                        chainId,
                        Topic(peerId, remotePeerId)
                    )
                )
            }
            onFailure = { error, peerId ->
                status.onNext(OnConnectionFailure(error, peerId))
            }
            onDisconnect = { code, peerId ->
                peerId?.let {
                    if (walletConnectClients.containsKey(peerId)) {
                        deleteSession(peerId)
                    }
                }
                status.onNext(OnDisconnect(code, peerId))
            }

            connect(
                WalletConnectSessionMapper.map(session),
                peerMeta = WCPeerMeta(),
                peerId = peerId,
                remotePeerId = remotePeerId
            )
        }
    }

    private fun deleteSession(peerId: String) {
        disposable = deleteDappSession(peerId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { Timber.e(it) })
    }

    override fun getWCSessionFromQr(qrCode: String): WalletConnectSession =
        WCSessionToWalletConnectSessionMapper.map(WCSession.from(qrCode))

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String, dapp: DappSession): Completable {
        val isApproved: Boolean = clientMap[peerId]?.approveSession(addresses, chainId, peerId) == true
        return if (isApproved) {
            saveDappSession(dapp)
        } else {
            Completable.error(Throwable("Session not approved"))
        }
    }

    override fun rejectSession(peerId: String) {
        with(clientMap) {
            this[peerId]?.rejectSession()
            remove(peerId)
        }
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