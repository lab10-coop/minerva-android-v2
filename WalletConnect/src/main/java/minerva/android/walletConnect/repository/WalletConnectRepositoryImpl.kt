package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.Topic
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import java.util.concurrent.ConcurrentHashMap

class WalletConnectRepositoryImpl : WalletConnectRepository {

    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    private val clientMap: ConcurrentHashMap<String, WCClient> = ConcurrentHashMap()

    override fun connect(session: WCSession, peerId: String, remotePeerId: String?) {
        with(WCClient()) {
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

            connect(
                session,
                peerMeta = WCPeerMeta( //todo extract values
                    name = "Minerva Wallet",
                    url = "https://docs.minerva.digital/"
                ),
                peerId = peerId,
                remotePeerId = remotePeerId
            )

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
        clientMap[peerId]?.killSession()
    }
}