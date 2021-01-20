package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

//todo tests will be added when multiple session management is implemented
class WalletConnectRepositoryImpl(private val okHttpClient: OkHttpClient) :
    WalletConnectRepository {

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)
    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()

    override fun connect(qrCode: String) {

        with(WCClient(httpClient = okHttpClient)) {

            onWCOpen = { peerId ->
                Timber.tag("kobe").d("ON OPEN peerId: $peerId")
                clientMap[peerId] = this
            }

            onSessionRequest = { remotePeerId, meta, chainId, peerId ->
                Timber.tag("kobe")
                    .d("ON SESSION REQUEST remotePeerId: $remotePeerId; peerId: $peerId")
                status.onNext(OnSessionRequest(meta, chainId, peerId))
            }

            onFailure = {
                status.onNext(OnConnectionFailure(it))
            }

            onEthSign = { id, _ ->
                Timber.tag("kobe").d("ON ETH SIGN id: $id")
            }

            onDisconnect = { code, reason, peerId ->
                status.onNext(OnDisconnect(code, peerId))
            }

            val session = WCSession.from(qrCode)
            Timber.tag("kobe").d("BRIDGE: ${session.bridge}")

            connect(
                session,
                peerMeta = WCPeerMeta( //todo extract values
                    name = "Minerva Wallet",
                    url = "https://docs.minerva.digital/"
                )
            )

        }
    }

    override fun approveSession(addresses: List<String>, chainId: Int, peerId: String) {
        clientMap[peerId]?.approveSession(addresses, chainId, peerId)
        Timber.tag("kobe").d("approved PEER ID: $peerId")
    }

    override fun rejectSession(peerId: String) {
        clientMap[peerId]?.rejectSession()
        Timber.tag("kobe").d("reject PEER ID: $peerId")
    }

    override fun killSession(peerId: String) {
        clientMap[peerId]?.killSession()
        Timber.tag("kobe").d("kill session PEER ID: $peerId")
    }
}