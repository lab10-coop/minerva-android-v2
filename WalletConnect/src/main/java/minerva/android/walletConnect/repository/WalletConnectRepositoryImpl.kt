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
    chyba trzeba bedzie to przenieść do WalletManagera w BindedSerwice (?) i updatowac na kinkretnych eventach base danych. Tam gdzie jest potrzebny update
    list dApps flowable bedzie nasluchiwana bedzie updatowana z DB. AccountViewModel (session count), services list.
    Ważne jest to zeby bylo jedno zrodło eventów, gdzie jak mam disconnect to usuwam DappSession, a tam gdzie musze zrobic update to nasluchuje na flowable dAppList.
    nasluchiwanie na websocket na konkretne channele musi byc z cyklem zycia calej apki, zeby usuwac DappSession jak poleci event. pewnie trzeba bedzie dodac pinga zeby timeout nie lecial.
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
        clientMap[peerId]?.approveSession(addresses, chainId, peerId)
    }

    override fun rejectSession(peerId: String) {
        clientMap[peerId]?.rejectSession()
    }

    override fun killSession(peerId: String) {
        with(clientMap) {
            this[peerId]?.killSession()
            remove(peerId)
        }
    }
}