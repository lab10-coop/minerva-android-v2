package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import java.util.concurrent.ConcurrentHashMap

/*TODO
  this should be moved to service binded to the MainActivity class. Here within the app lifecycle, on given events, databe base
  with dapp sessions should be updated. Thanks to that every view listeting for changes im DB will update all necesarry data.

  HERE ALL OPERATION ON DB. besides removal when user updates by himself

  tutaj powinno byc wstrzykniete DappSessionRepository, ktore ogrania handlowanie bazy danych
*/
class WalletConnectRepositoryImpl(
    private var wcClient: WCClient = WCClient(),
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()
) : WalletConnectRepository {

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    override fun connect(session: WCSession, peerId: String, remotePeerId: String?) {
        wcClient = WCClient()
        with(wcClient) {
            onWCOpen = { peerId ->
                clientMap[peerId] = this
            }
            onSessionRequest = { remotePeerId, meta, chainId, peerId ->
                status.onNext(OnSessionRequest(meta, chainId, Topic(peerId, remotePeerId)))
            }
            onFailure = { error, peerId ->
                status.onNext(OnConnectionFailure(error, peerId))
            }
            onDisconnect = { code, peerId ->
                status.onNext(OnDisconnect(code, peerId))
            }

            connect(session, peerMeta = WCPeerMeta(), peerId = peerId, remotePeerId = remotePeerId)
        }
    }

    override val isClientMapEmpty: Boolean
        get() = clientMap.isEmpty()

    override val walletConnectClients: ConcurrentHashMap<String, WCClient>
        get() = clientMap

    override fun getWCSessionFromQr(qrCode: String): WCSession = WCSession.from(qrCode)

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