package minerva.android.walletConnect.repository

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import minerva.android.walletConnect.client.*
import minerva.android.walletConnect.model.session.WCPeerMeta
import minerva.android.walletConnect.model.session.WCSession
import okhttp3.OkHttpClient

class WalletConnectRepositoryImpl(private val okHttpClient: OkHttpClient) :
    WalletConnectRepository {

    private lateinit var client: WCClient
    private val status: PublishSubject<WalletConnectStatus> = PublishSubject.create()
    override val connectionStatusFlowable: Flowable<WalletConnectStatus>
        get() = status.toFlowable(BackpressureStrategy.BUFFER)

    override fun connect(qrCode: String) {

        with(WCClient(httpClient = okHttpClient)) {
            client = this
            onWCOpen = { peerId ->
                //todo handle for multiple session management
            }

            onSessionRequest = { _, meta, chainId ->
                status.onNext(OnSessionRequest(meta, chainId))
            }

            onFailure = {
                status.onNext(OnConnectionFailure(it))
            }

            onDisconnect = { code, reason ->
                status.onNext(OnDisconnect(code))
            }

            WCSession.from(qrCode)?.let {
                connect(
                    it,
                    peerMeta = WCPeerMeta( //todo extract values
                        name = "Minerva Wallet",
                        url = "https://docs.minerva.digital/"
                    )
                )
            }
        }
    }

    override fun approveSession(addresses: List<String>, chainId: Int) {
        client.approveSession(addresses, chainId)
    }

    override fun rejectSession() {
        client.rejectSession()
    }

    override fun killSession() {
        client.killSession()
    }

    override fun disconnect() {
        client.disconnect()
    }

}