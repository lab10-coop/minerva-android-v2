package minerva.android.walletmanager.repository.walletconnect

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.WCClient
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import minerva.android.walletmanager.database.MinervaDatabase
import minerva.android.walletmanager.model.DappSession
import minerva.android.walletmanager.model.Topic
import minerva.android.walletmanager.model.WalletConnectSession
import minerva.android.walletmanager.model.mappers.*
import java.util.concurrent.ConcurrentHashMap

/*TODO
  this should be moved to service binded to the MainActivity class. Here within the app lifecycle, on given events, databe base
  with dapp sessions should be updated. Thanks to that every view listeting for changes im DB will update all necesarry data.

  HERE ALL OPERATION ON DB. besides removal when user updates by himself

  tutaj powinno byc wstrzykniete DappSessionRepository, ktore ogrania handlowanie bazy danych
*/
class WalletConnectRepositoryImpl(
    minervaDatabase: MinervaDatabase,
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()
) : WalletConnectRepository {

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    private val dappDao = minervaDatabase.dappDao()

    override fun saveDappSession(dappSession: DappSession): Completable =
        dappDao.insert(DappSessionToEntityMapper.map(dappSession))

    override fun deleteDappSession(peerId: String): Completable =
        dappDao.delete(peerId)

    override fun getSessions(): Single<List<DappSession>> =
        dappDao.getAll().firstOrError().map { EntityToDappSessionMapper.map(it) }

    override fun deleteAllDappsForAccount(address: String): Completable =
        dappDao.deleteAllDappsForAccount(address)

    override fun getSessionsFlowable(): Flowable<List<DappSession>> =
        dappDao.getAll().map { EntityToDappSessionMapper.map(it) }

    private var wcClient: WCClient = WCClient()

    override fun connect(session: WalletConnectSession, peerId: String, remotePeerId: String?) {
        wcClient = WCClient()
        wcClient.killSession()
        with(wcClient) {
            onWCOpen = { peerId ->
                clientMap[peerId] = this
            }
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

    override val isClientMapEmpty: Boolean
        get() = clientMap.isEmpty()

    override val walletConnectClients: ConcurrentHashMap<String, WCClient>
        get() = clientMap

    override fun getWCSessionFromQr(qrCode: String): WalletConnectSession =
        WCSessionToWalletConnectSessionMapper.map(WCSession.from(qrCode))

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String) {
        //todo add to DB
        clientMap[peerId]?.approveSession(addresses, chainId, peerId)
    }

    override fun rejectSession(peerId: String) {
        clientMap[peerId]?.rejectSession()
        //todo should delete WCClient from map (?)
    }

    override fun killSession(peerId: String) {
        //todo add killing all sessions for given account
        //todo remove from DB
        with(clientMap) {
            this[peerId]?.killSession()
            remove(peerId)
        }
    }
}